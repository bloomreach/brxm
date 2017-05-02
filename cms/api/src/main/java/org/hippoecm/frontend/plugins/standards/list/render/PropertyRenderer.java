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
package org.hippoecm.frontend.plugins.standards.list.render;

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PropertyRenderer<T extends Serializable> extends AbstractNodeRenderer {

    private static final Logger log = LoggerFactory.getLogger(PropertyRenderer.class);

    private final String prop;
    private final String relPath;

    public PropertyRenderer(final String prop) {
        this(prop, null);
    }

    public PropertyRenderer(final String prop, final String relPath) {
        this.prop = prop;
        this.relPath = relPath;
    }

    @Override
    protected Component getViewer(final String id, Node node) throws RepositoryException {
        try {
            node = getCanonicalNode(node);
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                Node actual = node.getNode(node.getName());
                if (relPath != null && actual.hasNode(relPath)) {
                    actual = actual.getNode(relPath);
                }
                if (actual.hasProperty(prop)) {
                    final Property p = actual.getProperty(prop);
                    return new Label(id, Model.of(getValue(p)));
                }
            }
        } catch (final RepositoryException e) {
            log.error("Failed to retrieve property {} from node {}", prop, JcrUtils.getNodePathQuietly(node), e);
        }
        return new Label(id, "");
    }

    private Node getCanonicalNode(final Node node) throws RepositoryException {
        if (node instanceof HippoNode) {
            return ((HippoNode) node).getCanonicalNode();
        } else {
            return node;
        }
    }

    protected abstract T getValue(Property p) throws RepositoryException;
}
