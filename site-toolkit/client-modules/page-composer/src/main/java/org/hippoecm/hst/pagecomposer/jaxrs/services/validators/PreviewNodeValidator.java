/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class PreviewNodeValidator extends AbstractValidator {

    final String previewConfigurationPath;
    final String id;
    final String requiredNodeType;
    final boolean inWorkspaceOnly;

    public PreviewNodeValidator(final String previewConfigurationPath,
                                final String id,
                                final String requiredNodeType,
                                final boolean inWorkspaceOnly) {
        this.previewConfigurationPath  = previewConfigurationPath;
        this.id = id;
        this.requiredNodeType = requiredNodeType;
        this.inWorkspaceOnly = inWorkspaceOnly;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        try {
            if (id == null) {
                throw new ClientException("null is not a valid uuid", ClientError.INVALID_UUID);
            }
            try {
                UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                final String message = String.format("'%s' is not a valid uuid", id);
                throw new ClientException(message, ClientError.INVALID_UUID);
            }
            final Node node = getNodeByIdentifier(id, requestContext.getSession());
            if (!node.getPath().startsWith(previewConfigurationPath + "/")) {
                final String message = String.format("'%s' is not part of currently edited preview site.", node.getPath());
                throw new ClientException(message, ClientError.ITEM_NOT_IN_PREVIEW);
            }

            if (requiredNodeType != null && !node.isNodeType(requiredNodeType)) {
                final String message = String.format("Required node of type '%s' but node '%s' of type '%s' found.", requiredNodeType, node.getPath(), node.getPrimaryNodeType().getName());
                throw new ClientException(message, ClientError.INVALID_NODE_TYPE);
            }

            if (inWorkspaceOnly && !isPreviewWorkspaceNode(node)) {
                final String message = String.format("Required workspace node but '%s' is not part of hst:workspace", node.getPath());
                throw new ClientException(message, ClientError.ITEM_NOT_IN_WORKSPACE);
            }

        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }

    }

    private boolean isPreviewWorkspaceNode(final Node node) throws RepositoryException {
        Node cr = node;
        Node root = cr.getSession().getRootNode();
        while (!cr.isSame(root)) {
            if (cr.isNodeType(HstNodeTypes.NODETYPE_HST_WORKSPACE) && cr.getParent().getName().endsWith("-preview")) {
                return true;
            }
            cr = cr.getParent();
        }
        return false;
    }

}
