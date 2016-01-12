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

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.jaxrs.contentrestapi.ContentRestApiResource;

class HtmlNodeVisitor extends Visitor {

    public HtmlNodeVisitor(VisitorFactory factory) {
        super(factory);
    }

    public void visit(final Item sourceItem, final Map<String, Object> destination) throws RepositoryException {
        final Node sourceNode = (Node) sourceItem;
        final String sourceNodeName = sourceNode.getName();
        final Map<String, Object> htmlNodeOutput = new TreeMap<>();
        destination.put(sourceNodeName, htmlNodeOutput);

        final PropertyIterator propertyIterator = sourceNode.getProperties();
        while (propertyIterator.hasNext()) {
            Property childProperty = (Property) propertyIterator.next();
            if (childProperty.getName().equals("hippostd:content")) {
                // TODO link rewriting - the href of the binary links needs to be altered
            }
            Visitor visitor = getFactory().getVisitor(childProperty);
            visitor.visit(childProperty, htmlNodeOutput);
        }

        final Map<String, Object> linksOutput = new TreeMap<>();
        htmlNodeOutput.put(ContentRestApiResource.NAMESPACE_PREFIX + ":links", linksOutput);

        final NodeIterator nodeIterator = sourceNode.getNodes();
        while (nodeIterator.hasNext()) {
            Node childNode = (Node) nodeIterator.next();
            Visitor visitor = getFactory().getVisitor(childNode);
            switch (childNode.getPrimaryNodeType().getName()) {
                case "hippo:facetselect":
                    visitor.visit(childNode, linksOutput);
                    break;
                default:
                    visitor.visit(childNode, htmlNodeOutput);
                    break;
            }
        }
    }

}