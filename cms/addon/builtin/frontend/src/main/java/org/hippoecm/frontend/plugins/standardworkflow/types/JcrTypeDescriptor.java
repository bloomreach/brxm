/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standardworkflow.types;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.jackrabbit.value.NameValue;
import org.apache.jackrabbit.value.PathValue;
import org.apache.jackrabbit.value.ReferenceValue;
import org.apache.jackrabbit.value.StringValue;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeDescriptor extends NodeModelWrapper implements ITypeDescriptor {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeDescriptor.class);

    private String name;
    private String type;
    private transient Map<String, IFieldDescriptor> fields;

    public JcrTypeDescriptor(JcrNodeModel nodeModel, String name, String type) {
        super(nodeModel);

        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<String> getSuperTypes() {
        try {
            Node node = getNodeModel().getNode();
            List<String> superTypes = new LinkedList<String>();
            if (node.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                Value[] values = node.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
                for (int i = 0; i < values.length; i++) {
                    superTypes.add(values[i].getString());
                }
            }
            return superTypes;
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public void setSuperTypes(List<String> superTypes) {
    }

    public Map<String, IFieldDescriptor> getFields() {
        if (fields == null) {
            fields = new HashMap<String, IFieldDescriptor>();
            try {
                Node node = getNodeModel().getNode();
                if (node != null) {
                    NodeIterator it = node.getNodes();
                    while (it.hasNext()) {
                        Node child = it.nextNode();
                        if (child != null && child.isNodeType(HippoNodeType.NT_FIELD)) {
                            IFieldDescriptor field = new JcrFieldDescriptor(new JcrNodeModel(child));
                            fields.put(field.getName(), field);
                        }
                    }
                }
                Set<String> explicit = new HashSet<String>();
                for (IFieldDescriptor field : fields.values()) {
                    if (!field.getPath().equals("*")) {
                        explicit.add(field.getPath());
                    }
                }
                for (IFieldDescriptor field : fields.values()) {
                    if (field.getPath().equals("*")) {
                        field.setExcluded(explicit);
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return fields;
    }

    public IFieldDescriptor getField(String key) {
        return getFields().get(key);
    }

    public boolean isNode() {
        try {
            if (nodeModel.getNode().hasProperty(HippoNodeType.HIPPO_NODE)) {
                return nodeModel.getNode().getProperty(HippoNodeType.HIPPO_NODE).getBoolean();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return true;
    }

    public void setIsNode(boolean isNode) {
        setBoolean(HippoNodeType.HIPPO_NODE, isNode);
    }

    public boolean isMixin() {
        return getBoolean("hippo:mixin");
    }

    public void setIsMixin(boolean isMixin) {
        setBoolean("hippo:mixin", isMixin);
    }

    public Value createValue() {
        try {
            int propertyType = PropertyType.valueFromName(type);
            switch (propertyType) {
            case PropertyType.BOOLEAN:
                return BooleanValue.valueOf("false");
            case PropertyType.DATE:
                return new DateValue(Calendar.getInstance());
            case PropertyType.DOUBLE:
                return DoubleValue.valueOf("0.0");
            case PropertyType.LONG:
                return LongValue.valueOf("0");
            case PropertyType.NAME:
                return NameValue.valueOf("");
            case PropertyType.PATH:
                return PathValue.valueOf("/");
            case PropertyType.REFERENCE:
                return ReferenceValue.valueOf(UUID.randomUUID().toString());
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
                return new StringValue("");
            default:
                return null;
            }
        } catch (ValueFormatException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    public String addField(String fieldType) {
        fields = null;
        try {
            Node typeNode = getNodeModel().getNode();
            Node field = typeNode.addNode(HippoNodeType.HIPPO_FIELD, HippoNodeType.NT_FIELD);
            field.setProperty(HippoNodeType.HIPPO_TYPE, fieldType);
            UUID uuid = java.util.UUID.randomUUID();
            field.setProperty(HippoNodeType.HIPPO_NAME, uuid.toString());
            String path = type.substring(0, type.indexOf(':')) + ":" + fieldType.toLowerCase().replace(':', '_');
            field.setProperty(HippoNodeType.HIPPO_PATH, path);

            return uuid.toString();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public void removeField(String field) {
        fields = null;
        try {
            Node fieldNode = getFieldNode(field);
            if (fieldNode != null) {
                fieldNode.remove();
            } else {
                log.warn("field " + field + " was not found in type " + type);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public void detach() {
        super.detach();
        fields = null;
    }

    private boolean getBoolean(String path) {
        try {
            if (nodeModel.getNode().hasProperty(path)) {
                return nodeModel.getNode().getProperty(path).getBoolean();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return false;
    }

    private void setBoolean(String path, boolean value) {
        try {
            nodeModel.getNode().setProperty(path, value);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private Node getFieldNode(String field) throws RepositoryException {
        Node typeNode = getNodeModel().getNode();
        NodeIterator fieldIter = typeNode.getNodes(HippoNodeType.HIPPO_FIELD);
        while (fieldIter.hasNext()) {
            Node fieldNode = fieldIter.nextNode();
            if (fieldNode.hasProperty(HippoNodeType.HIPPO_NAME)) {
                String name = fieldNode.getProperty(HippoNodeType.HIPPO_NAME).getString();
                if (name.equals(field)) {
                    return fieldNode;
                }
            }
        }
        return null;
    }

}
