/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.error;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ErrorInfo provides the client with additional information about the failure of a requested operation
 *
 * By "additional", we mean information on top of the HTTP response status code.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorInfo {

    private final Reason reason;
    private Map<String, Serializable> params;

    public ErrorInfo(Reason reason) {
        this(reason, null);
    }

    public ErrorInfo(Reason reason, Map<String, Serializable> params) {
        this.reason = reason;
        this.params = params;
    }

    public Reason getReason() {
        return reason;
    }

    public Map<String, Serializable> getParams() {
        return params;
    }

    public void setParams(final Map<String, Serializable> params) {
        this.params = params;
    }

    public enum Reason {
        NO_HOLDER,
        OTHER_HOLDER,
        REQUEST_PENDING,
        UNKNOWN_VALIDATOR,
        ALREADY_DELETED,
        INVALID_DATA,
        CARDINALITY_CHANGE,  // the cardinality/multiplicity of a field value changed, which we don't support (yet).
        NOT_A_DOCUMENT
        // add more specific failure reasons here.
    }
}
