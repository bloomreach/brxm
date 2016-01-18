/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.contentrestapi.visitors;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.contentrestapi.ContentRestApiResource.NAMESPACE_PREFIX;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_CONTENT;
import static org.hippoecm.repository.HippoStdNodeType.NT_HTML;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETSELECT;
import static org.hippoecm.repository.api.HippoNodeType.NT_MIRROR;

class HtmlNodeVisitor extends AbstractNodeVisitor {

    private static final Logger log = LoggerFactory.getLogger(HtmlNodeVisitor.class);

    public HtmlNodeVisitor(VisitorFactory visitorFactory) {
        super(visitorFactory);
    }

    @Override
    public String getNodeType() {
        return NT_HTML;
    }

    @Override
    protected void visitChildren(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        for (Property property : new PropertyIterable(node.getProperties())) {
            if (property.getName().equals(HIPPOSTD_CONTENT)) {
                destination.put(property.getName(), property.getValue().getString());
                // TODO link rewriting - the href of the binary links needs to be altered
            }
        }

        final Map<String, Object> linksOutput = new LinkedHashMap<>();
        destination.put(NAMESPACE_PREFIX + ":links", linksOutput);

        for (Node child : new NodeIterable(node.getNodes())) {
            if (!(child.isNodeType(NT_MIRROR) || child.isNodeType(NT_FACETSELECT))) {
                log.warn("Unexpected node type '{}' below node of type '{}'. Expected '{}' or '{}'",
                        child.getPrimaryNodeType().getName(), NT_HTML, NT_MIRROR, NT_FACETSELECT);
                continue;
            }
            final NodeVisitor visitor = getVisitorFactory().getVisitor(context, child);
            visitor.visit(context, child, linksOutput);
        }

    }
}