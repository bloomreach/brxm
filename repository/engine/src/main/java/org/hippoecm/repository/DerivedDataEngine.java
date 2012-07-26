/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.repository;

import java.security.AccessControlException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.ext.DerivedDataFunction;

public class DerivedDataEngine {

    protected static final Logger logger = LoggerFactory.getLogger(DerivedDataEngine.class);

    HippoSession session;

    public DerivedDataEngine(HippoSession session) {
        this.session = session;
    }

    public void save() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        save(null);
    }

    public void save(Node node) throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        long start = 0;
        if (!session.nodeExists("/hippo:configuration/hippo:derivatives")) {
            return;
        }
        if(logger.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }
        try {
            if(logger.isDebugEnabled())
                logger.debug("Derived engine active");
            Set<Node> recomputeSet = new TreeSet<Node>(new Comparator<Node>() {
                    public int compare(Node o1, Node o2) {
                        try {
                            int comparison = o1.getPath().length() - o2.getPath().length();
                            if (comparison == 0) {
                                return o1.getPath().compareTo(o2.getPath());
                            } else {
                                return comparison;
                            }
                        } catch (RepositoryException ex) {
                            logger.error("Error while comparing nodes: "+ex.getClass().getName()+": "+ex.getMessage(), ex);
                            return 0;
                        }
                    }
                    public boolean equals(Object obj) {
                        return obj == this;
                    }
                });
            try {
                for(NodeIterator iter = session.pendingChanges(node,"mix:referenceable"); iter.hasNext(); ) {
                    Node modified = iter.nextNode();
                    if(modified == null) {
                        logger.error("Unable to access node that was changed by own session");
                        continue;
                    }
                    if(logger.isDebugEnabled()) {
                        logger.debug("Derived engine found modified referenceable node " + modified.getPath() +
                                     " with " + modified.getReferences().getSize() + " references");
                    }
                    for(PropertyIterator i = modified.getReferences(); i.hasNext(); ) {
                        try {
                            Node dependency = i.nextProperty().getParent();
                            if(dependency.isNodeType(HippoNodeType.NT_DERIVED))
                                recomputeSet.add(dependency);
                        } catch(AccessDeniedException ex) {
                            logger.error(ex.getClass().getName()+": "+ex.getMessage());
                            throw new RepositoryException(ex); // configuration problem
                        } catch(ItemNotFoundException ex) {
                            logger.error(ex.getClass().getName()+": "+ex.getMessage());
                            throw new RepositoryException(ex); // inconsistent state
                        }
                    }
                }
            } catch(NamespaceException ex) {
                logger.error(ex.getClass().getName()+": "+ex.getMessage()); // internal error jcr:uuid not accessible
                throw new RepositoryException("Internal error accessing jcr:uuid");
            } catch(NoSuchNodeTypeException ex) {
                logger.error(ex.getClass().getName()+": "+ex.getMessage()); // internal error jcr:uuid not found
                throw new RepositoryException("Internal error jcr:uuid not found");
            }
            try {
                for(NodeIterator iter = session.pendingChanges(node,HippoNodeType.NT_DERIVED); iter.hasNext(); ) {
                    Node modified = iter.nextNode();
                    if(modified == null) {
                        logger.error("Unable to access node that was changed by own session");
                        continue;
                    }
                    if(logger.isDebugEnabled())
                        logger.debug("Derived engine found "+modified.getPath()+" "+(modified.isNodeType("mix:referenceable")?modified.getUUID():"")+" with derived mixin");
                    recomputeSet.add(modified);
                }
            } catch(NamespaceException ex) {
                logger.error(ex.getClass().getName()+": "+ex.getMessage());
                throw new RepositoryException("Internal error "+HippoNodeType.NT_DERIVED+" not found");
            } catch(NoSuchNodeTypeException ex) {
                logger.error(ex.getClass().getName()+": "+ex.getMessage());
                throw new RepositoryException("Internal error "+HippoNodeType.NT_DERIVED+" not found");
            }

            if(logger.isDebugEnabled()) {
                logger.debug("Derived engine found " + recomputeSet.size() + " nodes to be evaluated in " +
                             (System.currentTimeMillis() - start) + " ms");
            }


            if(recomputeSet.size() == 0)
                return;

            Node derivatesFolder = session.getRootNode().getNode("hippo:configuration/hippo:derivatives");
            for(Node modified : recomputeSet) {
                compute(session.getValueFactory(), derivatesFolder, modified);
            }
        } catch(NamespaceException ex) {
            // be lenient against confiuration problems
            logger.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch(ConstraintViolationException ex) {
            logger.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } finally {
            if(logger.isDebugEnabled()) {
                logger.debug("Derived engine done in " + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }

    public void validate() throws ConstraintViolationException, RepositoryException {
        int totalCount = 0, changedCount = 0;
        ValueFactory valueFactory = session.getValueFactory();
        Node derivatesFolder = session.getRootNode().getNode("hippo:configuration/hippo:derivatives");
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM hippo:derived", Query.SQL);
        QueryResult result = query.execute();
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            ++totalCount;
            Node node = iter.nextNode();
            if(compute(valueFactory, derivatesFolder, node)) {
                ++changedCount;
                if((changedCount % LocalHippoRepository.batchThreshold) == 0) {
                    session.save();
                }
            }
        }
        logger.warn("Validated "+totalCount+" nodes, and reset "+changedCount+" nodes");
        session.save();
    }

    private final boolean compute(ValueFactory valueFactory, Node derivatesFolder, Node modified) throws ConstraintViolationException, RepositoryException {
                if(!modified.isCheckedOut()) {
                    Node ancestor = modified;
                    while (!ancestor.isNodeType("mix:versionable")) {
                        ancestor = ancestor.getParent();
                    }
                    ancestor.checkout();
                }
                SortedSet<String> dependencies = new TreeSet<String>();

                for(NodeIterator funcIter = derivatesFolder.getNodes(); funcIter.hasNext(); ) {
                    Node function = funcIter.nextNode();
                    if(function == null) {
                        logger.error("unable to access all derived data functions");
                        continue;
                    }
                    try {
                        String nodetypeName = function.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString();
                        if(modified.isNodeType(nodetypeName)) {
                            if(logger.isDebugEnabled()) {
                                logger.debug("Derived node " + modified.getPath() + " is of derived type as defined in " +
                                             function.getPath());
                            }
                            /* preparation: build the map of parameters to be fed to
                             * the function and instantiate the class containing the
                             * compute function.
                             */
                            NodeType nodetype = session.getWorkspace().getNodeTypeManager().getNodeType(nodetypeName);
                            Map<String,Value[]> parameters = new TreeMap<String,Value[]>();
                            Class clazz = Class.forName(function.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString());
                            DerivedDataFunction func = (DerivedDataFunction) clazz.newInstance();
                            func.setValueFactory(valueFactory);

                            /* Now populate the parameters map to be fed to the
                             * compute function.
                             */
                            for(NodeIterator propDefIter = function.getNode("hipposys:accessed").getNodes(); propDefIter.hasNext(); ) {
                                Node propDef = propDefIter.nextNode();
                                if(propDef == null) {
                                    logger.error("unable to access derived data accessed property definition");
                                    continue;
                                }
                                String propName = propDef.getName();
                                if(propDef.isNodeType("hipposys:builtinpropertyreference")) {
                                    String builtinFunction = propDef.getProperty("hipposys:method").getString();
                                    if(builtinFunction.equals("ancestors")) {
                                        Vector<Value> ancestors = new Vector<Value>();
                                        Node ancestor = modified;
                                        while(ancestor != null) {
                                            if(ancestor.isNodeType("mix:referenceable")) {
                                                try {
                                                    ancestors.add(valueFactory.createValue(ancestor.getUUID()));
                                                } catch(UnsupportedRepositoryOperationException ex) {
                                                    // cannot happen because of check on mix:referenceable
                                                    logger.error("Impossible state reached");
                                                }
                                            }
                                            try {
                                                ancestor = ancestor.getParent();
                                            } catch(ItemNotFoundException ex) {
                                                ancestor = null; // valid exception outcome, no parent because we are at root
                                            }
                                        }
                                        parameters.put(propName, ancestors.toArray(new Value[ancestors.size()]));
                                    } else {
                                        logger.warn("Derived data definition contains unrecognized builtin reference, skipped");
                                    }
                                } else if(propDef.isNodeType("hipposys:relativepropertyreference")) {
                                    if(modified.hasProperty(propDef.getProperty("hipposys:relPath").getString())) {
                                        Property property = modified.getProperty(propDef.getProperty("hipposys:relPath").getString());
                                        if(property.getParent().isNodeType("mix:referenceable")) {
                                            dependencies.add(property.getParent().getUUID());
                                        }
                                        if(!property.getDefinition().isMultiple()) {
                                            Value[] values = new Value[1];
                                            values[0] = property.getValue();
                                            parameters.put(propName, values);
                                        } else
                                            parameters.put(propName, property.getValues());
                                    }
                                } else if(propDef.isNodeType("hipposys:resolvepropertyreference")) {
                                    /* FIXME: should read:
                                     * Property property = ((HippoWorkspace)(modified.getSession().getWorkspace())).getHierarchyResolver().getProperty(modified, propDef.getProperty("hipposys:relPath").getString());
                                     * however this is broken because of a cast exception as the session is not wrapped
                                     */
                                    HierarchyResolver.Entry lastNode = new HierarchyResolver.Entry();
                                    Property property = new HierarchyResolverImpl().getProperty(modified, propDef.getProperty("hipposys:relPath").getString(), lastNode);
                                    if(property != null) {
                                        if(property.getParent().isNodeType("mix:referenceable")) {
                                            dependencies.add(property.getParent().getUUID());
                                        }
                                        if(!property.getDefinition().isMultiple()) {
                                            Value[] values = new Value[1];
                                            values[0] = property.getValue();
                                            parameters.put(propName, values);
                                        } else {
                                            parameters.put(propName, property.getValues());
                                        }
                                    } else {
                                        if(lastNode.node.isNodeType("mix:referenceable")) {
                                            dependencies.add(lastNode.node.getUUID());
                                        }
                                    }
                                } else {
                                    logger.warn("Derived data definition contains unrecognized reference, skipped");
                                }
                            }
                            /* Perform the computation.
                             */
                            parameters = func.compute(parameters);
                            /* Use the definition of the derived properties to set the
                             * properties computed by the function.
                             */
                            for(NodeIterator propDefIter = function.getNode(HippoNodeType.HIPPO_DERIVED).getNodes(); propDefIter.hasNext(); ) {
                                Node propDef = propDefIter.nextNode();
                                if(propDef == null) {
                                    logger.error("unable to access derived data derived property definition");
                                    continue;
                                }
                                String propName = propDef.getName();
                                if(propDef.isNodeType("hipposys:relativepropertyreference")) {
                                    String propertyPath = propDef.getProperty("hipposys:relPath").getString();
                                    StringBuffer sb = null;
                                    if(logger.isDebugEnabled()) {
                                        sb = new StringBuffer();
                                        sb.append("derived property ");
                                        sb.append(propertyPath);
                                        sb.append(" in ");
                                        sb.append(modified.getPath());
                                        sb.append(" derived using ");
                                        sb.append(propName);
                                        sb.append(" valued ");
                                    }
                                    Node targetModifiedNode = modified;
                                    String targetModifiedPropertyPath = propertyPath;
                                    NodeType targetModifiedNodetype = nodetype;
                                    while(targetModifiedPropertyPath.contains("/")  && !targetModifiedPropertyPath.startsWith("..")) {
                                        String pathElement = targetModifiedPropertyPath.substring(0, targetModifiedPropertyPath.indexOf("/"));
                                        if(targetModifiedNode != null) {
                                            if(targetModifiedNode.hasNode(pathElement)) {
                                                targetModifiedNode = targetModifiedNode.getNode(pathElement);
                                            } else if(parameters.containsKey(propName)) {
                                                targetModifiedNode = targetModifiedNode.addNode(pathElement);
                                            } else {
                                                targetModifiedNode = null;
                                            }
                                        }
                                        targetModifiedPropertyPath = targetModifiedPropertyPath.substring(targetModifiedPropertyPath.indexOf("/")+1);
                                        if(!targetModifiedPropertyPath.contains("/")) {
                                            if(targetModifiedNode != null) {
                                                targetModifiedNodetype = targetModifiedNode.getPrimaryNodeType();
                                            } else {
                                                targetModifiedNodetype = null;
                                            }
                                        }
                                    }
                                    if(targetModifiedNode != null && targetModifiedNode.hasProperty(targetModifiedPropertyPath)) {
                                        Property property = targetModifiedNode.getProperty(targetModifiedPropertyPath);
                                        if(!property.getDefinition().isMultiple()) {
                                            Value[] values = parameters.get(propName);
                                            if(values != null && values.length >= 1) {
                                                if(!property.getValue().equals(values[0])) {
                                                    try {
                                                        property.getSession().checkPermission(property.getPath(), Privilege.JCR_MODIFY_PROPERTIES);
                                                        property.setValue(values[0]);
                                                        if(logger.isDebugEnabled()) {
                                                            sb.append(values[0].getString());
                                                            sb.append(" overwritten");
                                                        }
                                                    } catch(AccessControlException ex) {
                                                        logger.warn("cannot update "+(sb!=null?new String(sb):modified.getPath()));
                                                        if(logger.isDebugEnabled()) {
                                                            sb.append(" failed");
                                                        }
                                                    }
                                                } else {
                                                    if(logger.isDebugEnabled()) {
                                                        sb.append(values[0].getString());
                                                        sb.append(" unchanged");
                                                    }
                                                }
                                            } else {
                                                try {
                                                    property.getSession().checkPermission(property.getPath(), Privilege.JCR_MODIFY_PROPERTIES);
                                                    property.remove();
                                                    if(logger.isDebugEnabled()) {
                                                        sb.append(" removed");
                                                    }
                                                } catch(AccessControlException ex) {
                                                    logger.warn("cannot update "+(sb!=null?new String(sb):modified.getPath()));
                                                    if(logger.isDebugEnabled()) {
                                                        sb.append(" failed");
                                                    }
                                                }
                                            }
                                        } else {
                                            Value[] values = parameters.get(propName);
                                            boolean changed = false;
                                            if(values.length == property.getValues().length) {
                                                Value[] oldValues = property.getValues();
                                                for(int i=0; i<values.length; i++)
                                                    if(!values[i].equals(oldValues[i])) {
                                                        changed = true;
                                                        break;
                                                    }
                                            } else
                                                changed = true;
                                            if(logger.isDebugEnabled()) {
                                                sb.append("{");
                                                for(int i=0; i<values.length; i++) {
                                                    sb.append(i==0 ? " " : ", ");
                                                    sb.append(values[i].getString());
                                                }
                                                sb.append(" }");
                                            }
                                            if(changed) {
                                                try {
                                                    property.getSession().checkPermission(property.getPath(), Privilege.JCR_MODIFY_PROPERTIES);
                                                    property.setValue(parameters.get(propName));
                                                    if(logger.isDebugEnabled()) {
                                                        sb.append(" overwritten");
                                                    }
                                                } catch(AccessControlException ex) {
                                                    logger.warn("cannot update "+(sb!=null?new String(sb):modified.getPath()));
                                                    if(logger.isDebugEnabled()) {
                                                        sb.append(" failed");
                                                    }
                                                }
                                            } else {
                                                if(logger.isDebugEnabled()) {
                                                    sb.append(" unchanged");
                                                }
                                            }
                                        }
                                    } else {
                                        PropertyDefinition derivedPropDef = null;
                                        if(targetModifiedNodetype != null) {
                                            derivedPropDef = getPropertyDefinition(targetModifiedNodetype, targetModifiedPropertyPath);
                                        }
                                        if(derivedPropDef == null || !derivedPropDef.isMultiple()) {
                                            Value[] values = parameters.get(propName);
                                            if(values != null && values.length >= 1) {
                                                    try {
                                                        if(!targetModifiedNode.isCheckedOut()) {
                                                            targetModifiedNode.checkout(); // FIXME: is this node always versionalble?
                                                        }
                                                        targetModifiedNode.setProperty(targetModifiedPropertyPath, values[0]);
                                                        if(logger.isDebugEnabled()) {
                                                            sb.append(values[0].getString());
                                                            sb.append(" created");
                                                        }
                                                    } catch(AccessControlException ex) {
                                                        logger.warn("cannot update "+(sb!=null?new String(sb):modified.getPath()));
                                                        if(logger.isDebugEnabled()) {
                                                            sb.append(" failed");
                                                        }
                                                    }
                                            } else {
                                                if(logger.isDebugEnabled()) {
                                                    sb.append(" skipped");
                                                }
                                            }
                                        } else {
                                            Value[] values = parameters.get(propName);
                                            if(logger.isDebugEnabled()) {
                                                sb.append("{");
                                                for(int i=0; i<values.length; i++) {
                                                    sb.append(i==0 ? " " : ", ");
                                                    sb.append(values[i].getString());
                                                }
                                                sb.append(" }");
                                            }
                                            try {
                                                if(!targetModifiedNode.isCheckedOut()) {
                                                    targetModifiedNode.checkout(); // FIXME: is this node always versionalble?
                                                }
                                                targetModifiedNode.setProperty(targetModifiedPropertyPath, values);
                                                if(logger.isDebugEnabled()) {
                                                    sb.append(" created");
                                                }
                                            } catch(AccessControlException ex) {
                                                logger.warn("cannot update "+(sb!=null?new String(sb):modified.getPath()));
                                                if(logger.isDebugEnabled()) {
                                                    sb.append(" failed");
                                                }
                                            }
                                        }
                                    }
                                    if(logger.isDebugEnabled()) {
                                        logger.debug(new String(sb));
                                    }
                                } else {
                                    logger.warn("Derived data definition contains unrecognized reference " +
                                                propDef.getPrimaryNodeType().getName()+", skipped");
                                }
                            }
                        }
                    } catch(AccessDeniedException ex) {
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // should not be possible
                    } catch(ItemNotFoundException ex) {
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // impossible
                    } catch(PathNotFoundException ex) {
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // impossible
                    } catch(ValueFormatException ex) {
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // impossible
                    } catch(ClassNotFoundException ex) {
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // impossible
                    } catch(InstantiationException ex) {
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // impossible
                    } catch(IllegalAccessException ex) {
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // impossible
                    }
                }

                if (modified.isNodeType("mix:referenceable")) {
                    dependencies.remove(modified.getUUID());
                }
                Value[] dependenciesValues = new Value[dependencies.size()];
                int i = 0;
                for(String dependency : dependencies) {
                    dependenciesValues[i++] = valueFactory.createValue(dependency, PropertyType.REFERENCE);
                }
                Value[] oldDependenciesValues = null;
                if(modified.hasProperty(HippoNodeType.HIPPO_RELATED)) {
                    oldDependenciesValues = modified.getProperty(HippoNodeType.HIPPO_RELATED).getValues();
                }
                boolean changed = false;
                if(oldDependenciesValues != null && dependenciesValues.length == oldDependenciesValues.length) {
                    for(i=0; i<dependenciesValues.length; i++)
                        if(!dependenciesValues[i].equals(oldDependenciesValues[i])) {
                            changed = true;
                            break;
                        }
                } else
                    changed = true;
                if(changed) {
                    try {
                        if(!modified.isCheckedOut()) {
                            modified.checkout(); // FIXME: is this node always versionalble?
                        }
                        modified.setProperty(HippoNodeType.HIPPO_RELATED, dependenciesValues, PropertyType.REFERENCE);
                    } catch(ItemNotFoundException ex) {
                        logger.info("write error on modified node "+modified.getPath(), ex);
                    }
                }
                return changed;
    }

    public static void removal(Node removed) throws RepositoryException {
        if(removed.isNodeType("mix:referenceable")) {
            final String uuid = removed.getUUID();
            removed.accept(new ItemVisitor() {
                    public void visit(Property property) throws RepositoryException {
                    }
                    public void visit(Node node) throws RepositoryException {
                        for(PropertyIterator iter = node.getReferences(); iter.hasNext(); ) {
                            Property prop = iter.nextProperty();
                            if(prop.getDefinition().getName().equals(HippoNodeType.HIPPO_RELATED)) {
                                Value[] values = prop.getValues();
                                for(int i=0; i<values.length; i++) {
                                    if(values[i].getString().equals(uuid)) {
                                        Value[] newValues = new Value[values.length - 1];
                                        if(i > 0)
                                            System.arraycopy(values, 0, newValues, 0, i);
                                        if(values.length - i > 1)
                                            System.arraycopy(values, i + 1, newValues, i, values.length - i - 1);
                                        Node ancestor = prop.getParent();
                                        if (!ancestor.isCheckedOut()) {
                                            while (!ancestor.isNodeType("mix:versionable")) {
                                                ancestor = ancestor.getParent();
                                            }
                                            ancestor.checkout();
                                        }
                                        prop.setValue(values);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                });
        }
    }

    private PropertyDefinition getPropertyDefinition(NodeType nodetype, String propertyPath) {
        PropertyDefinition[] definitions = nodetype.getPropertyDefinitions();
        for(PropertyDefinition propDef : definitions) {
            if(propDef.getName().equals(propertyPath)) {
                return propDef;
            }
        }
        return null;
    }

    public static class CoreDerivedDataFunction extends DerivedDataFunction {
        static final long serialVersionUID = 1;
        public Map<String,Value[]> compute(Map<String,Value[]> parameters) {
            return parameters;
        }
    }

    public static class CopyDerivedDataFunction extends DerivedDataFunction {
        static final long serialVersionUID = 1;
        public Map<String,Value[]> compute(Map<String,Value[]> parameters) {
            Value[] source = parameters.get("source");
            if(source != null) {
                Value[] destination = new Value[source.length];
                System.arraycopy(source, 0, destination, 0, source.length);
                parameters.put("destination", destination);
            }
            return parameters;
        }
    }
}
