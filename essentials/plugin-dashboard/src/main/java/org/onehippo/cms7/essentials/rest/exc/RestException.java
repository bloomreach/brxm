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

package org.onehippo.cms7.essentials.rest.exc;

import javax.ws.rs.core.Response;

/**
 * @version "$Id$"
 */
public class RestException extends Throwable {

    private final Response.Status errorCode;
    private static final long serialVersionUID = 1L;


    public RestException(final Response.Status errorCode) {
        this.errorCode = errorCode;
    }

    public RestException(final String message, final Response.Status errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public RestException(final String message, final Response.Status errorCode, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public RestException(final Response.Status errorCode, final Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    protected RestException(final String message, final Response.Status errorCode, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;

    }

    public Response.Status getErrorCode() {
        return errorCode;
    }


}
