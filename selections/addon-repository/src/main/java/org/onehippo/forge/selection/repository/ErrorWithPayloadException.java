/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.forge.selection.repository;

import javax.ws.rs.core.Response;

public abstract class ErrorWithPayloadException extends Exception {
    private final Object payload;
    private final Response.Status status;

    /**
     * @param status  HTTP status code
     * @param payload response payload, may be null
     */
    protected ErrorWithPayloadException(final Response.Status status, final Object payload) {
        this.status = status;
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    public Response.Status getStatus() {
        return status;
    }
}
