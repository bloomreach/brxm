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
import javax.jcr.Session;

import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class ChildExistsValidator extends AbstractValidator {

    final String parentId;
    final String childId;

    public ChildExistsValidator(final String parentId, final String childId) {
        this.parentId = parentId;
        this.childId = childId;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        try {
            final Session session = requestContext.getSession();
            final Node parent = getNodeByIdentifier(parentId, session);
            final Node child = getNodeByIdentifier(childId, session);
            if (!parent.isSame(child.getParent())) {
                final String message = String.format("Node '%s' is not a child of '%s", child, parent);
                throw new ClientException(message, ClientError.ITEM_NOT_CHILD_OF_PARENT);
            }
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }
    }

}
