/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.restapi.content.visitors;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.restapi.NodeVisitor;
import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public class HippoHandleVisitor extends AbstractNodeVisitor {

    private static final Logger log = LoggerFactory.getLogger(HippoHandleVisitor.class);
    private static final String ID_TAG = "id";
    private static final String NAME_TAG = "name";
    private static final String DISPLAY_NAME_TAG = "displayName";

    @Override
    public String getNodeType() {
        return NT_HANDLE;
    }

    public void visit(final ResourceContext context, final Node node, final Map<String, Object> response) throws RepositoryException {
        final String nodeName = node.getName();

        if (!node.hasNode(nodeName)) {
            log.info("Variant for handle '{}' for session '{}' is not present", node.getPath(), node.getSession().getUserID());
            return;
        }

        response.put(ID_TAG, node.getIdentifier());
        response.put(NAME_TAG, nodeName);

        if (node instanceof HippoNode) {
            response.put(DISPLAY_NAME_TAG, ((HippoNode) node).getDisplayName());
        }

        final Node variant = node.getNode(nodeName);
        NodeVisitor variantVisitor = context.getVisitor(variant);
        variantVisitor.visit(context, variant, response);
    }
}