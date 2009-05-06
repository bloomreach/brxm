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
package org.hippoecm.repository.decorating.spi;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.decorating.DecoratorFactory;

public class NodeDecorator extends org.hippoecm.repository.decorating.NodeDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    HippoSession remoteSession;

    protected NodeDecorator(DecoratorFactory factory, Session session, Node node) {
        super(factory, session, node);
        remoteSession = ((SessionDecorator)session).getRemoteSession();
    }

    public Node getCanonicalNode() throws RepositoryException {
        return getCanonicalNode(session, remoteSession, this);
    }

    public String getDisplayName() throws RepositoryException {
        return getDisplayName(session, remoteSession, this);
    }

    static Node getCanonicalNode(Session session, HippoSession remoteSession, Node node) throws RepositoryException {
        if (node.isNew()) {
            return node;
        }
        String path = node.getPath();
        if (path == null) {
            return null;
        }
        if ("/".equals(path)) {
            return session.getRootNode();
        }
        node = remoteSession.getRootNode().getNode(node.getPath().substring(1));
        if (node == null) {
            return null;
        }
        node = ((HippoNode) node).getCanonicalNode();
        if (node == null) {
            return null;
        }
        return session.getRootNode().getNode(node.getPath().substring(1));
    }

    static String getDisplayName(Session session, HippoSession remoteSession, Node node) throws RepositoryException {
        node = remoteSession.getRootNode().getNode(node.getPath().substring(1));
        return ((HippoNode)node).getDisplayName();
    }
}
