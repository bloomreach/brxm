/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_MOVE_TO_SELF_OR_DESCENDANT;

public class ItemNotSameOrDescendantOfValidator extends AbstractValidator {

    private String validateUUID;
    private String targetUUID;

    public ItemNotSameOrDescendantOfValidator(final String validateUUID, final String targetUUID) {
        this.validateUUID = validateUUID;
        this.targetUUID = targetUUID;
    }

    @Override
    public void validate(final HstRequestContext requestContext) throws RuntimeException {
        try {
            final Session session = requestContext.getSession();
            final String validate = getNodeByIdentifier(validateUUID, session).getPath();
            final String target = getNodeByIdentifier(targetUUID, session).getPath();
            if (validate.equals(target) || validate.startsWith(target + "/")) {
                final String message = String.format("Node '%s' is the same as or a descendant of '%s", validate, target);
                throw new ClientException(message, INVALID_MOVE_TO_SELF_OR_DESCENDANT);
            }
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }
    }
}
