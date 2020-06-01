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

import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

abstract class AbstractValidator implements Validator {

    protected final Node getNodeByIdentifier(String id, Session session) throws RepositoryException {
        if (id == null) {
            throw new ClientException("uuid not allowed to be null", ClientError.INVALID_UUID);
        }
        try {
            // if not valid id, we want an IllegalArgumentException
            UUID.fromString(id);
            return session.getNodeByIdentifier(id);
        } catch (ItemNotFoundException e) {
            final String message = String.format("Repository configuration not found for node with id %s : %s", id, e.toString());
            throw new ClientException(message, ClientError.ITEM_NOT_FOUND);
        } catch (IllegalArgumentException e) {
            final String message = String.format("'%s' is not a valid uuid", id);
            throw new ClientException(message, ClientError.INVALID_UUID);
        }
    }

}
