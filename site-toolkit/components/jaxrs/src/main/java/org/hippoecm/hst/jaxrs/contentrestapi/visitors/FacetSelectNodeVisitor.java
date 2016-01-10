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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.jaxrs.contentrestapi.ContentRestApiResource;

class FacetSelectNodeVisitor extends Visitor {

    public FacetSelectNodeVisitor(VisitorFactory factory) {
        super(factory);
    }

    public void visit(final Item sourceItem, final Map<String, Object> destination) throws RepositoryException {
        final Node sourceNode = (Node) sourceItem;
        final String sourceNodeName = sourceNode.getName();
        final Map<String, Object> output = new TreeMap<>();
        destination.put(sourceNodeName, output);

        try {
            Property docbase = sourceNode.getProperty("hippo:docbase");
            output.put(ContentRestApiResource.NAMESPACE_PREFIX + ":url", "http://localhost:8080/site/api/documents/" + docbase.getValue().getString());
        } catch (RepositoryException e) {
            // log warning
        }
        DefaultNodeVisitor.visitAllSiblings(getFactory(), sourceNode, output);
    }

}