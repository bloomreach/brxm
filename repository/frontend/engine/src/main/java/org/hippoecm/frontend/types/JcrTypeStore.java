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
package org.hippoecm.frontend.types;

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
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeStore implements ITypeStore {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeStore.class);

    public static final String DRAFT = "draft";

    private String draftNs;
    private transient Map<String, JcrTypeDescriptor> types = null;

    public JcrTypeStore() {
        this(null);
    }

    public JcrTypeStore(String prefix) {
        this.draftNs = prefix;
    }

    public JcrTypeDescriptor getTypeDescriptor(String name) {
        if ("rep:root".equals(name)) {
            // ignore the root node
            return null;
        }
        if (types == null) {
            types = new HashMap<String, JcrTypeDescriptor>();
        }
        JcrTypeDescriptor result = types.get(name);
        if (result == null) {
            try {
                Node typeNode = lookupConfigNode(name);
                if (typeNode != null) {
                    result = createTypeDescriptor(typeNode, name);
                    types.put(name, result);
                } else {
                    log.warn("No nodetype description found for " + name);
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
            boolean getDraft = namespace.equals(draftNs);
            String uri = getUri(namespace);

            String xpath = HippoNodeType.NAMESPACES_PATH + "/" + namespace + "/*/" + HippoNodeType.HIPPO_NODETYPE + "/"
                    + HippoNodeType.HIPPO_NODETYPE;
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(xpath, Query.XPATH);
            QueryResult result = query.execute();
            NodeIterator iter = result.getNodes();
            while (iter.hasNext()) {
                Node pluginNode = iter.nextNode();
                Node typeNode = pluginNode.getParent().getParent();
                String typeName = typeNode.getParent().getName() + ":" + typeNode.getName();
                ITypeDescriptor typeDescriptor = createTypeDescriptor(pluginNode, typeName);
                if (pluginNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                    if (pluginNode.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        currentTypes.put(typeName, typeDescriptor);
                    }
                } else {
                    if (getDraft) {
                        versionedTypes.put(typeName, typeDescriptor);
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
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

    private String getUri(String prefix) {
        if ("system".equals(prefix)) {
            return "internal";
        }
        try {
            NamespaceRegistry nsReg = getJcrSession().getWorkspace().getNamespaceRegistry();
            return nsReg.getURI(prefix);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }

    private Node lookupConfigNode(String type) throws RepositoryException {
        HippoSession session = (HippoSession) getJcrSession();

        String prefix = "system";
        String subType = type;
        if (type.indexOf(':') > 0) {
            prefix = type.substring(0, type.indexOf(':'));
            subType = NodeNameCodec.encode(type.substring(type.indexOf(':') + 1));
        }

        String uri = getUri(prefix);
        String nsVersion = "_" + uri.substring(uri.lastIndexOf("/") + 1);
        if (prefix.length() > nsVersion.length()
                && nsVersion.equals(prefix.substring(prefix.length() - nsVersion.length()))) {
            prefix = prefix.substring(0, prefix.length() - nsVersion.length());
        }

        String path = "/" + HippoNodeType.NAMESPACES_PATH + "/" + prefix + "/" + subType + "/"
                + HippoNodeType.HIPPO_NODETYPE;
        if (!session.itemExists(path) || !session.getItem(path).isNode()) {
            return null;
        }
        NodeIterator iter = ((Node) session.getItem(path)).getNodes(HippoNodeType.HIPPO_NODETYPE);

        Node current = null;
        while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (prefix.equals(draftNs)) {
                if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    return node;
                } else {
                    if (node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        current = node;
                    }
                }
            } else {
                if (node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    if (node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        return node;
                    }
                }
            }
        }
        return current;
    }

    public JcrTypeDescriptor createTypeDescriptor(Node typeNode, String type) throws RepositoryException {
        try {
            if (typeNode.isNodeType(HippoNodeType.NT_NODETYPE)) {
                Node templateTypeNode = typeNode;
                while (!templateTypeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                    templateTypeNode = templateTypeNode.getParent();
                }

                String prefix = templateTypeNode.getParent().getName();
                String pseudoName;
                if ("system".equals(prefix)) {
                    pseudoName = templateTypeNode.getName();
                } else {
                    pseudoName = prefix + ":" + templateTypeNode.getName();
                }

                String typeName;
                if (typeNode.hasProperty(HippoNodeType.HIPPO_TYPE)) {
                    typeName = typeNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
                } else {
                    typeName = pseudoName;
                }
                return new JcrTypeDescriptor(new JcrNodeModel(typeNode), pseudoName, typeName);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
