/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.relateddocs.restapi.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.restapi.NodeVisitor;
import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.restapi.content.visitors.DefaultNodeVisitor;
import org.hippoecm.hst.restapi.scanning.PrimaryNodeTypeNodeVisitor;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;

@SuppressWarnings("unused")
@PrimaryNodeTypeNodeVisitor
public class RelatedDocsVisitor extends DefaultNodeVisitor {

    private static final String NODETYPE_RELATEDDOCS_DOCS = "relateddocs:docs";
    private static final String RELATEDDOCS_RELDOC = "relateddocs:reldoc";

    @Override
    public String getNodeType() {
        return NODETYPE_RELATEDDOCS_DOCS;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);
        LinkedHashMap<String, Object> relatedResponse = new LinkedHashMap<>();
        for (Node child : new NodeIterable(node.getNodes(RELATEDDOCS_RELDOC))) {
            NodeVisitor childVisitor = context.getVisitor(child);
            childVisitor.visit(context, child, relatedResponse);
        }
        if (relatedResponse.containsKey(RELATEDDOCS_RELDOC)) {
            response.put("related", relatedResponse.get(RELATEDDOCS_RELDOC));
        }
        else {
            response.put("related", Collections.emptyList());
        }
    }

    @Override
    protected boolean skipChild(final ResourceContext context, final ContentTypeChild childType, final Node child)
            throws RepositoryException {
        return child.isNodeType(RELATEDDOCS_RELDOC);
    }
}
