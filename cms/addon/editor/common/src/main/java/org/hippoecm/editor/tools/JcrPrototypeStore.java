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
package org.hippoecm.editor.tools;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPrototypeStore implements IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrPrototypeStore.class);

    private Map<String, JcrNodeModel> prototypes;

    public JcrNodeModel getPrototype(String name, boolean draft) {
        if ("rep:root".equals(name)) {
            // ignore the root node
            return null;
        }
        if (prototypes == null) {
            prototypes = new HashMap<String, JcrNodeModel>();
        }
        JcrNodeModel result = prototypes.get(name);
        if (result == null) {
            try {
                Node node = lookupConfigNode(name, draft);
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
            String path = getLocation(name);
            Session session = getJcrSession();
            Node handle;
            String parentPath = path.substring(0, path.lastIndexOf('/'));
            Node parent = session.getRootNode().getNode(parentPath.substring(1));
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

    private Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }

    private String getLocation(String type) throws RepositoryException {
        String prefix = "system";
        String subType = type;
        if (type.indexOf(':') > 0) {
            prefix = type.substring(0, type.indexOf(':'));
            subType = type.substring(type.indexOf(':') + 1);
        }

        JcrNamespace namespace = new JcrNamespace(getJcrSession(), prefix);
        return namespace.getPath() + "/" + subType + "/" + HippoNodeType.HIPPO_PROTOTYPES;
    }

    private Node lookupConfigNode(String type, boolean draft) throws RepositoryException {
        HippoSession session = (HippoSession) getJcrSession();
        String path = getLocation(type);
        if (!session.itemExists(path) || !session.getItem(path).isNode()) {
            return null;
        }
        NodeIterator iter = ((Node) session.getItem(path)).getNodes(HippoNodeType.HIPPO_PROTOTYPE);

        while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (draft) {
                if (node.isNodeType("nt:unstructured")) {
                    return node;
                }
            } else {
                if (!node.isNodeType("nt:unstructured")) {
                    return node;
                }
            }
        }
        return null;
    }
}
