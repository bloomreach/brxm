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

package org.hippoecm.hst.jaxrs.contentrestapi.visitors;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

public class DefaultVisitorFactory implements VisitorFactory {

    public Visitor getVisitor(final Item item) throws RepositoryException {
        if (item instanceof Node) {
            final Node node = (Node) item;
            final NodeType nodeType = node.getPrimaryNodeType();

            switch (nodeType.getName()) {
                case "hippo:facetselect":
                    return new FacetSelectNodeVisitor(this);
                case "hippo:handle":
                    return new HandleNodeVisitor(this);
                case "hippostd:html":
                    return new HtmlNodeVisitor(this);
                default:
                    return new DefaultNodeVisitor(this);
            }
        }

        if (item instanceof Property) {
            final Property property = (Property) item;

            switch (property.getName()) {
                case "jcr:uuid":
                    return new NoopVisitor(this);
                default:
                    return new DefaultPropertyVisitor(this);
            }
        }

        return new NoopVisitor(this);
    }

}