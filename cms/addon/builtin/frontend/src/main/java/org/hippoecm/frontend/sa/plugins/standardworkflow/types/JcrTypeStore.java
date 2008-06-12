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
package org.hippoecm.frontend.sa.plugins.standardworkflow.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeStore implements ITypeStore {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeStore.class);

    private String version;

    public JcrTypeStore(String version) {
        this.version = version;
    }

    public TypeDescriptor getTypeDescriptor(String name) {
        try {
            Node typeNode = lookupConfigNode(name);
            if (typeNode != null) {
                return createDescriptor(typeNode, name).type;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public JcrTypeModel getTypeModel(String name) {
        try {
            Node node = lookupConfigNode(name);
            if (node != null) {
                return new JcrTypeModel(new JcrNodeModel(node), name);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public List<TypeDescriptor> getTypes(String namespace) {
        Session session = getJcrSession();

        Map<String, TypeDescriptor> currentTypes = new HashMap<String, TypeDescriptor>();
        Map<String, TypeDescriptor> versionedTypes = new HashMap<String, TypeDescriptor>();
        try {
            String xpath = HippoNodeType.NAMESPACES_PATH + "/" + namespace + "/*/" + HippoNodeType.HIPPO_NODETYPE + "/"
                    + HippoNodeType.HIPPO_NODETYPE;
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(xpath, Query.XPATH);
            QueryResult result = query.execute();
            NodeIterator iter = result.getNodes();
            while (iter.hasNext()) {
                Node pluginNode = iter.nextNode();
                Descriptor descriptor = new Descriptor(pluginNode, namespace);
                TypeDescriptor typeDescriptor = descriptor.type;
                if (isVersion(pluginNode, RemodelWorkflow.VERSION_CURRENT)) {
                    currentTypes.put(typeDescriptor.getName(), typeDescriptor);
                }
                if (isVersion(pluginNode, version)) {
                    versionedTypes.put(typeDescriptor.getName(), typeDescriptor);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            ex.printStackTrace();
        }

        ArrayList<TypeDescriptor> list = new ArrayList<TypeDescriptor>(currentTypes.values().size());
        list.addAll(versionedTypes.values());
        for (Map.Entry<String, TypeDescriptor> entry : currentTypes.entrySet()) {
            if (!versionedTypes.containsKey(entry.getKey())) {
                list.add(entry.getValue());
            }
        }
        return list;
    }

    // Privates

    private Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }

    private boolean useOldType() {
        return (RemodelWorkflow.VERSION_OLD.equals(version) || RemodelWorkflow.VERSION_ERROR.equals(version));
    }

    private boolean isVersion(Node pluginNode, String version) throws RepositoryException {
        if (pluginNode.isNodeType(HippoNodeType.NT_REMODEL)) {
            if (pluginNode.getProperty(HippoNodeType.HIPPO_REMODEL).getString().equals(version)) {
                return true;
            }
        } else if (RemodelWorkflow.VERSION_CURRENT.equals(version)) {
            return true;
        }
        return false;
    }

    private Node lookupConfigNode(String type) throws RepositoryException {
        HippoSession session = (HippoSession) getJcrSession();
        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();

        String prefix = "system";
        String uri = "";
        if (type.indexOf(':') > 0) {
            prefix = type.substring(0, type.indexOf(':'));
            uri = nsReg.getURI(prefix);
        }

        String nsVersion = "_" + uri.substring(uri.lastIndexOf("/") + 1);
        if (prefix.length() > nsVersion.length()
                && nsVersion.equals(prefix.substring(prefix.length() - nsVersion.length()))) {
            type = type.substring(prefix.length());
            prefix = prefix.substring(0, prefix.length() - nsVersion.length());
            type = prefix + type;
        } else {
            uri = nsReg.getURI("rep");
        }

        String path = "/"+HippoNodeType.NAMESPACES_PATH + "/" + prefix + "/" + type + "/" + HippoNodeType.HIPPO_NODETYPE;
        if(!session.itemExists(path) || !session.getItem(path).isNode()) {
            return null;
        }
        NodeIterator iter = ((Node)session.getItem(path)).getNodes(HippoNodeType.HIPPO_NODETYPE);
        
        Node current = null;
        while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (node.isNodeType(HippoNodeType.NT_REMODEL)) {
                String state = node.getProperty(HippoNodeType.HIPPO_REMODEL).getString();
                if (version.equals(state)) {
                    if (useOldType()) {
                        if (node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                            return node;
                        }
                    } else {
                        return node;
                    }
                } else if (RemodelWorkflow.VERSION_CURRENT.equals(state)) {
                    current = node;
                }
            } else if (RemodelWorkflow.VERSION_CURRENT.equals(version)) {
                return node;
            } else {
                current = node;
            }
        }

        if (RemodelWorkflow.VERSION_DRAFT.equals(version) || RemodelWorkflow.VERSION_ERROR.equals(version)) {
            return current;
        }
        return null;
    }

    protected Descriptor createDescriptor(Node pluginNode, String type) throws RepositoryException {
        String prefix = "system";
        if (type.indexOf(':') > 0) {
            prefix = type.substring(0, type.indexOf(':'));
        }
        return new Descriptor(pluginNode, prefix);
    }

    public TypeDescriptor createTypeDescriptor(Node node, String type) throws RepositoryException {
        return createDescriptor(node, type).type;
    }

    protected class Descriptor implements IClusterable {
        private static final long serialVersionUID = 1L;

        private String jcrPath;

        RepositoryFieldDescriptor field;
        TypeDescriptor type;
        String prefix;
        Descriptor(Node typeNode, String prefix) {

            System.out.println("......");
            try {
                this.jcrPath = typeNode.getPath();
                this.prefix = prefix;

                if (typeNode.isNodeType(HippoNodeType.NT_NODETYPE)) {
                    Node templateTypeNode = typeNode;
                    while (!templateTypeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                        templateTypeNode = templateTypeNode.getParent();
                    }

                    String typeName;
                    if (typeNode.hasProperty(HippoNodeType.HIPPO_TYPE)) {
                        typeName = typeNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
                    } else {
                        typeName = templateTypeNode.getName();
                    }
                    type = new RepositoryTypeDescriptor(typeNode, templateTypeNode.getName(), typeName, this);
                } else if (typeNode.isNodeType(HippoNodeType.NT_FIELD)) {
                    String path = null;
                    if (typeNode.hasProperty(HippoNodeType.HIPPO_PATH)) {
                        path = typeNode.getProperty(HippoNodeType.HIPPO_PATH).getString();
                        if (RemodelWorkflow.VERSION_DRAFT.equals(version)
                                || RemodelWorkflow.VERSION_ERROR.equals(version)) {
                            // convert path
                            if (path.indexOf(':') > 0) {
                                path = prefix + path.substring(path.indexOf(':'));
                            }
                        }
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
                            result.add(new Descriptor(child, prefix));
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
            super(name, type);

            this.nodeDescriptor = nodeDescriptor;

            try {
                if (node.hasProperty(HippoNodeType.HIPPO_NODE)) {
                    setIsNode(node.getProperty(HippoNodeType.HIPPO_NODE).getBoolean());
                }
                if (node.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                    List<String> superTypes = new LinkedList<String>();
                    Value[] values = node.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
                    for (int i = 0; i < values.length; i++) {
                        superTypes.add(values[i].getString());
                    }
                    setSuperTypes(superTypes);
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
