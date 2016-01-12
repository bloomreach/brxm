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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

public abstract class Visitor {

    private VisitorFactory factory;

    public Visitor(VisitorFactory factory) {
        this.factory = factory;
    }

    public VisitorFactory getFactory() {
        return factory;
    }

    abstract public void visit(Item source, Map<String, Object> destination) throws RepositoryException;

    // TODO this is not visiting all siblings : It is visiting properties & descendant nodes.
    protected void visitAllSiblings(final VisitorFactory factory, final Node source, final Map<String, Object> destination) throws RepositoryException {
        // Iterate over all nodes and add those to the response.
        // In case of a property and a sub node with the same name, this overwrites the property.
        // In Hippo 10.x and up, it is not possible to create document types through the document type editor that
        // have this type of same-name-siblings. It is possible when creating document types in the console or when
        // upgrading older projects. For now, it is acceptable that in those exceptional situations there is
        // data-loss. Note that Destination#put will log an info message when an overwrite occurs.

        visitAllProperties(factory, source.getProperties(), destination);
        visitAllNodes(factory, source.getNodes(), destination);
    }

    protected void visitAllProperties(final VisitorFactory factory, final PropertyIterator source, final Map<String, Object> destination) throws RepositoryException {
        while (source.hasNext()) {
            Property property = (Property) source.next();
            Visitor visitor = factory.getVisitor(property);
            visitor.visit(property, destination);
        }
    }

    protected void visitAllNodes(final VisitorFactory factory, final NodeIterator source, final Map<String, Object> destination) throws RepositoryException {
        while (source.hasNext()) {
            // TODO replace  (Node) source.next(); with source.nextNode()
            Node childNode = (Node) source.next();
            Visitor visitor = factory.getVisitor(childNode);
            visitor.visit(childNode, destination);
        }
    }

}