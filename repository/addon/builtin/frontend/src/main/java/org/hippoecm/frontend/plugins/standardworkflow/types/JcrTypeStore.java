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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

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
    private transient Map<String, ITypeDescriptor> types = null;

    public JcrTypeStore(String version) {
        this.version = version;
    }

    public ITypeDescriptor getTypeDescriptor(String name) {
        if (types == null) {
            types = new HashMap<String, ITypeDescriptor>();
        }
        ITypeDescriptor result = types.get(name);
        if (result == null) {
            try {
                Node typeNode = lookupConfigNode(name);
                if (typeNode != null) {
                    result = createTypeDescriptor(typeNode, name);
                    types.put(name, result);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return result;
    }

    public void detach() {
        types = null;
    }

    public List<ITypeDescriptor> getTypes(String namespace) {
        Session session = getJcrSession();

        Map<String, ITypeDescriptor> currentTypes = new HashMap<String, ITypeDescriptor>();
        Map<String, ITypeDescriptor> versionedTypes = new HashMap<String, ITypeDescriptor>();
        try {
            String xpath = HippoNodeType.NAMESPACES_PATH + "/" + namespace + "/*/" + HippoNodeType.HIPPO_NODETYPE + "/"
                    + HippoNodeType.HIPPO_NODETYPE;
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(xpath, Query.XPATH);
            QueryResult result = query.execute();
            NodeIterator iter = result.getNodes();
            while (iter.hasNext()) {
                Node pluginNode = iter.nextNode();
                ITypeDescriptor typeDescriptor = createTypeDescriptor(pluginNode, pluginNode.getParent().getParent()
                        .getName());
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

        ArrayList<ITypeDescriptor> list = new ArrayList<ITypeDescriptor>(currentTypes.values().size());
        list.addAll(versionedTypes.values());
        for (Map.Entry<String, ITypeDescriptor> entry : currentTypes.entrySet()) {
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

        String path = "/" + HippoNodeType.NAMESPACES_PATH + "/" + prefix + "/" + type + "/"
                + HippoNodeType.HIPPO_NODETYPE;
        if (!session.itemExists(path) || !session.getItem(path).isNode()) {
            return null;
        }
        NodeIterator iter = ((Node) session.getItem(path)).getNodes(HippoNodeType.HIPPO_NODETYPE);

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

    public ITypeDescriptor createTypeDescriptor(Node typeNode, String type) throws RepositoryException {
        try {
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
                return new JcrTypeDescriptor(new JcrNodeModel(typeNode), templateTypeNode.getName(), typeName);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
