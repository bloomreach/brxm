/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;

/**
 * Comparator implementation based on display names of folder or document nodes.
 */
public class JcrTreeNameComparator implements Comparator<IJcrTreeNode>, IClusterable {

    private Comparator<Node> nameComparator;

    public JcrTreeNameComparator() {
        this.nameComparator = NameComparator.getInstance();
    }

    @Override
    public int compare(final IJcrTreeNode o1, final IJcrTreeNode o2) {
        return nameComparator.compare(o1.getNodeModel().getObject(), o2.getNodeModel().getObject());
    }
}
