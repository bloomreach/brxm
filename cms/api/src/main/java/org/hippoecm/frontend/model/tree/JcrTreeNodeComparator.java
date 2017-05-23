/*
 * Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.model.tree;

import java.util.Comparator;

import javax.jcr.Node;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeNameComparator;

/**
 * Comparator implementation based on physical JCR node names.
 */
public class JcrTreeNodeComparator implements Comparator<IJcrTreeNode>, IClusterable {

    private Comparator<Node> nodeComparator;

    public JcrTreeNodeComparator() {
        this.nodeComparator = new NodeNameComparator();
    }

    @Override
    public int compare(final IJcrTreeNode o1, final IJcrTreeNode o2) {
        return nodeComparator.compare(o1.getNodeModel().getObject(), o2.getNodeModel().getObject());
    }
}
