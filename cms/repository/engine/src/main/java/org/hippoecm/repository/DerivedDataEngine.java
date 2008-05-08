/*
 * Copyright 2007 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.repository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;

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
      try {
        ValueFactory valueFactory = session.getValueFactory();

        if(logger.isDebugEnabled())
            logger.debug("Derived engine active");
        Set<Node> recomputeSet = new HashSet<Node>();
        try {
            for(NodeIterator iter = session.pendingChanges(node,"jcr:uuid"); iter.hasNext(); ) {
                Node modified = iter.nextNode();
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
                        // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                        // ex.printStackTrace(System.err);
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // configuration problem
                    } catch(ItemNotFoundException ex) {
                        // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                        // ex.printStackTrace(System.err);
                        logger.error(ex.getClass().getName()+": "+ex.getMessage());
                        throw new RepositoryException(ex); // inconsistent state
                    }
                }
            }
        } catch(NamespaceException ex) {
            // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            // ex.printStackTrace(System.err);
            logger.error(ex.getClass().getName()+": "+ex.getMessage()); // internal error jcr:uuid not accessible
            throw new RepositoryException("Internal error accessing jcr:uuid");
        } catch(NoSuchNodeTypeException ex) {
            // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            // ex.printStackTrace(System.err);
            logger.error(ex.getClass().getName()+": "+ex.getMessage()); // internal error jcr:uuid not found
            throw new RepositoryException("Internal error jcr:uuid not found");
        }
        try {
            for(NodeIterator iter = session.pendingChanges(node,HippoNodeType.NT_DERIVED); iter.hasNext(); ) {
                Node modified = iter.nextNode();
                if(logger.isDebugEnabled())
                    logger.debug("Derived engine found node "+modified.getPath()+" with derived mixin");
                recomputeSet.add(modified);
            }
        } catch(NamespaceException ex) {
            // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            // ex.printStackTrace(System.err);
            logger.error(ex.getClass().getName()+": "+ex.getMessage());
            throw new RepositoryException("Internal error "+HippoNodeType.NT_DERIVED+" not found");
        } catch(NoSuchNodeTypeException ex) {
            // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            // ex.printStackTrace(System.err);
            logger.error(ex.getClass().getName()+": "+ex.getMessage());
            throw new RepositoryException("Internal error "+HippoNodeType.NT_DERIVED+" not found");
        }

        if(logger.isDebugEnabled())
            logger.debug("Derived engine found "+recomputeSet.size()+" nodes to be evaluated");

        if(recomputeSet.size() == 0)
            return;

        Node derivatesFolder = session.getRootNode().getNode("hippo:configuration/hippo:derivatives");
        for(Node modified : recomputeSet) {

            modified.setProperty(HippoNodeType.HIPPO_RELATED, new Value[0]);
            Set<String> dependencies = new HashSet<String>();

            for(NodeIterator funcIter = derivatesFolder.getNodes();
                funcIter.hasNext(); ) {
                Node function = funcIter.nextNode();
                try {
                    String nodetypeName = function.getProperty(HippoNodeType.HIPPO_NODETYPE).getString();
                    if(modified.isNodeType(nodetypeName)) {
                        if(logger.isDebugEnabled())
                            logger.debug("Derived node "+modified.getPath()+" is of derived type as defined in "+function.getPath());
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
                        for(NodeIterator propDefIter = function.getNode("hippo:accessed").getNodes(); propDefIter.hasNext(); ) {
                            Node propDef = propDefIter.nextNode();
                            String propName = propDef.getName();
                            if(propDef.isNodeType("hippo:builtinpropertyreference")) {
                                String builtinFunction = propDef.getProperty("hippo:method").getString();
                                if(builtinFunction.equals("ancestors")) {
                                    Vector<Value> ancestors = new Vector<Value>();
                                    Node ancestor = modified;
                                    while(ancestor != null) {
                                        try {
                                            ancestor = ancestor.getParent();
                                        } catch(ItemNotFoundException ex) {
                                            ancestor = null; // valid exception outcome, no parent because we are at root
                                        }
                                        if(ancestor != null && ancestor.isNodeType("mix:referenceable")) {
                                            try {
                                                ancestors.add(valueFactory.createValue(ancestor.getUUID()));
                                            } catch(UnsupportedRepositoryOperationException ex) {
                                                // cannot happen because of check on mix:referenceable
                                                logger.error("Impossible state reached");
                                            }
                                        }
                                    }
                                    parameters.put(propName, ancestors.toArray(new Value[ancestors.size()]));
                                } else {
                                    logger.warn("Derived data definition contains unrecognized builtin reference, skipped");
                                }
                            } else if(propDef.isNodeType("hippo:relativepropertyreference")) {
                                Property property = modified.getProperty(propDef.getProperty("hippo:relPath").getString());
                                if(property.getParent().isNodeType("mix:referenceable")) {
                                    dependencies.add(property.getParent().getUUID());
                                }
                                if(!property.getDefinition().isMultiple()) {
                                    Value[] values = new Value[1];
                                    values[0] = property.getValue();
                                    parameters.put(propName, values);
                                } else
                                    parameters.put(propName, property.getValues());
                            } else if(propDef.isNodeType("hippo:resolvepropertyreference")) {
                                Property property = ((HippoWorkspace)modified.getSession().getWorkspace()).getHierarchyResolver().getProperty(modified, propDef.getProperty("hippo:relPath").getString());
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
                        for(NodeIterator propDefIter=function.getNode(HippoNodeType.NT_DERIVED).getNodes();propDefIter.hasNext();) {
                            Node propDef = propDefIter.nextNode();
                            String propName = propDef.getName();
                            if(propDef.isNodeType("hippo:relativepropertyreference")) {
                                String propertyPath = propDef.getProperty("hippo:relPath").getString();
                                StringBuffer sb = null;
                                if(logger.isDebugEnabled()) {
                                    sb = new StringBuffer();
                                    sb.append("Derived property ");
                                    sb.append(propertyPath);
                                    sb.append(" in ");
                                    sb.append(modified.getPath());
                                    sb.append(" derived using ");
                                    sb.append(propName);
                                    sb.append(" valued ");
                                }
                                if(modified.hasProperty(propertyPath)) {
                                    Property property = modified.getProperty(propertyPath);
                                    if(!property.getDefinition().isMultiple()) {
                                        Value[] values = parameters.get(propName);
                                        if(values != null && values.length >= 1) {
                                            property.setValue(values[0]);
                                            if(logger.isDebugEnabled()) {
                                                sb.append(values[0].getString());
                                                sb.append(" overwritten");
                                            }
                                        } else {
                                            property.remove();
                                            if(logger.isDebugEnabled()) {
                                                sb.append(" removed");
                                            }
                                        }
                                    } else {
                                        Value[] values = parameters.get(propName);
                                        property.setValue(parameters.get(propName));
                                        if(logger.isDebugEnabled()) {
                                            sb.append("{");
                                            for(int i=0; i<values.length; i++) {
                                                sb.append(i==0 ? " " : ", ");
                                                sb.append(values[i].getString());
                                            }
                                            sb.append(" } overwritten");
                                        }
                                    }
                                } else {
                                    PropertyDefinition derivedPropDef = getPropertyDefinition(nodetype, propertyPath);
                                    if(!derivedPropDef.isMultiple()) {
                                        Value[] values = parameters.get(propName);
                                        if(values != null && values.length >= 1) {
                                            modified.setProperty(propertyPath, values[0]);
                                            if(logger.isDebugEnabled()) {
                                                sb.append(values[0].getString());
                                                sb.append(" created");
                                            }
                                        } else {
                                            if(logger.isDebugEnabled()) {
                                                sb.append(" skipped");
                                            }
                                        }
                                    } else {
                                        Value[] values = parameters.get(propName);
                                        modified.setProperty(propertyPath, values);
                                        if(logger.isDebugEnabled()) {
                                            sb.append("{");
                                            for(int i=0; i<values.length; i++) {
                                                sb.append(i==0 ? " " : ", ");
                                                sb.append(values[i].getString());
                                            }
                                            sb.append(" } created");
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
                    // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    // ex.printStackTrace(System.err);
                    logger.error(ex.getClass().getName()+": "+ex.getMessage());
                    throw new RepositoryException(ex); // should not be possible
                } catch(ItemNotFoundException ex) {
                    // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    // ex.printStackTrace(System.err);
                    logger.error(ex.getClass().getName()+": "+ex.getMessage());
                    throw new RepositoryException(ex); // impossible
                } catch(PathNotFoundException ex) {
                    // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    // ex.printStackTrace(System.err);
                    logger.error(ex.getClass().getName()+": "+ex.getMessage());
                    throw new RepositoryException(ex); // impossible
                } catch(ValueFormatException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                    logger.error(ex.getClass().getName()+": "+ex.getMessage());
                    throw new RepositoryException(ex); // impossible
                } catch(ClassNotFoundException ex) {
                    // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    // ex.printStackTrace(System.err);
                    logger.error(ex.getClass().getName()+": "+ex.getMessage());
                    throw new RepositoryException(ex); // impossible
                } catch(InstantiationException ex) {
                    // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    // ex.printStackTrace(System.err);
                    logger.error(ex.getClass().getName()+": "+ex.getMessage());
                    throw new RepositoryException(ex); // impossible
                } catch(IllegalAccessException ex) {
                    // System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    // ex.printStackTrace(System.err);
                    logger.error(ex.getClass().getName()+": "+ex.getMessage());
                    throw new RepositoryException(ex); // impossible
                }
            }

            dependencies.remove(modified.getUUID());
            Value[] dependenciesValues = new Value[dependencies.size()];
            int i = 0;
            for(String dependency : dependencies) {
                dependenciesValues[i++] = valueFactory.createValue(dependency, PropertyType.REFERENCE);
            }
            modified.setProperty(HippoNodeType.HIPPO_RELATED, dependenciesValues);
        }
      } catch(ConstraintViolationException ex) {
          System.err.println(ex.getClass().getName()+": "+ex.getMessage());
          ex.printStackTrace(System.err);
      } catch(RepositoryException ex) {
          System.err.println(ex.getClass().getName()+": "+ex.getMessage());
          ex.printStackTrace(System.err);
      }
    }
    
    public static void removal(Node removed) throws RepositoryException {
        if(removed.isNodeType("jcr:referenceable")) {
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
                                            System.arraycopy(values, 0, newValues, 0, i - 1);
                                        if(values.length - i > 0)
                                            System.arraycopy(values, i + 1, newValues, i, values.length - i);
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

}
