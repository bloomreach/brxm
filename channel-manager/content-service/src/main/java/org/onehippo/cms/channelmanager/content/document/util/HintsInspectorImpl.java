/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.error.ErrorInfo;

public class HintsInspectorImpl implements HintsInspector {

    private static final String HINT_IN_USE_BY = "inUseBy";
    private static final String HINT_COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";
    private static final String HINT_DISPOSE_EDITABLE_INSTANCE = "disposeEditableInstance";
    private static final String HINT_OBTAIN_EDITABLE_INSTANCE = "obtainEditableInstance";
    private static final String HINT_REQUESTS = "requests";

    @Override
    public boolean canCreateDraft(Map<String, Serializable> hints) {
        return isActionAvailable(hints, HINT_OBTAIN_EDITABLE_INSTANCE);
    }

    @Override
    public boolean canUpdateDraft(Map<String, Serializable> hints) {
        return isActionAvailable(hints, HINT_COMMIT_EDITABLE_INSTANCE);
    }

    @Override
    public boolean canDeleteDraft(Map<String, Serializable> hints) {
        return isActionAvailable(hints, HINT_DISPOSE_EDITABLE_INSTANCE);
    }

    @Override
    public Optional<ErrorInfo> determineEditingFailure(final Map<String, Serializable> hints, final Session session) {
        if (hints.containsKey(HINT_IN_USE_BY)) {
            final Map<String, Serializable> params = new HashMap<>();
            final String userId = (String) hints.get(HINT_IN_USE_BY);
            params.put("userId", userId);
            UserUtils.getUserName(userId, session).ifPresent(userName -> params.put("userName", userName));
            return errorInfo(ErrorInfo.Reason.OTHER_HOLDER, params);
        }
        if (hints.containsKey(HINT_REQUESTS)) {
            return errorInfo(ErrorInfo.Reason.REQUEST_PENDING, null);
        }
        return Optional.empty();
    }

    protected Optional<ErrorInfo> errorInfo(ErrorInfo.Reason reason, Map<String, Serializable> params) {
        return Optional.of(new ErrorInfo(reason, params));
    }

    private boolean isActionAvailable(Map<String, Serializable> hints, final String action) {
        return hints.containsKey(action) && ((Boolean) hints.get(action));
    }
}
