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
package org.hippoecm.frontend.plugins.standards;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentListFilter implements IClusterable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DocumentListFilter.class);

    private String currentState = "";

    private static class FilterDefinition implements IClusterable {
        private static final long serialVersionUID = 1L;

        String state;
        String path;
        String parent;
        String child;
        String documentType;
        String targetState;
        boolean targetDisplay;
        String targetName;

        FilterDefinition(String state, String path, String parent, String child, String documentType, String targetState, boolean targetDisplay, String targetName) {
            this.state = state;
            this.path = path;
            this.parent = parent;
            this.child = child;
            this.documentType = documentType;
            this.targetState = targetState;
            this.targetDisplay = targetDisplay;
            this.targetName = targetName;
        }

        boolean match(String currentState, Node node) throws RepositoryException {
            if (!currentState.isEmpty() && !state.isEmpty() && !currentState.equals(state)) {
                return false;
            }
            if (!path.isEmpty()) {
                if (path.startsWith("/")) {
                    if (!path.equals(node.getPath())) {
                        return false;
                    }
                } else if (!path.equals(node.getName())) {
                    return false;
                }
            }
            if (!parent.isEmpty() && (node.getDepth() == 0 || !node.getParent().isNodeType(parent))) {
                return false;
            }
            if (!child.isEmpty() && !node.isNodeType(child)) {
                return false;
            }
            return documentType.isEmpty() || isDocumentOfType(node, documentType);
        }

        private static boolean isDocumentOfType(final Node node, final String type) throws RepositoryException {
            if (!node.isNodeType(HippoNodeType.NT_HANDLE)) {
                return false;
            }
            final String nodeName = node.getName();
            return node.hasNode(nodeName) && node.getNode(nodeName).isNodeType(type);
        }
    }

    private List<FilterDefinition> filters;

    public DocumentListFilter(IPluginConfig config) {
        filters = new ArrayList<>();

        IPluginConfig filterConfig = config.getPluginConfig("filters");
        if (filterConfig != null) {
            for (IPluginConfig filter : filterConfig.getPluginConfigSet()) {
                filters.add(new FilterDefinition(filter.getString("state", ""),
                        filter.getString("path", ""),
                        filter.getString("parent", ""),
                        filter.getString("child", ""),
                        filter.getString("documentType", ""),
                        filter.getString("target", ""),
                        filter.getBoolean("display"),
                        filter.getString("name", "")));
            }
        }

        // Checking for log levels enabled or not is not a good practice but in this case we want to avoid the cost
        // of looping over {@link FilterDefinition}(s) in case the debug level is not enabled
        if (log.isDebugEnabled()) {
            log.debug("Filter definitions are:");
            for (FilterDefinition def : filters) {
                log.debug("  ({}, {}, {}, {}, {}, {}, {}, {})", def.state, def.path, def.parent, def.child,
                        def.documentType, def.targetState, def.targetDisplay, def.targetName);
            }
        }
    }

    public DocumentListFilter(DocumentListFilter parent, String state) {
        filters = parent.filters;
        currentState = state;
    }

    public NodeIterator filter(Node current, final NodeIterator iter) {
        return new NodeIterator() {
            private int index = 0;
            private Node nextNode;

            private void fillNextNode() {
                nextNode = null;
                try {
                    while (iter.hasNext() && nextNode == null) {
                        Node candidate = iter.nextNode();
                        for (FilterDefinition filterDefinition : filters) {
                            if (filterDefinition.match(currentState, candidate)) {
                                if (!filterDefinition.targetDisplay) {
                                    candidate = null;
                                }
                                break;
                            }
                        }
                        if (candidate != null) {
                            nextNode = candidate;
                        }
                    }
                } catch (RepositoryException ignored) {
                }
            }

            public Node nextNode() {
                if (nextNode == null) {
                    fillNextNode();
                }
                if (nextNode == null) {
                    throw new NoSuchElementException();
                }
                Node rtValue = nextNode;
                nextNode = null;
                return rtValue;
            }

            public long getPosition() {
                return index;
            }

            public long getSize() {
                return -1;
            }

            public void skip(long count) {
                while (count-- > 0) {
                    nextNode();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public Object next() {
                return nextNode();
            }

            public boolean hasNext() {
                if (nextNode == null) {
                    fillNextNode();
                }
                if (nextNode == null) {
                    return false;
                }
                return true;
            }
        };
    }

    public String getDisplayName(Node node) throws RepositoryException {
        String displayName = node.getName();
        for (FilterDefinition def : filters) {
            if (def.match(currentState, node)) {
                if (!def.targetName.isEmpty()) {
                    displayName = def.targetName;
                }
                break;
            }
        }
        return displayName;
    }
}
