/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class CopyNodeValidator extends AbstractValidator {

    private final String sourceUUID;
    private final String targetUUID;

    public CopyNodeValidator(final String sourceUUID, final String targetUUID) {
        this.sourceUUID = sourceUUID;
        this.targetUUID = targetUUID;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        final String message = "Cannot copy a node below itself";
        if (sourceUUID.equals(targetUUID)) {
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }
        try {
            final Node sourceNode = getNodeByIdentifier(sourceUUID, requestContext.getSession());
            final Node targetNode = getNodeByIdentifier(targetUUID, requestContext.getSession());
            if (targetNode.getPath().startsWith(sourceNode.getPath() + "/")) {
                throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED,
                        Collections.singletonMap("errorReason", message));
            }
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

}
