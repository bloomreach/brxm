/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hippoecm.repository.deriveddata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.ext.DerivedDataFunction;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DerivedDataEngine {

    static final Logger log = LoggerFactory.getLogger(DerivedDataEngine.class);

    private static final String DERIVATIVES_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/hippo:derivatives";

    private final HippoSession session;

    public DerivedDataEngine(HippoSession session) {
        this.session = session;
    }

    public void save() throws RepositoryException {
        save(null);
    }

    public void save(Node node) throws RepositoryException {
        long start = 0;
        final Node derivatesFolder = JcrUtils.getNodeIfExists(DERIVATIVES_PATH, session);
        if (derivatesFolder == null) {
            return;
        }
        if (log.isDebugEnabled()) {
            start = System.currentTimeMillis();
            log.debug("Derived engine active");
        }
        try {
            final Collection<Node> nodesToCompute = findNodesToCompute(node);
            if (log.isDebugEnabled()) {
                log.debug("Derived engine found " + nodesToCompute.size() + " nodes to be evaluated in " +
                                     (System.currentTimeMillis() - start) + " ms");
            }
            for (Node modified : nodesToCompute) {
                compute(derivatesFolder, modified);
            }
        } catch (NamespaceException | ConstraintViolationException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Derived engine done in " + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }

    private Collection<Node> findNodesToCompute(final Node node) throws RepositoryException {
        final Collection<Node> result = new TreeSet<Node>(new ComputeSetComparator());
        result.addAll(findModifiedDerivatives(node));
        result.addAll(findDerivativesReferencingModifiedNodes(node));
        return result;
    }

    private Collection<Node> findModifiedDerivatives(final Node node) throws RepositoryException {
        final Collection<Node> result = new TreeSet<>(new Comparator<Node>() {
            @Override
            public int compare(final Node o1, final Node o2) {
                try {
                    return o1.getPath().compareTo(o2.getPath());
                } catch (RepositoryException e) {
                    return 0;
                }
            }
        });
        try {
            for (String baseType : new String[] { HippoNodeType.NT_DERIVED, HippoNodeType.NT_DOCUMENT }) {
                for (Node modified : new NodeIterable(session.pendingChanges(node, baseType))) {
                    if (log.isDebugEnabled()) {
                        log.debug("Derived engine found modified node " + modified.getPath() + " ("
                                + modified.getIdentifier() + ") with derived mixin");
                    }
                    result.add(modified);
                }
            }
        } catch (NamespaceException | NoSuchNodeTypeException ex) {
            throw new RepositoryException(HippoNodeType.NT_DERIVED + " not found");
        }
        return result;
    }

    private Collection<Node> findDerivativesReferencingModifiedNodes(final Node node) throws RepositoryException {
        final Collection<Node> result = new ArrayList<Node>();
        try {
            for (Node modified : new NodeIterable(session.pendingChanges(node, "mix:referenceable"))) {
                if (log.isDebugEnabled()) {
                    log.debug("Derived engine found modified referenceable node " + modified.getPath() +
                            " with " + modified.getReferences().getSize() + " references");
                }
                for (Property property : new PropertyIterable(modified.getReferences())) {
                    try {
                        final Node dependentNode = property.getParent();
                        if (property.getName().equals(HippoNodeType.HIPPO_RELATED) && dependentNode.isNodeType(HippoNodeType.NT_DERIVED)) {
                            result.add(dependentNode);
                        }
                    } catch (AccessDeniedException ex) {
                        throw new RepositoryException("configuration problem", ex);
                    } catch (ItemNotFoundException ex) {
                        throw new RepositoryException("inconsistent state", ex);
                    }
                }
            }
        } catch (NamespaceException ex) {
            throw new RepositoryException("jcr:uuid not accessible");
        } catch (NoSuchNodeTypeException ex) {
            throw new RepositoryException("jcr:uuid not found");
        }
        return result;
    }

    public void validate() throws RepositoryException {
        int totalCount = 0, changedCount = 0;
        Node derivatesFolder = session.getNode(DERIVATIVES_PATH);
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM hippo:derived", Query.SQL);
        QueryResult result = query.execute();
        for (Node node : new NodeIterable(result.getNodes())) {
            ++totalCount;
            if (compute(derivatesFolder, node)) {
                ++changedCount;
                if ((changedCount % LocalHippoRepository.batchThreshold) == 0) {
                    session.save();
                }
            }
        }
        log.warn("Validated " + totalCount + " nodes, and reset " + changedCount + " nodes");
        session.save();
    }

    public boolean compute(Node node) throws RepositoryException {
        final Node derivativesFolder = session.getNode(DERIVATIVES_PATH);
        return compute(derivativesFolder, node);
    }

    private boolean compute(Node derivatesFolder, Node modified) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(modified);
        final Collection<String> dependencies = new TreeSet<String>();

        boolean changed = applyFunctions(derivatesFolder, modified, dependencies);
        changed |= updateRelatedProperty(modified, dependencies);

        return changed;
    }

    private boolean applyFunctions(final Node derivatesFolder, final Node modified, final Collection<String> dependencies) throws RepositoryException {
        boolean changed = false;
        for (Node functionNode : new NodeIterable(derivatesFolder.getNodes())) {
            if (functionNode == null) {
                log.error("unable to access all derived data functions");
                continue;
            }

            final FunctionDescription functionDescription = new FunctionDescription(functionNode);
            if (!modified.isNodeType(functionDescription.getApplicableNodeType())) {
                continue;
            }

            changed |= applyFunction(modified, functionDescription, dependencies);
        }
        return changed;
    }

    private boolean applyFunction(final Node modified, final FunctionDescription function, final Collection<String> dependencies) throws RepositoryException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Applying " + function.getName() + " to " + modified.getPath());
            }

            final DerivedDataFunction func = createFunction(function);
            final PropertyMapper mapper = new PropertyMapper(function, modified);

            Map<String, Value[]> accessedPropertyValues = mapper.getAccessedPropertyValues(dependencies);
            Map<String, Value[]> derivedPropertyValues = func.compute(accessedPropertyValues);
            final boolean changed = mapper.persistDerivedPropertyValues(derivedPropertyValues);

            return changed;

        } catch (AccessDeniedException ex) {
            throw new RepositoryException(ex);
        } catch (ItemNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (PathNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (ValueFormatException ex) {
            throw new RepositoryException(ex);
        }
    }

    private DerivedDataFunction createFunction(final FunctionDescription function) throws RepositoryException {
        try {
            Class clazz = Class.forName(function.getClassName());
            DerivedDataFunction func = (DerivedDataFunction) clazz.newInstance();
            func.setValueFactory(session.getValueFactory());
            return func;
        } catch (ClassNotFoundException e) {
            throw new RepositoryException("No such function", e);
        } catch (InstantiationException e) {
            throw new RepositoryException("Can't create function", e);
        } catch (IllegalAccessException e) {
            throw new RepositoryException("Can't access function", e);
        }
    }

    private boolean updateRelatedProperty(final Node modified, final Collection<String> dependencies) throws RepositoryException {
        if (modified.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
            dependencies.remove(modified.getIdentifier());
        }
        Value[] dependenciesValues = new Value[dependencies.size()];
        int i = 0;
        for (String dependency : dependencies) {
            dependenciesValues[i++] = session.getValueFactory().createValue(dependency, PropertyType.REFERENCE);
        }
        Value[] oldDependenciesValues = null;
        if (modified.hasProperty(HippoNodeType.HIPPO_RELATED)) {
            oldDependenciesValues = modified.getProperty(HippoNodeType.HIPPO_RELATED).getValues();
        }

        final boolean changed = !Arrays.equals(oldDependenciesValues, dependenciesValues);

        if (changed && modified.isNodeType(HippoNodeType.NT_DERIVED)) {
            JcrUtils.ensureIsCheckedOut(modified);
            modified.setProperty(HippoNodeType.HIPPO_RELATED, dependenciesValues, PropertyType.REFERENCE);
        }
        return changed;
    }

    public static void removal(Node removed) throws RepositoryException {
        if (removed.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
            final String removedId= removed.getIdentifier();
            for (Property refProp : new PropertyIterable(removed.getReferences())) {
                if (refProp.getName().equals(HippoNodeType.HIPPO_RELATED)) {
                    final Value[] values = refProp.getValues();
                    for (int i = 0; i < values.length; i++) {
                        if (values[i].getString().equals(removedId)) {
                            Value[] newValues = new Value[values.length - 1];
                            if (i > 0) {
                                System.arraycopy(values, 0, newValues, 0, i);
                            }
                            if (values.length - i > 1) {
                                System.arraycopy(values, i + 1, newValues, i, values.length - i - 1);
                            }
                            JcrUtils.ensureIsCheckedOut(refProp.getParent());
                            refProp.setValue(newValues);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static class ComputeSetComparator implements Comparator<Node> {

        @Override
        public int compare(Node o1, Node o2) {
            try {
                int comparison = o1.getPath().length() - o2.getPath().length();
                if (comparison == 0) {
                    return o1.getPath().compareTo(o2.getPath());
                } else {
                    return comparison;
                }
            } catch (RepositoryException ex) {
                log.error("Error while comparing nodes: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                return 0;
            }
        }
    }

}
