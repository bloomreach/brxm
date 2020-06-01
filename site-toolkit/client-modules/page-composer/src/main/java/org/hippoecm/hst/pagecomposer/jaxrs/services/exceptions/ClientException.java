/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions;

import java.util.Collections;
import java.util.Map;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ErrorStatus;

public class ClientException extends RuntimeException {

    private final ErrorStatus errorStatus;

    public ClientException(String message, ClientError error, Map<?, ?> parameterMap) {
        this(message, ErrorStatus.from(error, parameterMap));
    }

    protected ClientException(String message, ErrorStatus errorStatus) {
        super(message);
        this.errorStatus = errorStatus;
    }

    public ClientException(String message, ClientError error) {
        this(message, error, Collections.emptyMap());
    }

    public ClientError getError() {
        return errorStatus.getError();
    }

    public Map<?, ?> getParameterMap() {
        return errorStatus.getParameterMap();
    }

    public ErrorStatus getErrorStatus() {
        return errorStatus;
    }
}
