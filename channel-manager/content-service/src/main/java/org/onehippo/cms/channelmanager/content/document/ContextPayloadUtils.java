/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.onehippo.cms.channelmanager.content.document;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;

/**
 * Utility methods for interacting with the context payload.
 */
final class ContextPayloadUtils {

    // TODO (meggermont): Replace Hard-coded constant copied from WpmConstants with constant in HST or repository.
    // If the enterprise wpm feature is enabled, then the channel manager client code will call a wpm REST method
    // to PUT/GET/DELETE the active project. However, the wpm feature has evolved such that we now have the
    // community concept of a document branch.  Unfortunately there is not enough time to fully implement this
    // concept in the channel manager. So for now we just hard code the attribute key here.
    //
    private static final String ATTRIBUTE_SESSION_CONTEXT_ACTIVE_BRANCH_ID = "com.onehippo.cms7.services.wpm.WpmConstants.active_session_context_project_id";

    private ContextPayloadUtils() {
    }

    static String getBranchId(Map<String, Serializable> contextPayload) {
        return Optional.ofNullable(contextPayload)
                .map(m -> (String) m.getOrDefault(ATTRIBUTE_SESSION_CONTEXT_ACTIVE_BRANCH_ID, null))
                .orElse(MASTER_BRANCH_ID);
    }

}
