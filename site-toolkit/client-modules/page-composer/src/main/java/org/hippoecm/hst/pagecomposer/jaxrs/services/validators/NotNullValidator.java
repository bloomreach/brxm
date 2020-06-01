/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class NotNullValidator extends AbstractValidator {

    final Object notNull;
    final ClientError clientError;
    final String errorMessage;

    public NotNullValidator(final Object notNull, final ClientError clientError) {
        this.notNull = notNull;
        this.clientError = clientError;
        this.errorMessage = null;
    }

    public NotNullValidator(final Object notNull, final ClientError clientError, final String errorMessage) {
        this.notNull = notNull;
        this.clientError = clientError;
        this.errorMessage = errorMessage;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        if (notNull == null) {
            final String message;
            if (errorMessage == null) {
                message = "Field not allowed to be null";
            } else {
                message = errorMessage;
            }
            throw new ClientException(message, clientError, Collections.singletonMap("errorReason", errorMessage));
        }
    }

}
