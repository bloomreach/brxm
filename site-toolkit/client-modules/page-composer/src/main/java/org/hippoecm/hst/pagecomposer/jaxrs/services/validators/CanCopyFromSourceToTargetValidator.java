/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CanCopyFromSourceToTargetValidator extends AbstractValidator {

    private static final Logger log = LoggerFactory.getLogger(CanCopyFromSourceToTargetValidator.class);

    private final String uuidSource;
    private final String uuidTarget;

    public CanCopyFromSourceToTargetValidator(final String uuidSource, final String uuidTarget) {
        this.uuidSource = uuidSource;
        this.uuidTarget = uuidTarget;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        final String message = "Cannot copy a page below itself";
        if (uuidSource.equals(uuidTarget)) {
            throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED, Collections.singletonMap("errorReason", message));
        }
        try {
            final Node sourceNode = getNodeByIdentifier(uuidSource, requestContext.getSession());
            final Node targetNode = getNodeByIdentifier(uuidTarget, requestContext.getSession());
            if (targetNode.getPath().startsWith(sourceNode.getPath() + "/")) {
                throw new ClientException(message, ClientError.ITEM_CANNOT_BE_CLONED,
                        Collections.singletonMap("errorReason", message));
            }
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

}
