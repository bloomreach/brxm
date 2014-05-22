/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.util.TreeMap;

public class HintsBuilder {

    public static String INFO_STATUS = "status";
    public static String INFO_IS_LIVE = "isLive";
    public static String INFO_PREVIEW_AVAILABLE = "previewAvailable";
    public static String INFO_IN_USE_BY = "inUseBy";
    public static String INFO_REQUESTS = "requests";

    public static String ACTION_CHECK_MODIFIED = "checkModified";
    public static String ACTION_DISPOSE_EDITABLE_INSTANCE = "disposeEditableInstance";
    public static String ACTION_OBTAIN_EDITABLE_INSTANCE = "obtainEditableInstance";
    public static String ACTION_COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";
    public static String ACTION_UNLOCK = "unlock";
    public static String ACTION_REJECT_REQUEST = "rejectRequest";
    public static String ACTION_ACCEPT_REQUEST = "acceptRequest";
    public static String ACTION_CANCEL_REQUEST = "cancelRequest";
    public static String ACTION_PUBLISH = "publish";
    public static String ACTION_REQUEST_PUBLICATION = "requestPublication";
    public static String ACTION_DEPUBLISH = "depublish";
    public static String ACTION_REQUEST_DEPUBLICATION = "requestDepublication";
    public static String ACTION_LIST_VERSIONS = "listVersions";
    public static String ACTION_RETRIEVE_VERSION = "retrieveVersion";
    public static String ACTION_VERSION = "version";
    public static String ACTION_RESTORE_VERSION = "restoreVersion";
    public static String ACTION_VERSION_RESTORE_TO = "versionRestoreTo";
    public static String ACTION_REQUEST_DELETE = "requestDelete";
    public static String ACTION_DELETE = "delete";
    public static String ACTION_MOVE = "move";
    public static String ACTION_RENAME = "rename";
    public static String ACTION_COPY = "copy";


    private TreeMap<String, Serializable> info = new TreeMap<>();
    private TreeMap<String, Boolean> actions = new TreeMap<>();

    public static HintsBuilder build() {
        return new HintsBuilder();
    }

    public TreeMap<String, Serializable> info() {
        return info;
    }

    public TreeMap<String, Boolean> actions() {
        return actions;
    }

    public TreeMap<String, Serializable> hints() {
        TreeMap<String, Serializable> hints = new TreeMap<>(info);
        hints.putAll(actions);
        hints.put("hints", true);
        return hints;
    }

    public HintsBuilder status(boolean editable) {
        info.put(INFO_STATUS, editable);
        return this;
    }

    public HintsBuilder isLive(boolean isLive) {
        info.put(INFO_IS_LIVE, isLive);
        return this;
    }

    public HintsBuilder previewAvailable(boolean previewAvailable) {
        info.put(INFO_PREVIEW_AVAILABLE, previewAvailable);
        return this;
    }

    public HintsBuilder checkModified(boolean checkModified) {
        actions.put(ACTION_CHECK_MODIFIED, checkModified);
        return this;
    }

    public HintsBuilder noEdit() {
        actions.put(ACTION_DISPOSE_EDITABLE_INSTANCE, false);
        actions.put(ACTION_OBTAIN_EDITABLE_INSTANCE, false);
        actions.put(ACTION_COMMIT_EDITABLE_INSTANCE, false);
        return this;
    }

    public HintsBuilder editable() {
        actions.put(ACTION_OBTAIN_EDITABLE_INSTANCE, true);
        return this;
    }

    public HintsBuilder editing() {
        actions.put(ACTION_DISPOSE_EDITABLE_INSTANCE, true);
        actions.put(ACTION_OBTAIN_EDITABLE_INSTANCE, true);
        actions.put(ACTION_COMMIT_EDITABLE_INSTANCE, true);
        return this;
    }

    public HintsBuilder inUseBy(String holder) {
        info.put(INFO_IN_USE_BY, holder);
        return this;
    }

    public HintsBuilder unlock(boolean unlock) {
        actions.put(ACTION_UNLOCK, unlock);
        return this;
    }

    @SuppressWarnings("unchecked")
    protected TreeMap<String, Boolean> getRequestActions(String requestId) {
        TreeMap<String, TreeMap<String, Boolean>> requests = (TreeMap<String, TreeMap<String, Boolean>>)info.get(INFO_REQUESTS);
        if (requests == null) {
            requests = new TreeMap<>();
            info.put(INFO_REQUESTS, requests);
        }
        TreeMap<String, Boolean> actions = requests.get(requestId);
        if (actions == null) {
            actions = new TreeMap<>();
            requests.put(requestId, actions);
        }
        return actions;
    }

    public HintsBuilder rejectRequest(String requestId) {
        getRequestActions(requestId).put(ACTION_REJECT_REQUEST, true);
        return this;
    }

    public HintsBuilder acceptRequest(String requestId, boolean accept) {
        getRequestActions(requestId).put(ACTION_ACCEPT_REQUEST, accept);
        return this;
    }

    public HintsBuilder cancelRequest(String requestId) {
        getRequestActions(requestId).put(ACTION_CANCEL_REQUEST, true);
        return this;
    }

    public HintsBuilder publish(boolean publish) {
        actions.put(ACTION_PUBLISH, publish);
        return this;
    }

    public HintsBuilder requestPublication(boolean requestPublication) {
        actions.put(ACTION_REQUEST_PUBLICATION, requestPublication);
        return this;
    }

    public HintsBuilder depublish(boolean depublish) {
        actions.put(ACTION_DEPUBLISH, depublish);
        return this;
    }

    public HintsBuilder requestDepublication(boolean requestDepublication) {
        actions.put(ACTION_REQUEST_DEPUBLICATION, requestDepublication);
        return this;
    }

    public HintsBuilder listVersions() {
        actions.put(ACTION_LIST_VERSIONS, true);
        return this;
    }

    public HintsBuilder retrieveVersion() {
        actions.put(ACTION_RETRIEVE_VERSION, true);
        return this;
    }

    public HintsBuilder versionable() {
        actions.put(ACTION_VERSION, true);
        actions.put(ACTION_RESTORE_VERSION, true);
        actions.put(ACTION_VERSION_RESTORE_TO, true);
        return this;
    }

    public HintsBuilder requestDelete(boolean requestDelete) {
        actions.put(ACTION_REQUEST_DELETE, requestDelete);
        return this;
    }

    public HintsBuilder terminateable(boolean terminateable) {
        actions.put(ACTION_DELETE, terminateable);
        actions.put(ACTION_MOVE, terminateable);
        actions.put(ACTION_RENAME, terminateable);
        return this;
    }

    public HintsBuilder copy() {
        actions.put(ACTION_COPY, true);
        return this;
    }
}
