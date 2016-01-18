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
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.contentrestapi.ContentRestApiResource;
import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.NodeIterable;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public class HandleNodeVisitor extends AbstractNodeVisitor {

    public HandleNodeVisitor(VisitorFactory factory) {
        super(factory);
    }

    @Override
    public String getNodeType() {
        return NT_HANDLE;
    }

    public void visit(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        final String nodeName = node.getName();

        destination.put("id", node.getIdentifier());
        destination.put("name", nodeName);

        final Node variant = node.getNode(nodeName);
        Map<String, Object> content = new TreeMap<>();
        destination.put("content", nodeName);

        visit(context, new NodeIterable(variant.getNodes()).iterator(), content);
    }

    @Override
    protected void visitChildren(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        // variant already handled above.
    }
}