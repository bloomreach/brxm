/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;

public class ErrorStatus {
    private final ClientError error;

    private Map<?, ?> parameterMap = Collections.emptyMap();

    ErrorStatus(final ClientError error, final String paramName, final Object paramValue) {
        this(error);

        final HashMap<String, Object> params = new HashMap<>();
        params.put(paramName, paramValue);
        parameterMap = params;
    }

    public ErrorStatus(final ClientError error) {
        this.error = error;
    }

    public ErrorStatus(final ClientError errorCode, final Map<?, ?> parameterMap) {
        this.error = errorCode;
        this.parameterMap = parameterMap;
    }

    public ClientError getError() {
        return error;
    }

    public Map<?, ?> getParameterMap() {
        return this.parameterMap;
    }

    public static ErrorStatus from(final ClientError errorCode) {
        return new ErrorStatus(errorCode);
    }

    public static ErrorStatus from(final ClientError errorCode, final String paramName, final Object paramValue) {
        return new ErrorStatus(errorCode, paramName, paramValue);
    }

    public static ErrorStatus from(final ClientError errorCode, final Map<?, ?> parameterMap) {
        return new ErrorStatus(errorCode, parameterMap);
    }

    /**
     * Create an {@link ClientError#UNKNOWN} error status that contains an error message in the parameter 'errorReason'
     *
     * @param message text describing the error
     * @return an 'unknown' error status with errorReason message
     */
    public static ErrorStatus unknown(final String message) {
        return new ErrorStatus(ClientError.UNKNOWN, "errorReason", message);
    }
}
