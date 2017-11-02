/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * ErrorInfo provides the client with additional information about the failure of a requested operation
 * <p>
 * By "additional", we mean information on top of the HTTP response status code.
 */
@JsonInclude(Include.NON_NULL)
public class ErrorInfo {

    private final Reason reason;
    private Map<String, Serializable> params;

    public ErrorInfo(final Reason reason) {
        this(reason, null);
    }

    public ErrorInfo(final Reason reason, final String key, final String value) {
        this(reason, Collections.singletonMap(key, value));
    }

    public ErrorInfo(final Reason reason, final Map<String, Serializable> params) {
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
        ALREADY_DELETED,
        CARDINALITY_CHANGE,  // the cardinality/multiplicity of a field value changed, which we don't support (yet).
        INVALID_DATA,
        NO_HOLDER,
        NOT_A_DOCUMENT,
        OTHER_HOLDER,
        REQUEST_PENDING,
        UNKNOWN_VALIDATOR,
        INVALID_TEMPLATE_QUERY,
        TEMPLATE_QUERY_NOT_FOUND,
        NOT_A_FOLDER,
        NAME_ALREADY_EXISTS,
        SLUG_ALREADY_EXISTS,
        // add more specific failure reasons here.
    }
}
