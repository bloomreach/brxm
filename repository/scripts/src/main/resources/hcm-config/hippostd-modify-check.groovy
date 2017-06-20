/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.updaters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.update.BaseNodeUpdateVisitor;

/**
 * This script checks the hippostd:modify properties on hippostd:templatequery nodes
 * for possible misconfigurations. This script will not make any changes but emit a warning
 * whenever it encounters a hippostd:modify value that might be invalid. This means
 * that it cannot locate the node or property pointed to in the hippostd:modify property value
 * in the configured template. Manually check these values to see if there is a problem.
 */
class CheckStdModifyProperties extends BaseNodeUpdateVisitor {

    @Override
    public boolean doUpdate(final Node node) throws RepositoryException {
        for (Node template : getTemplates(node)) {
            final PathVisitor visitor = new PathVisitor();
            template.accept(visitor);
            for (String relPath : getPathsToCheck(node)) {
                if (!visitor.hasPath(relPath)) {
                    log.warn("Possible invalid hippostd:modify value at {}: not found {}", node.getPath(), relPath);
                }
            }
        }
        return false;
    }

    Iterable<Node> getTemplates(Node queryNode) throws RepositoryException {
        final QueryManager queryManager = queryNode.getSession().getWorkspace().getQueryManager();
        final Query query = queryManager.getQuery(queryNode);
        return new NodeIterable(query.execute().getNodes());
    }

    Iterable<String> getPathsToCheck(Node queryNode) throws RepositoryException {
        final Property modify = JcrUtils.getPropertyIfExists(queryNode, "hippostd:modify");
        if (modify == null) {
            return Collections.emptyList();
        }
        Collection<String> result = new ArrayList<>();
        for (Value value : modify.getValues()) {
            final String key = value.getString();
            if (key.startsWith("./") && key.lastIndexOf('/') != 1) {
                result.add(key);
            }
        }
        return result;
    }

    boolean undoUpdate(Node node) {
        throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
    }

    static class PathVisitor implements ItemVisitor {

        private final Stack<String> current = new Stack<>();
        private Collection<String> paths = new ArrayList<>();
        @Override
        public void visit(final Property property) throws RepositoryException {
        }

        @Override
        public void visit(final Node node) throws RepositoryException {
            if (current.isEmpty()) {
                current.push(".");
            } else {
                current.push(node.getName());
            }
            addPath();
            for (Node child : new NodeIterable(node.getNodes())) {
                visit(child);
            }
            current.pop();
        }

        private void addPath() {
            StringBuilder sb = new StringBuilder();
            Iterator iter = current.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next());
                if (iter.hasNext()) {
                    sb.append("/");
                }
            }
            paths.add(sb.toString());
        }

        boolean hasPath(String path) {
            String[] elements = path.split("/");
            Collection<String> candidates = new ArrayList<>(paths);
            for (int i = 0; i < elements.length - 1; i++) {
                final Iterator<String> iterator = candidates.iterator();
                while (iterator.hasNext()) {
                    if (!matchElement(elements[i], i, iterator.next())) {
                        iterator.remove();
                    }
                }
            }
            return candidates.size() > 0;
        }

        private boolean matchElement(final String element, final int i, final String candidate) {
            final String[] elements = candidate.split("/");
            return elements.length > i && (element.equals("_node") || element.equals(elements[i]));
        }
    }


}