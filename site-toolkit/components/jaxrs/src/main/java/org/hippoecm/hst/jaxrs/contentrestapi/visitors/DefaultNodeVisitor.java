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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.jaxrs.contentrestapi.ResourceContext;

public class DefaultNodeVisitor extends AbstractBaseVisitor {

    public DefaultNodeVisitor(VisitorFactory visitorFactory) {
        super(visitorFactory);
    }

    @Override
    public void visit(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        final Map<String, Object> descendantsOutput = new TreeMap<>();
        destination.put(node.getName(), descendantsOutput);
        // Iterate over all properties and child nodes and add those to the response.
        // In case of a property and a child node with the same name, this overwrites the property.
        // In Hippo 10.x and up, it is not possible to create document types through the document type editor that
        // have this type of same-name-siblings. It is possible when creating document types in the console or when
        // upgrading older projects. For now, it is acceptable that in those exceptional situations there is
        // data-loss. Note that Destination#put will log an info message when an overwrite occurs.
        visit(context, node.getProperties(), descendantsOutput);
        visit(context, node.getNodes(), descendantsOutput);
    }
}