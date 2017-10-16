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
