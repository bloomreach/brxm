/*
 * Copyright 2012-2020 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.deriveddata;

import java.security.AccessControlException;
import java.util.Collection;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.security.Privilege;

import org.hippoecm.repository.util.JcrUtils;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_MULTIVALUE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_REL_PATH;
import static org.onehippo.repository.util.JcrConstants.MIX_REFERENCEABLE;

public class RelativePropertyReference extends PropertyReference {

    private boolean multiValue = false;

    public RelativePropertyReference(final Node node, final FunctionDescription function) {
        super(node, function);
        try {
            multiValue = JcrUtils.getBooleanProperty(node, HIPPOSYS_MULTIVALUE, false);
        } catch (RepositoryException e) {
            DerivedDataEngine.log.error("cannot access configuration property", e);
        }
    }

    @Override
    Value[] getPropertyValues(Node modified, Collection<String> dependencies) throws RepositoryException {
        final Property property = JcrUtils.getPropertyIfExists(modified, getRelativePath());
        if (property != null) {
            if (property.getParent().isNodeType(MIX_REFERENCEABLE)) {
                dependencies.add(property.getParent().getIdentifier());
            }
            if (!property.getDefinition().isMultiple()) {
                return new Value[] { property.getValue() };
            } else {
                return property.getValues();
            }
        }
        return null;
    }

    @Override
    boolean persistPropertyValues(final Node modified, final Map<String, Value[]> parameters) throws RepositoryException {
        String propertyPath = getRelativePath();
        Node targetModifiedNode = modified;
        String targetModifiedPropertyPath = propertyPath;
        NodeType targetModifiedNodetype = getApplicableNodeType();
        while (targetModifiedPropertyPath.contains("/") && !targetModifiedPropertyPath.startsWith("..")) {
            String pathElement = targetModifiedPropertyPath.substring(0, targetModifiedPropertyPath.indexOf("/"));
            if (targetModifiedNode != null) {
                if (targetModifiedNode.hasNode(pathElement)) {
                    targetModifiedNode = targetModifiedNode.getNode(pathElement);
                } else if (parameters.containsKey(getName())) {
                    targetModifiedNode = targetModifiedNode.addNode(pathElement);
                } else {
                    targetModifiedNode = null;
                }
            }
            targetModifiedPropertyPath = targetModifiedPropertyPath.substring(targetModifiedPropertyPath.indexOf("/") + 1);
            if (!targetModifiedPropertyPath.contains("/")) {
                if (targetModifiedNode != null) {
                    targetModifiedNodetype = targetModifiedNode.getPrimaryNodeType();
                } else {
                    targetModifiedNodetype = null;
                }
            }
        }
        PropertyUpdateLogger updateLogger = new PropertyUpdateLogger(propertyPath, getName(), modified, DerivedDataEngine.log);
        Value[] values = parameters.get(getName());
        if (targetModifiedNode != null && targetModifiedNode.hasProperty(targetModifiedPropertyPath)) {
            Property property = targetModifiedNode.getProperty(targetModifiedPropertyPath);
            updateProperty(values, updateLogger, property, modified);
        } else {
            addProperty(values, updateLogger, targetModifiedNode, targetModifiedPropertyPath, targetModifiedNodetype, modified);
        }
        updateLogger.flush();
        return updateLogger.isChanged();
    }

    private NodeType getApplicableNodeType() throws RepositoryException {
        return node.getSession().getWorkspace().getNodeTypeManager().getNodeType(function.getApplicableNodeType());
    }

    private String getRelativePath() throws RepositoryException {
        return node.getProperty(HIPPOSYS_REL_PATH).getString();
    }


    private void updateProperty(final Value[] values, final PropertyUpdateLogger pul, Property property, Node modified) throws RepositoryException {
        if (!property.getDefinition().isMultiple()) {
            if (values != null && values.length > 0) {
                if (!property.getValue().equals(values[0])) {
                    try {
                        property.getSession().checkPermission(property.getParent().getPath(), Privilege.JCR_MODIFY_PROPERTIES);
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
                    property.getSession().checkPermission(property.getParent().getPath(), Privilege.JCR_MODIFY_PROPERTIES);
                    property.remove();
                    pul.removed();
                } catch (AccessControlException ex) {
                    DerivedDataEngine.log.warn("cannot update " + modified.getPath());
                    pul.failed();
                }
            }
        } else {
            //Backwards compatibility: if a deriveddata function returns null instead of an empty Value[], 
            //we *do not* remove the existing property (we do that for single value properties, see code above).
            //Up to now the code was throwing an NPE if values == null, and thus was not removing the property
            Value[] checkedValues = (values == null) ? new Value[0] : values;
            
            boolean changed = false;
            if (checkedValues.length == property.getValues().length) {
                Value[] oldValues = property.getValues();
                for (int i = 0; i < checkedValues.length; i++) {
                    if (!checkedValues[i].equals(oldValues[i])) {
                        changed = true;
                        break;
                    }
                }
            } else {
                changed = true;
            }
            if (changed) {
                try {
                    property.getSession().checkPermission(property.getParent().getPath(), Privilege.JCR_MODIFY_PROPERTIES);
                    property.setValue(checkedValues);
                    pul.overwritten(checkedValues);
                } catch (AccessControlException ex) {
                    DerivedDataEngine.log.warn("cannot update " + modified.getPath());
                    pul.failed();
                }
            } else {
                pul.unchanged(checkedValues);
            }
        }
    }

    private void addProperty(Value[] values, final PropertyUpdateLogger pul, final Node targetModifiedNode, final String targetModifiedPropertyPath, final NodeType targetModifiedNodetype, Node modified) throws RepositoryException {
        PropertyDefinition derivedPropDef = null;
        if (targetModifiedNodetype != null) {
            derivedPropDef = getPropertyDefinition(targetModifiedNodetype, targetModifiedPropertyPath);
        }

        boolean useSingleProperty = !isMultiValue() && (derivedPropDef == null || !derivedPropDef.isMultiple());

        if (useSingleProperty) {
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

    public boolean isMultiValue() {
        return multiValue;
    }
}
