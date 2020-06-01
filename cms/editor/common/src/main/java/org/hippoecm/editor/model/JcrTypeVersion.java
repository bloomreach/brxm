/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor.model;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;

/**
 * Version information on a CMS type, as stored in the repository.
 * This class is not intended to be used by plugin developers.
 */
public final class JcrTypeVersion {

    private final Session session;
    private final JcrTypeInfo info;
    private final boolean draft;
    private final String uri;

    public JcrTypeVersion(Session session, String type) throws RepositoryException {
        this.session = session;
        info = new JcrTypeInfo(session, type);
        draft = false;
        if (type.indexOf(':') > 0) {
            String prefix = type.substring(0, type.indexOf(':'));
            uri = session.getWorkspace().getNamespaceRegistry().getURI(prefix);
        } else {
            uri = "internal";
        }
    }

    public JcrTypeVersion(Session session, JcrTypeInfo info, boolean draft, String uri) {
        this.session = session;
        this.info = info;
        this.draft = draft;
        this.uri = uri;
    }

    public JcrTypeInfo getTypeInfo() {
        return info;
    }
    
    public Node getTypeNode() throws RepositoryException {
        String path = info.getPath();
        if (!session.itemExists(path) || !session.getItem(path).isNode()) {
            return null;
        }

        NodeIterator iter = ((Node) session.getItem(path)).getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(
                HippoNodeType.HIPPOSYSEDIT_NODETYPE);

        String prefix = info.getNamespace().getPrefix();
        boolean isHippoNs = "hippo".equals(prefix) || "hipposys".equals(prefix);
        while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                if (draft) {
                    return node;
                } else if (node.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                    String realType = node.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
                    String pseudoType;
                    if (!"system".equals(prefix)) {
                        pseudoType = prefix + ":" + info.getTypeName();
                    } else {
                        pseudoType = info.getTypeName();
                    }
                    if (!realType.equals(pseudoType)) {
                        return node;
                    }
                }
            } else {
                // ignore uris for hippo namespace
                if (isHippoNs || node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                    return node;
                }
            }
        }
        return null;
    }

}