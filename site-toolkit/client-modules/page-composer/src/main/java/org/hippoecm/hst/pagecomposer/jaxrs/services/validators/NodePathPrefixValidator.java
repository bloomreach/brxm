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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class NodePathPrefixValidator extends AbstractValidator {

    final String nodePathPrefix;
    private boolean liveAllowed;
    final String id;
    final String requiredNodeType;

    public NodePathPrefixValidator(final String nodePathPrefix,
                                   final String id,
                                   final String requiredNodeType) {
       this(nodePathPrefix, false, id, requiredNodeType);
    }

    public NodePathPrefixValidator(final String nodePathPrefix, final boolean liveAllowed, final String id, final String requiredNodeType) {
        this.nodePathPrefix  = nodePathPrefix;
        this.liveAllowed = liveAllowed;
        this.id = id;
        this.requiredNodeType = requiredNodeType;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        try {
            final Node node = getNodeByIdentifier(id, requestContext.getSession());
            String path = node.getPath();
            if (!validPath(path, liveAllowed)) {
                final String message = String.format("'%s' is not part of required node path '%s'.", path, nodePathPrefix);
                throw new ClientException(message, ClientError.ITEM_NOT_CORRECT_LOCATION);
            }
            if (requiredNodeType != null && !node.isNodeType(requiredNodeType)) {
                final String message = String.format("Required node of type '%s' but node '%s' of type '%s' found.", requiredNodeType, path, node.getPrimaryNodeType().getName());
                throw new ClientException(message, ClientError.INVALID_NODE_TYPE);
            }

        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }
    }

    private boolean validPath(final String path, final boolean liveAllowed) {
        if (path.startsWith(nodePathPrefix + "/")) {
            return true;
        }
        if (liveAllowed && nodePathPrefix.endsWith("-preview")) {
            return path.startsWith(StringUtils.substringBeforeLast(nodePathPrefix, "-preview") + "/");
        }
        return false;
    }

}
