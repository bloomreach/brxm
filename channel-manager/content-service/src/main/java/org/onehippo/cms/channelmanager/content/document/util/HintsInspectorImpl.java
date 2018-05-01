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
 */
package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.UserUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;

import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.PUBLISH;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.HINT_COMMIT_EDITABLE_INSTANCE;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.HINT_DISPOSE_EDITABLE_INSTANCE;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.HINT_OBTAIN_EDITABLE_INSTANCE;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.isHintActionFalse;
import static org.onehippo.cms.channelmanager.content.document.util.EditingUtils.isHintActionTrue;


public class HintsInspectorImpl implements HintsInspector {

    private static final String HINT_IN_USE_BY = "inUseBy";
    private static final String HINT_REQUESTS = "requests";

    @Override
    public boolean canCreateDraft(Map<String, Serializable> hints) {
        return isHintActionTrue(hints, HINT_OBTAIN_EDITABLE_INSTANCE);
    }

    @Override
    public boolean canUpdateDraft(Map<String, Serializable> hints) {
        return isHintActionTrue(hints, HINT_COMMIT_EDITABLE_INSTANCE);
    }

    @Override
    public boolean canDeleteDraft(Map<String, Serializable> hints) {
        return isHintActionTrue(hints, HINT_DISPOSE_EDITABLE_INSTANCE);
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
            if (hasCancelablePublicationRequest(hints, session)) {
                return errorInfo(ErrorInfo.Reason.CANCELABLE_PUBLICATION_REQUEST_PENDING, null);
            }
            return errorInfo(ErrorInfo.Reason.REQUEST_PENDING, null);
        }

        if (isHintActionFalse(hints, HINT_OBTAIN_EDITABLE_INSTANCE)) {
            return errorInfo(ErrorInfo.Reason.NOT_EDITABLE, null);
        }

        return Optional.empty();
    }

    private boolean hasCancelablePublicationRequest(final Map<String, Serializable> hints, final Session session) {
        final Map<String, Serializable> hintRequests = (Map<String, Serializable>) hints.get(HINT_REQUESTS);
        for (final Map.Entry<String, Serializable> entry : hintRequests.entrySet()) {
            final Map<String, Boolean> entryMap = (Map<String, Boolean>) entry.getValue();
            if (entryMap.containsKey("cancelRequest")) {
                final String requestNodeId = entry.getKey();
                try {
                    final Node requestNode = session.getNodeByIdentifier(requestNodeId);
                    final String type = requestNode.getProperty(HIPPOSTDPUBWF_TYPE).getString();
                    if (type.equals(PUBLISH)) {
                        return true;
                    }
                } catch (final RepositoryException ignore) {
                }
            }
        }
        return false;
    }

    protected Optional<ErrorInfo> errorInfo(ErrorInfo.Reason reason, Map<String, Serializable> params) {
        return Optional.of(new ErrorInfo(reason, params));
    }
}
