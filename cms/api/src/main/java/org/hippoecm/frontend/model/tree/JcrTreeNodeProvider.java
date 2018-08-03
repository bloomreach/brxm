/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeNameComparator;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTreeNodeProvider implements ITreeProvider<Node> {

    private static final Logger log = LoggerFactory.getLogger(JcrTreeNodeProvider.class);

    private final JcrNodeModel root;

    public JcrTreeNodeProvider(final JcrNodeModel nodeModel) {
        this.root = nodeModel;
    }

    @Override
    public Iterator<? extends Node> getRoots() {
        return Collections.singletonList(root.getNode()).iterator();
    }

    @Override
    public boolean hasChildren(final Node node) {
        try {
            return node.hasNodes();
        } catch (RepositoryException e) {
            log.error("Error reading child nodes", e);
        }
        return false;
    }

    @Override
    public Iterator<? extends Node> getChildren(final Node node) {
        try {
            if (!node.getPrimaryNodeType().hasOrderableChildNodes() && !node.isNodeType(HippoNodeType.NT_FACETRESULT)) {
                return sortChildNodes(node.getNodes());
            } else {
                return node.getNodes();
            }
        } catch (RepositoryException e) {
            log.error("Error reading child nodes", e);
        }
        return null;
    }

    private Iterator<? extends Node> sortChildNodes(final NodeIterator nodeIterator) {
        final List<Node> childNodes = new ArrayList<>();
        while (nodeIterator.hasNext()) {
            final Node child = nodeIterator.nextNode();
            childNodes.add(child);
        }
        childNodes.sort(new NodeNameComparator());
        return childNodes.iterator();
    }

    @Override
    public IModel<Node> model(final Node node) {
        return new JcrNodeModel(node);
    }

    @Override
    public void detach() {
        root.detach();
    }
}
