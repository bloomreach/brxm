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

class DefaultNodeVisitor extends Visitor {

    public DefaultNodeVisitor(VisitorFactory factory) {
        super(factory);
    }

    public void visit(final Item sourceItem, final Map<String, Object> destination) throws RepositoryException {
        final Node sourceNode = (Node) sourceItem;
        final String sourceNodeName = sourceNode.getName();
        final Map<String, Object> descendants = new TreeMap<>();
        destination.put(sourceNodeName, descendants);

        visitAllSiblings(getFactory(), sourceNode, descendants);
    }

    static void visitAllSiblings(final VisitorFactory factory, final Node source, final Map<String, Object> destination) throws RepositoryException {
        // Iterate over all nodes and add those to the response.
        // In case of a property and a sub node with the same name, this overwrites the property.
        // In Hippo 10.x and up, it is not possible to create document types through the document type editor that
        // have this type of same-name-siblings. It is possible when creating document types in the console or when
        // upgrading older projects. For now, it is acceptable that in those exceptional situations there is
        // data-loss. Note that Destination#put will log an info message when an overwrite occurs.

        final PropertyIterator propertyIterator = source.getProperties();
        while (propertyIterator.hasNext()) {
            Property property = (Property) propertyIterator.next();
            Visitor visitor = factory.getVisitor(property);
            visitor.visit(property, destination);
        }

        final NodeIterator nodeIterator = source.getNodes();
        while (nodeIterator.hasNext()) {
            Node childNode = (Node) nodeIterator.next();
            Visitor visitor = factory.getVisitor(childNode);
            visitor.visit(childNode, destination);
        }
    }
}