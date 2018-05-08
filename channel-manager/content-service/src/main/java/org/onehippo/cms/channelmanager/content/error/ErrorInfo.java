/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.repository.util.DocumentUtils;

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

    public void addParam(final String name, final Serializable value) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(name, value);
    }

    public static ErrorInfo withDisplayName(final ErrorInfo errorInfo, final Node handle) {
        if (errorInfo != null) {
            DocumentUtils.getDisplayName(handle).ifPresent(displayName -> {
                errorInfo.addParam("displayName", displayName);
            });
        }
        return errorInfo;
    }

    public enum Reason {
        ALREADY_DELETED,
        CANCELABLE_PUBLICATION_REQUEST_PENDING,
        CARDINALITY_CHANGE,  // the cardinality/multiplicity of a field value changed, which we don't support (yet).
        CORE_PROJECT,
        DOES_NOT_EXIST,
        INVALID_DATA,
        INVALID_TEMPLATE_QUERY,
        NAME_ALREADY_EXISTS,
        NO_HOLDER,
        MULTIPLE_REQUESTS,
        NO_REQUEST_PENDING,
        NOT_A_DOCUMENT,
        NOT_EDITABLE,
        NOT_A_FOLDER,
        OTHER_HOLDER,
        PART_OF_PROJECT,
        PROJECT_INVALID_STATE,
        PROJECT_NOT_FOUND,
        REQUEST_PENDING,
        SERVER_ERROR,
        SLUG_ALREADY_EXISTS,
        TEMPLATE_QUERY_NOT_FOUND,
        UNKNOWN_VALIDATOR,
        WORKFLOW_ERROR,
        WORKFLOW_ACTION_NOT_AVAILABLE,
        WORKFLOW_ACTION_NOT_IMPLEMENTED,
    }
}
