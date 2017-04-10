/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.richtext.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrNodeFactory implements NodeFactory {

    public static final Logger log = LoggerFactory.getLogger(JcrNodeFactory.class);

    private final JcrSessionProvider sessionProvider;

    public JcrNodeFactory(final JcrSessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @Override
    public Node getNodeByIdentifier(final String uuid) throws RepositoryException {
        return getSession().getNodeByIdentifier(uuid);
    }

    @Override
    public Node getNodeByPath(final String path) throws RepositoryException {
        return getSession().getNode(path);
    }

    @Override
    public Model<Node> getNodeModelByIdentifier(final String uuid) throws RepositoryException {
        return new NodeModel(uuid);
    }

    @Override
    public Model<Node> getNodeModelByNode(final Node node) {
        if (node == null) {
            return null;
        }
        try {
            return new NodeModel(node);
        } catch (final RepositoryException e) {
            if (log.isInfoEnabled()) {
                log.error("Failed to create node model from node {}", JcrUtils.getNodePathQuietly(node), e);
            } else {
                log.error("Failed to create node model from node {}", JcrUtils.getNodePathQuietly(node));
            }
        }
        return null;
    }

    protected final Session getSession() throws RepositoryException {
        return sessionProvider.getSession();
    }

    public static JcrNodeFactory of(final Node node) {
        return new JcrNodeFactory(node::getSession);
    }

    private class NodeModel implements Model<Node> {

        private String uuid;
        private Node node;

        public NodeModel(final Node node) throws RepositoryException {
            this.uuid = node.getIdentifier();
            this.node = node;
        }

        public NodeModel(final String uuid) {
            this.uuid = uuid;
        }

        @Override
        public Node get() {
            try {
                if (node == null) {
                    node = getNodeByIdentifier(uuid);
                }
            } catch (final RepositoryException e) {
                if (log.isInfoEnabled()) {
                    log.error("Failed to load node with uuid {}", uuid, e);
                } else {
                    log.error("Failed to load node with uuid {}", uuid);
                }
            }
            return node;
        }

        @Override
        public void set(final Node value) {
            try {
                uuid = value.getIdentifier();
                node = value;
            } catch (final RepositoryException e) {
                if (log.isInfoEnabled()) {
                    log.error("Failed to retrieve uuid from node {}", JcrUtils.getNodePathQuietly(value), e);
                } else {
                    log.error("Failed to retrieve uuid from node {}", JcrUtils.getNodePathQuietly(value));
                }
            }
        }

        @Override
        public void release() {
            node = null;
        }
    }
}
