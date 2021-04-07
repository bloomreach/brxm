/*
 * Copyright 2014-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;
import java.util.TreeMap;

public class HintsBuilder {

    public static String INFO_STATUS = "status";
    public static String INFO_IS_LIVE = "isLive";
    public static String INFO_PREVIEW_AVAILABLE = "previewAvailable";
    public static String INFO_IN_USE_BY = "inUseBy";
    public static String INFO_REQUESTS = "requests";
    public static final String TRANSFERABLE = "transferable";

    public static String ACTION_CHECK_MODIFIED = "checkModified";
    public static String ACTION_DISPOSE_EDITABLE_INSTANCE = "disposeEditableInstance";
    public static String ACTION_OBTAIN_EDITABLE_INSTANCE = "obtainEditableInstance";
    public static final String ACTION_EDIT_DRAFT = "editDraft";
    public static final String ACTION_SAVE_DRAFT = "saveDraft";
    public static String ACTION_COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";
    public static String ACTION_UNLOCK = "unlock";
    public static String ACTION_REJECT_REQUEST = "rejectRequest";
    public static String ACTION_ACCEPT_REQUEST = "acceptRequest";
    public static String ACTION_CANCEL_REQUEST = "cancelRequest";
    public static final String ACTION_INFO_REQUEST = "infoRequest";
    public static String ACTION_PUBLISH = "publish";
    public static String ACTION_REQUEST_PUBLICATION = "requestPublication";
    public static String ACTION_DEPUBLISH = "depublish";
    public static String ACTION_REQUEST_DEPUBLICATION = "requestDepublication";
    public static String ACTION_LIST_VERSIONS = "listVersions";
    public static String ACTION_RETRIEVE_VERSION = "retrieveVersion";
    public static String ACTION_VERSION = "version";
    public static String ACTION_RESTORE_VERSION = "restoreVersion";
    public static String ACTION_VERSION_RESTORE_TO = "versionRestoreTo";
    public static String ACTION_RESTORE_VERSION_TO_BRANCH = "restoreVersionToBranch";
    public static String ACTION_LIST_BRANCHES = "listBranches";
    public static String ACTION_BRANCH = "branch";
    public static String ACTION_GET_BRANCH = "getBranch";
    public static String ACTION_REMOVE_BRANCH = "removeBranch";
    public static String ACTION_REINTEGRATE_BRANCH = "reintegrateBranch";
    public static String ACTION_PUBLISH_BRANCH = "publishBranch";
    public static String ACTION_DEPUBLISH_BRANCH = "depublishBranch";
    public static String ACTION_CHECKOUT_BRANCH = "checkoutBranch";
    public static String ACTION_DELETE = "delete";
    public static String ACTION_MOVE = "move";
    public static String ACTION_RENAME = "rename";
    public static String ACTION_COPY = "copy";
    public static String ACTION_SAVE_UNPUBLISHED = "saveUnpublished";
    public static String ACTION_CAMPAIGN = "campaign";
    public static String ACTION_REMOVE_CAMPAIGN = "removeCampaign";

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

    public HintsBuilder obtainEditableInstance(boolean value) {
        actions.put(ACTION_OBTAIN_EDITABLE_INSTANCE, value);
        return this;
    }

    public HintsBuilder commitEditableInstance(boolean value) {
        actions.put(ACTION_COMMIT_EDITABLE_INSTANCE, value);
        return this;
    }

    public HintsBuilder disposeEditableInstance(boolean value) {
        actions.put(ACTION_DISPOSE_EDITABLE_INSTANCE, value);
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
        TreeMap<String, TreeMap<String, Boolean>> requests = (TreeMap<String, TreeMap<String, Boolean>>) info.get(INFO_REQUESTS);
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

    public HintsBuilder infoRequest(String requestId){
        getRequestActions(requestId).put(ACTION_INFO_REQUEST, true);
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
        actions.put(ACTION_RESTORE_VERSION_TO_BRANCH, true);
        return this;
    }

    public HintsBuilder listBranches() {
        actions.put(ACTION_LIST_BRANCHES, true);
        return this;
    }

    public HintsBuilder branch(final boolean branchable) {
        actions.put(ACTION_BRANCH, branchable);
        return this;
    }

    public HintsBuilder campaign(final boolean canCamapaign) {
        actions.put(ACTION_CAMPAIGN, canCamapaign);
        return this;
    }
    public HintsBuilder removeCampaign(final boolean canRemoveCampaign) {
        actions.put(ACTION_REMOVE_CAMPAIGN, canRemoveCampaign);
        return this;
    }

    public HintsBuilder getBranch(final boolean canGetBranch) {
        actions.put(ACTION_GET_BRANCH, canGetBranch);
        return this;
    }

    public HintsBuilder checkoutBranch(final boolean canCheckout) {
        actions.put(ACTION_CHECKOUT_BRANCH, canCheckout);
        return this;
    }

    public HintsBuilder removeBranch(final boolean branchRemovable) {
        actions.put(ACTION_REMOVE_BRANCH, branchRemovable);
        return this;
    }

    public HintsBuilder reintegrateBranch(final boolean branchReingratable) {
        actions.put(ACTION_REINTEGRATE_BRANCH, branchReingratable);
        return this;
    }

    public HintsBuilder publishBranch(final boolean publishableBranch) {
        actions.put(ACTION_PUBLISH_BRANCH, publishableBranch);
        return this;
    }

    public HintsBuilder depublishBranch(final boolean depublishableBranch) {
        actions.put(ACTION_DEPUBLISH_BRANCH, depublishableBranch);
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

    public HintsBuilder saveUnpublished(boolean enabled) {
        actions.put(ACTION_SAVE_UNPUBLISHED, enabled);
        return this;
    }

    public HintsBuilder editDraft() {
        actions.put(ACTION_EDIT_DRAFT, true);
        return this;
    }

    public HintsBuilder saveDraft() {
        actions.put(ACTION_SAVE_DRAFT, true);
        return this;
    }
}
