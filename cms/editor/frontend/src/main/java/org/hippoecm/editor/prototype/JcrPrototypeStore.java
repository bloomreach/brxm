/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor.prototype;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPrototypeStore implements IPrototypeStore<Node>, IDetachable {

    private static final Logger log = LoggerFactory.getLogger(JcrPrototypeStore.class);

    private Map<String, JcrNodeModel> prototypes;

    public JcrNodeModel getPrototype(String name, boolean draft) {
        if ("rep:root".equals(name)) {
            // ignore the root node
            return null;
        }
        if (prototypes == null) {
            prototypes = new HashMap<>();
        }
        JcrNodeModel result = prototypes.get(name);
        if (result == null) {
            try {
                final Node node = lookupConfigNode(name, draft);
                if (node != null) {
                    result = new JcrNodeModel(node);
                    prototypes.put(name, result);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return result;
    }

    public JcrNodeModel createPrototype(String name, boolean draft) {
        JcrNodeModel prototype = getPrototype(name, draft);
        if (prototype != null) {
            throw new IllegalArgumentException("prototype " + name + " exists");
        }

        try {
            final String path = getLocation(name);
            final Session session = getJcrSession();
            Node handle;
            final String parentPath = path.substring(0, path.lastIndexOf('/'));
            final Node parent = session.getRootNode().getNode(parentPath.substring(1));
            if (!session.itemExists(path)) {
                handle = parent.addNode(HippoNodeType.HIPPO_PROTOTYPES, HippoNodeType.NT_PROTOTYPESET);
            } else {
                handle = parent.getNode(HippoNodeType.HIPPO_PROTOTYPES);
            }

            Node node;
            if (draft) {
                node = handle.addNode(HippoNodeType.HIPPO_PROTOTYPE, "nt:unstructured");
            } else {
                node = handle.addNode(HippoNodeType.HIPPO_PROTOTYPE, name);
            }
            parent.save();

            prototype = new JcrNodeModel(node);
        } catch (RepositoryException ex) {
            log.error("Failed to create prototype node", ex);
        }
        return prototype;
    }

    public void detach() {
        prototypes = null;
    }

    // Privates

    private String getUri(String prefix) {
        if ("system".equals(prefix)) {
            return "internal";
        }
        try {
            final NamespaceRegistry nsReg = getJcrSession().getWorkspace().getNamespaceRegistry();
            return nsReg.getURI(prefix);
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    private Session getJcrSession() {
        return UserSession.get().getJcrSession();
    }

    private String getLocation(String type) {
        String prefix = "system";
        String subType = type;

        final int separatorIndex = type.indexOf(':');
        if (separatorIndex > 0) {
            prefix = type.substring(0, separatorIndex);
            subType = type.substring(separatorIndex + 1);
        }

        final String uri = getUri(prefix);
        final String nsVersion = "_" + uri.substring(uri.lastIndexOf('/') + 1);

        final int prefixLength = prefix.length();
        final int nsVersionLength = nsVersion.length();

        if (prefixLength > nsVersionLength && nsVersion.equals(prefix.substring(prefixLength - nsVersionLength))) {
            prefix = prefix.substring(0, prefixLength - nsVersionLength);
        }

        return "/" + HippoNodeType.NAMESPACES_PATH + "/" + prefix + "/" + subType + "/"
                + HippoNodeType.HIPPO_PROTOTYPES;
    }

    private Node lookupConfigNode(String type, boolean draft) throws RepositoryException {
        final HippoSession session = (HippoSession) getJcrSession();
        final String path = getLocation(type);
        if (!session.itemExists(path) || !session.getItem(path).isNode()) {
            return null;
        }
        final NodeIterator iter = ((Node) session.getItem(path)).getNodes(HippoNodeType.HIPPO_PROTOTYPE);

        while (iter.hasNext()) {
            final Node node = iter.nextNode();
            if (draft) {
                if (node.isNodeType(JcrConstants.NT_UNSTRUCTURED)) {
                    return node;
                }
            } else {
                if (!node.isNodeType(JcrConstants.NT_UNSTRUCTURED)) {
                    return node;
                }
            }
        }
        return null;
    }
}
