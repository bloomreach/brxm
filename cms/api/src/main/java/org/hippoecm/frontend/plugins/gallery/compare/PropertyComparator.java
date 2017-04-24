/*
 * Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.compare;

import java.util.Comparator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public abstract class PropertyComparator implements Comparator<Node>, IClusterable {

    private final String prop;
    private final String relPath;

    public PropertyComparator(final String prop) {
        this(prop, null);
    }

    public PropertyComparator(final String prop, final String relPath) {
        this.prop = prop;
        this.relPath = relPath;
    }

    public int compare(final Node o1, final Node o2) {
        final Property p1 = getProperty(o1);
        final Property p2 = getProperty(o2);
        return compare(p1, p2);
    }

    protected abstract int compare(Property p1, Property p2);

    private Property getProperty(Node node) {
        try {
            node = getCanonicalNode(node);
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                Node actual = node.getNode(node.getName());
                if (relPath != null && actual.hasNode(relPath)) {
                    actual = actual.getNode(relPath);
                }
                if (actual.hasProperty(prop)) {
                    return actual.getProperty(prop);
                }
            }
        } catch (final RepositoryException ignored) {
        }
        return null;
    }

    private Node getCanonicalNode(final Node node) throws RepositoryException {
        if (node instanceof HippoNode) {
            return ((HippoNode) node).getCanonicalNode();
        } else {
            return node;
        }
    }
}
