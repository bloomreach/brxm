/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standards.list.comparators;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;

public class NodeNameComparator extends NodeComparator {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(final JcrNodeModel node1, final JcrNodeModel node2) {
        try {
            return String.CASE_INSENSITIVE_ORDER.compare(node1.getNode().getName(), node2.getNode().getName());
        } catch (RepositoryException ignore) {
        }

        return 0;
    }
}
