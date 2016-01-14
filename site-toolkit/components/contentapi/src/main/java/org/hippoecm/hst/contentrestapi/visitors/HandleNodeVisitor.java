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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.contentrestapi.ContentRestApiResource;
import org.hippoecm.hst.contentrestapi.ResourceContext;

public class HandleNodeVisitor extends AbstractBaseVisitor {

    public HandleNodeVisitor(VisitorFactory factory) {
        super(factory);
    }

    public void visit(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        final String nodeName = node.getName();

        destination.put(ContentRestApiResource.NAMESPACE_PREFIX + ":name", nodeName);
        destination.put(ContentRestApiResource.NAMESPACE_PREFIX + ":uuid", node.getIdentifier());

        final Node variant = node.getNode(nodeName);
        visit(context, variant.getProperties(), destination);
        visit(context, variant.getNodes(), destination);
    }
}