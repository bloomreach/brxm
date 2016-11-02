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

/**
 * ErrorInfo provides the client with additional information about the failure of a requested operation
 *
 * By "additional", we mean information on top of the HTTP response status code.
 */
public class ErrorInfo {

    private Reason reason;

    public ErrorInfo(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        NOT_HOLDER,
        HOLDERSHIP_LOST,
        ALREADY_DELETED,
        INVALID_DATA,
        CARDINALITY_CHANGE  // the cardinality/multiplicity of a field value changed, which we don't support (yet).
        // add more specific failure reasons here.
    }
}
