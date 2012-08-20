/*
 *  Copyright 2012 Hippo.
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

import java.security.AccessControlException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.security.Privilege;

import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.HierarchyResolver;

class PropertyMapper {
    private final FunctionDescription function;
    private final Node modified;
    private final ValueFactory valueFactory;
    private final NodeType nodeType;

    PropertyMapper(final FunctionDescription function, final Node modified, final ValueFactory valueFactory, final NodeType nodeType) {
        this.function = function;
        this.modified = modified;
        this.valueFactory = valueFactory;
        this.nodeType = nodeType;
    }

    /* Creates a parameters map to be fed to the compute function.
    */
    Map<String, Value[]> getParameters(final SortedSet<String> dependencies) throws RepositoryException {
        Map<String, Value[]> parameters = new TreeMap<String, Value[]>();
        for (PropertyReference reference : function.getAccessedProperties()) {
            String propName = reference.getName();
            switch (reference.getType()) {
                case BUILTIN:
                    String builtinFunction = reference.getMethod();
                    if (builtinFunction.equals("ancestors")) {
                        Vector<Value> ancestors = new Vector<Value>();
                        Node ancestor = modified;
                        while (ancestor != null) {
                            if (ancestor.isNodeType("mix:referenceable")) {
                                try {
                                    ancestors.add(valueFactory.createValue(ancestor.getIdentifier()));
                                } catch (UnsupportedRepositoryOperationException ex) {
                                    // cannot happen because of check on mix:referenceable
                                    DerivedDataEngine.log.error("Impossible state reached");
                                }
                            }
                            try {
                                ancestor = ancestor.getParent();
                            } catch (ItemNotFoundException ex) {
                                ancestor = null; // valid exception outcome, no parent because we are at root
                            }
                        }
                        parameters.put(propName, ancestors.toArray(new Value[ancestors.size()]));
                    } else {
                        DerivedDataEngine.log.warn("Derived data definition contains unrecognized builtin reference, skipped");
                    }
                    break;

                case RELATIVE:
                    if (modified.hasProperty(reference.getRelativePath())) {
                        Property property = modified.getProperty(reference.getRelativePath());
                        if (property.getParent().isNodeType("mix:referenceable")) {
                            dependencies.add(property.getParent().getIdentifier());
                        }
                        if (!property.getDefinition().isMultiple()) {
                            Value[] values = new Value[1];
                            values[0] = property.getValue();
                            parameters.put(propName, values);
                        } else {
                            parameters.put(propName, property.getValues());
                        }
                    }
                    break;

                case RESOLVE:
                    /* FIXME: should read:
                    * Property property = ((HippoWorkspace)(modified.getSession().getWorkspace())).getHierarchyResolver().getProperty(modified, propDef.getProperty("hipposys:relPath").getString());
                    * however this is broken because of a cast exception as the session is not wrapped
                    */
                    HierarchyResolver.Entry lastNode = new HierarchyResolver.Entry();
                    Property property = new HierarchyResolverImpl().getProperty(modified,
                                                                                reference.getRelativePath(),
                                                                                lastNode);
                    if (property != null) {
                        if (property.getParent().isNodeType("mix:referenceable")) {
                            dependencies.add(property.getParent().getIdentifier());
                        }
                        if (!property.getDefinition().isMultiple()) {
                            Value[] values = new Value[1];
                            values[0] = property.getValue();
                            parameters.put(propName, values);
                        } else {
                            parameters.put(propName, property.getValues());
                        }
                    } else {
                        if (lastNode.node.isNodeType("mix:referenceable")) {
                            dependencies.add(lastNode.node.getIdentifier());
                        }
                    }
                    break;

                default:
                    DerivedDataEngine.log.warn("Derived data definition contains unrecognized reference, skipped");
            }
        }
        return parameters;
    }

    /* Use the definition of the derived properties to set the
    * properties computed by the function.
    */
    void persistValues(final Map<String, Value[]> parameters) throws RepositoryException {
        for (PropertyReference reference : function.getDerivedProperties()) {
            String propName = reference.getName();
            if (reference.getType() == PropertyReferenceType.RELATIVE) {
                String propertyPath = reference.getRelativePath();
                Node targetModifiedNode = modified;
                String targetModifiedPropertyPath = propertyPath;
                NodeType targetModifiedNodetype = nodeType;
                while (targetModifiedPropertyPath.contains("/") && !targetModifiedPropertyPath.startsWith("..")) {
                    String pathElement = targetModifiedPropertyPath.substring(0, targetModifiedPropertyPath.indexOf(
                            "/"));
                    if (targetModifiedNode != null) {
                        if (targetModifiedNode.hasNode(pathElement)) {
                            targetModifiedNode = targetModifiedNode.getNode(pathElement);
                        } else if (parameters.containsKey(propName)) {
                            targetModifiedNode = targetModifiedNode.addNode(pathElement);
                        } else {
                            targetModifiedNode = null;
                        }
                    }
                    targetModifiedPropertyPath = targetModifiedPropertyPath.substring(
                            targetModifiedPropertyPath.indexOf("/") + 1);
                    if (!targetModifiedPropertyPath.contains("/")) {
                        if (targetModifiedNode != null) {
                            targetModifiedNodetype = targetModifiedNode.getPrimaryNodeType();
                        } else {
                            targetModifiedNodetype = null;
                        }
                    }
                }
                PropertyUpdateLogger updateLogger = new PropertyUpdateLogger(propertyPath, propName, modified, DerivedDataEngine.log);
                Value[] values = parameters.get(propName);
                if (targetModifiedNode != null && targetModifiedNode.hasProperty(targetModifiedPropertyPath)) {
                    Property property = targetModifiedNode.getProperty(targetModifiedPropertyPath);
                    updateProperty(values, updateLogger, property);
                } else {
                    addProperty(values, updateLogger, targetModifiedNode, targetModifiedPropertyPath,
                                targetModifiedNodetype);
                }
                updateLogger.flush();
            } else {
                DerivedDataEngine.log.warn("Derived data definition contains unrecognized reference type " +
                                    reference.getType().name() + ", skipped");
            }
        }
    }

    private void updateProperty(Value[] values, final PropertyUpdateLogger pul, Property property) throws RepositoryException {
        if (!property.getDefinition().isMultiple()) {
            if (values != null && values.length >= 1) {
                if (!property.getValue().equals(values[0])) {
                    try {
                        property.getSession().checkPermission(property.getPath(), Privilege.JCR_MODIFY_PROPERTIES);
                        property.setValue(values[0]);
                        pul.overwritten(values[0]);
                    } catch (AccessControlException ex) {
                        DerivedDataEngine.log.warn("cannot update " + modified.getPath());
                        pul.failed();
                    }
                } else {
                    pul.unchanged(values[0]);
                }
            } else {
                try {
                    property.getSession().checkPermission(property.getPath(), Privilege.JCR_MODIFY_PROPERTIES);
                    property.remove();
                    pul.removed();
                } catch (AccessControlException ex) {
                    DerivedDataEngine.log.warn("cannot update " + modified.getPath());
                    pul.failed();
                }
            }
        } else {
            boolean changed = false;
            if (values.length == property.getValues().length) {
                Value[] oldValues = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (!values[i].equals(oldValues[i])) {
                        changed = true;
                        break;
                    }
                }
            } else {
                changed = true;
            }
            if (changed) {
                try {
                    property.getSession().checkPermission(property.getPath(), Privilege.JCR_MODIFY_PROPERTIES);
                    property.setValue(values);
                    pul.overwritten(values);
                } catch (AccessControlException ex) {
                    DerivedDataEngine.log.warn("cannot update " + modified.getPath());
                    pul.failed();
                }
            } else {
                pul.unchanged(values);
            }
        }
    }

    private void addProperty(Value[] values, final PropertyUpdateLogger pul, final Node targetModifiedNode, final String targetModifiedPropertyPath, final NodeType targetModifiedNodetype) throws RepositoryException {
        PropertyDefinition derivedPropDef = null;
        if (targetModifiedNodetype != null) {
            derivedPropDef = getPropertyDefinition(targetModifiedNodetype, targetModifiedPropertyPath);
        }
        if (derivedPropDef == null || !derivedPropDef.isMultiple()) {
            if (values != null && values.length >= 1) {
                try {
                    if (!targetModifiedNode.isCheckedOut()) {
                        targetModifiedNode.checkout(); // FIXME: is this node always versionalble?
                    }
                    targetModifiedNode.setProperty(targetModifiedPropertyPath, values[0]);
                    pul.created(values[0]);
                } catch (AccessControlException ex) {
                    DerivedDataEngine.log.warn("cannot update " + modified.getPath());
                    pul.failed();
                }
            } else {
                pul.skipped();
            }
        } else {
            try {
                if (!targetModifiedNode.isCheckedOut()) {
                    targetModifiedNode.checkout(); // FIXME: is this node always versionalble?
                }
                targetModifiedNode.setProperty(targetModifiedPropertyPath, values);
                pul.created(values);
            } catch (AccessControlException ex) {
                DerivedDataEngine.log.warn("cannot update " + modified.getPath());
                pul.failed();
            }
        }
    }

    private static PropertyDefinition getPropertyDefinition(NodeType nodetype, String propertyPath) {
        PropertyDefinition[] definitions = nodetype.getPropertyDefinitions();
        for (PropertyDefinition propDef : definitions) {
            if (propDef.getName().equals(propertyPath)) {
                return propDef;
            }
        }
        return null;
    }

}
