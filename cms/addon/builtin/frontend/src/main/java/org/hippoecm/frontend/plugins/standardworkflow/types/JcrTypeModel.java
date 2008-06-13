/*
 * Copyright 2008 Hippo
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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeModel extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeModel.class);

    private String typeName;

    public JcrTypeModel(JcrNodeModel model, String type) {
        super(model);
        typeName = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public JcrFieldModel getField(String field) {
        try {
            Node fieldNode = getFieldNode(field);
            if (fieldNode != null) {
                return new JcrFieldModel(new JcrNodeModel(fieldNode));
            } else {
                log.warn("field " + field + " was not found in type " + typeName);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public List<JcrFieldModel> getFields() {
        List<JcrFieldModel> list = new LinkedList<JcrFieldModel>();
        try {
            Node typeNode = getNodeModel().getNode();
            NodeIterator iter = typeNode.getNodes(HippoNodeType.HIPPO_FIELD);
            while (iter.hasNext()) {
                Node fieldNode = iter.nextNode();
                list.add(new JcrFieldModel(new JcrNodeModel(fieldNode)));
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return list;
    }

    public String addField(String type) {
        try {
            Node typeNode = getNodeModel().getNode();
            Node field = typeNode.addNode(HippoNodeType.HIPPO_FIELD, HippoNodeType.NT_FIELD);
            field.setProperty(HippoNodeType.HIPPO_TYPE, type);
            UUID uuid = java.util.UUID.randomUUID();
            field.setProperty(HippoNodeType.HIPPO_NAME, uuid.toString());
            String path = typeName.substring(0, typeName.indexOf(':')) + ":" + type.toLowerCase().replace(':', '_');
            field.setProperty(HippoNodeType.HIPPO_PATH, path);

            return uuid.toString();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public void removeField(String field) {
        try {
            Node fieldNode = getFieldNode(field);
            if (fieldNode != null) {
                fieldNode.remove();
            } else {
                log.warn("field " + field + " was not found in type " + typeName);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void save() {
        try {
            getNodeModel().getNode().save();
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
