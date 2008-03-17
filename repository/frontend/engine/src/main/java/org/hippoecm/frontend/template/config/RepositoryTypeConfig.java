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
package org.hippoecm.frontend.template.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTypeConfig implements TypeConfig {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RepositoryTypeConfig.class);

    public RepositoryTypeConfig() {
    }

    public TypeDescriptor getTypeDescriptor(String name) {
        try {
            Node typeNode = lookupConfigNode(name);
            if (typeNode != null) {
                return createDescriptor(typeNode).type;
            } else {
                log.error("No plugin node found for " + name);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public Node getTypeNode(String name) {
        try {
            return lookupConfigNode(name);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public List<TypeDescriptor> getTypes() {
        return getTypes("*");
    }

    public List<TypeDescriptor> getTypes(String namespace) {
        Session session = getJcrSession();

        List<TypeDescriptor> list = new LinkedList<TypeDescriptor>();
        try {
            String xpath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.NAMESPACES_PATH + "/" + namespace
                    + "/*";

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(xpath, Query.XPATH);
            QueryResult result = query.execute();
            NodeIterator iter = result.getNodes();
            while (iter.hasNext()) {
                Node pluginNode = iter.nextNode();
                Descriptor descriptor = createDescriptor(pluginNode.getNode(pluginNode.getName()));
                TypeDescriptor template = descriptor.type;
                list.add(template);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return list;
    }

    // Privates

    private Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }

    private Node lookupConfigNode(String type) throws RepositoryException {
        Session session = getJcrSession();

        String xpath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.NAMESPACES_PATH + "/*/" + type;

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        if (iter.getSize() > 1) {
            throw new IllegalStateException("Multiple type descriptions found for type " + type);
        } else if (iter.getSize() == 0) {
            return null;
        } else {
            return iter.nextNode().getNode(type);
        }
    }

    protected Descriptor createDescriptor(Node pluginNode) throws RepositoryException {
        return new Descriptor(pluginNode);
    }

    protected class Descriptor implements IClusterable {
        private static final long serialVersionUID = 1L;

        private String jcrPath;

        RepositoryFieldDescriptor field;
        TypeDescriptor type;

        Descriptor(Node typeNode) {
            try {
                this.jcrPath = typeNode.getPath();

                if (typeNode.isNodeType(HippoNodeType.NT_NODETYPE)) {
                    String typeName = typeNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
                    type = new RepositoryTypeDescriptor(typeNode, typeNode.getName(), typeName, this);
                } else if (typeNode.isNodeType(HippoNodeType.NT_FIELD)) {
                    String path = null;
                    if (typeNode.hasProperty(HippoNodeType.HIPPO_PATH)) {
                        path = typeNode.getProperty(HippoNodeType.HIPPO_PATH).getString();
                    }

                    String name = "";
                    if (typeNode.hasProperty(HippoNodeType.HIPPO_NAME)) {
                        name = typeNode.getProperty(HippoNodeType.HIPPO_NAME).getString();
                    }

                    field = new RepositoryFieldDescriptor(typeNode, name, path, this);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        List<Descriptor> getNodeChildren() {
            List<Descriptor> result = new ArrayList<Descriptor>();
            try {
                Node node = getNode();
                if (node != null) {
                    NodeIterator it = node.getNodes();
                    while (it.hasNext()) {
                        Node child = it.nextNode();
                        if (child != null) {
                            result.add(createDescriptor(child));
                        }
                    }
                } else {
                    log.error("No plugin node found under " + jcrPath);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            return result;
        }

        List<TypeDescriptor> getTypes() {
            List<TypeDescriptor> list = new LinkedList<TypeDescriptor>();
            Iterator<Descriptor> iterator = getNodeChildren().iterator();
            while (iterator.hasNext()) {
                Descriptor plugin = iterator.next();
                if (plugin.type != null) {
                    list.add(plugin.type);
                }
            }
            return list;
        }

        Map<String, FieldDescriptor> getFields() {
            Map<String, FieldDescriptor> map = new HashMap<String, FieldDescriptor>();
            Iterator<Descriptor> iterator = getNodeChildren().iterator();
            while (iterator.hasNext()) {
                Descriptor plugin = iterator.next();
                if (plugin.field != null) {
                    map.put(plugin.field.name, plugin.field);
                }
            }
            return map;
        }

        protected Node getNode() throws RepositoryException {
            return (Node) getJcrSession().getItem(jcrPath);
        }
    }

    protected class RepositoryTypeDescriptor extends TypeDescriptor {
        private static final long serialVersionUID = 1L;

        Descriptor nodeDescriptor;

        public RepositoryTypeDescriptor(Node node, String name, String type, Descriptor nodeDescriptor) {
            super(name, type, null);

            this.nodeDescriptor = nodeDescriptor;

            try {
                if (node.hasProperty(HippoNodeType.HIPPO_NODE)) {
                    setIsNode(node.getProperty(HippoNodeType.HIPPO_NODE).getBoolean());
                }
                if (node.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                    setSuperType(node.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getString());
                }
                if (node.hasProperty(HippoNodeType.HIPPO_MIXINTYPES)) {
                    List<String> mixins = new LinkedList<String>();
                    Value[] values = node.getProperty(HippoNodeType.HIPPO_MIXINTYPES).getValues();
                    for (int i = 0; i < values.length; i++) {
                        mixins.add(values[i].getString());
                    }
                    setMixinTypes(mixins);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        @Override
        public Map<String, FieldDescriptor> getFields() {
            Map<String, FieldDescriptor> fields = nodeDescriptor.getFields();
            Set<String> explicit = new HashSet<String>();
            for (FieldDescriptor field : fields.values()) {
                if (!field.getPath().equals("*")) {
                    explicit.add(field.getPath());
                }
            }
            for (FieldDescriptor field : fields.values()) {
                if (field.getPath().equals("*")) {
                    field.setExcluded(explicit);
                }
            }

            return fields;
        }
    }

    protected class RepositoryFieldDescriptor extends FieldDescriptor {
        private static final long serialVersionUID = 1L;

        String name;
        Descriptor nodeDescriptor;

        public RepositoryFieldDescriptor(Node node, String name, String path, Descriptor nodeDescriptor) {
            super(path);

            this.name = name;
            this.nodeDescriptor = nodeDescriptor;

            try {
                if (node.hasProperty(HippoNodeType.HIPPO_MULTIPLE)) {
                    boolean multiple = node.getProperty(HippoNodeType.HIPPO_MULTIPLE).getBoolean();
                    setIsMultiple(multiple);
                }

                if (node.hasProperty(HippoNodeType.HIPPO_MANDATORY)) {
                    boolean mandatory = node.getProperty(HippoNodeType.HIPPO_MANDATORY).getBoolean();
                    setMandatory(mandatory);
                }

                if (node.hasProperty(HippoNodeType.HIPPO_ORDERED)) {
                    setIsOrdered(node.getProperty(HippoNodeType.HIPPO_ORDERED).getBoolean());
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        @Override
        public String getType() {
            try {
                Node pluginNode = nodeDescriptor.getNode();
                if (pluginNode.hasProperty(HippoNodeType.HIPPO_TYPE)) {
                    return pluginNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return null;
        }
    }
}
