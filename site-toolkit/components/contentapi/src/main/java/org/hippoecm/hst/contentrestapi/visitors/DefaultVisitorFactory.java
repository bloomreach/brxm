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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.hst.contentrestapi.ResourceContext;

import static org.hippoecm.repository.HippoStdNodeType.NT_HTML;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETSELECT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_MIRROR;

public class DefaultVisitorFactory implements VisitorFactory {

    public Visitor getVisitor(final ResourceContext context, final Property property) throws RepositoryException {
        return new DefaultPropertyVisitor(this);
    }

    public Visitor getVisitor(final ResourceContext context, final Node node) throws RepositoryException {

        final NodeType nodeType = node.getPrimaryNodeType();
        switch (nodeType.getName()) {
            case NT_FACETSELECT:
                return new FacetSelectNodeVisitor(this);
            case NT_MIRROR:
                return new MirrorNodeVisitor(this);
            case NT_HANDLE:
                return new HandleNodeVisitor(this);
            case NT_HTML:
                return new HtmlNodeVisitor(this);
            default:
                // TODO this needs to be done differently. The fallback if not an explicit jcr primary type matches, a
                // TODO #isNodeType check should be done. Falling back to 'DefaultNodeVisitor' is the last resort
                return new DefaultNodeVisitor(this);
        }
    }
}