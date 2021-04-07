/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hippoecm.repository.api;

import java.util.HashMap;
import java.util.Map;

public class DocumentWorkflowAction implements ActionAware, WorkflowAction {

    public static DocumentWorkflowAction unlock() {
        return new DocumentWorkflowAction("unlock");
    }

    public static DocumentWorkflowAction publish() {
        return new DocumentWorkflowAction("publish");
    }

    public static DocumentWorkflowAction checkModified() {
        return new DocumentWorkflowAction("checkModified", false);
    }

    public static DocumentWorkflowAction obtainEditableInstance() {
        return new DocumentWorkflowAction("obtainEditableInstance");
    }

    public static DocumentWorkflowAction commitEditableInstance() {
        return new DocumentWorkflowAction("commitEditableInstance");
    }

    public static DocumentWorkflowAction editDraft() {
        return new DocumentWorkflowAction("editDraft");
    }

    public static DocumentWorkflowAction saveDraft() {
        return new DocumentWorkflowAction("saveDraft");
    }

    public static DocumentWorkflowAction disposeEditableInstance() {
        return new DocumentWorkflowAction("disposeEditableInstance");
    }

    public static DocumentWorkflowAction requestDepublication() {
        return new DocumentWorkflowAction("requestDepublication");
    }

    public static DocumentWorkflowAction requestPublication() {
        return new DocumentWorkflowAction("requestPublication");
    }

    public static DocumentWorkflowAction delete() {
        return new DocumentWorkflowAction("delete");
    }

    public static DocumentWorkflowAction rename() {
        return new DocumentWorkflowAction("rename");
    }

    public static DocumentWorkflowAction copy() {
        return new DocumentWorkflowAction("copy");
    }

    public static DocumentWorkflowAction move() {
        return new DocumentWorkflowAction("move");
    }

    public static DocumentWorkflowAction depublish() {
        return new DocumentWorkflowAction("depublish");
    }

    public static DocumentWorkflowAction cancelRequest() {
        return new DocumentWorkflowAction("cancelRequest");
    }

    public static DocumentWorkflowAction acceptRequest() {
        return new DocumentWorkflowAction("acceptRequest");
    }

    public static DocumentWorkflowAction rejectRequest() {
        return new DocumentWorkflowAction("rejectRequest");
    }

    public static DocumentWorkflowAction version() {
        return new DocumentWorkflowAction("version");
    }

    public static DocumentWorkflowAction versionRestoreTo() {
        return new DocumentWorkflowAction("versionRestoreTo");
    }

    public static DocumentWorkflowAction restoreVersion() {
        return new DocumentWorkflowAction("restoreVersion");
    }

    public static DocumentWorkflowAction restoreVersionToBranch() {
        return new DocumentWorkflowAction("restoreVersionToBranch");
    }

    public static DocumentWorkflowAction listVersions() {
        return new DocumentWorkflowAction("listVersions", false);
    }

    public static DocumentWorkflowAction retrieveVersion() {
        return new DocumentWorkflowAction("retrieveVersion");
    }

    public static DocumentWorkflowAction listBranches() {
        return new DocumentWorkflowAction("listBranches", false);
    }

    public static DocumentWorkflowAction branch() {
        return new DocumentWorkflowAction("branch");
    }
    public static DocumentWorkflowAction getBranch() {
        return new DocumentWorkflowAction("getBranch", false);
    }

    public static DocumentWorkflowAction checkoutBranch() {
        return new DocumentWorkflowAction("checkoutBranch");
    }

    public static DocumentWorkflowAction reintegrateBranch() {
        return new DocumentWorkflowAction("reintegrateBranch");
    }

    public static DocumentWorkflowAction publishBranch() {
        return new DocumentWorkflowAction("publishBranch");
    }

    public static DocumentWorkflowAction depublishBranch() {
        return new DocumentWorkflowAction("depublishBranch");
    }

    public static DocumentWorkflowAction removeBranch() {
        return new DocumentWorkflowAction("removeBranch");
    }

    public static DocumentWorkflowAction none() {
        return new DocumentWorkflowAction("None", false);
    }

    public static DocumentWorkflowAction saveUnpublished() {
        return new DocumentWorkflowAction("saveUnpublished");
    }

    public static DocumentWorkflowAction campaign() {
        return new DocumentWorkflowAction("campaign");
    }


    public static DocumentWorkflowAction removeCampaign() {
        return new DocumentWorkflowAction("removeCampaign");
    }

    private final String action;
    private boolean mutates;
    private String requestIdentifier;
    private final Map<String, Object> eventPayload = new HashMap<>();

    public DocumentWorkflowAction(final String action) {
        this.action = action;
        // default is mutates true
        this.mutates = true;
    }

    public DocumentWorkflowAction(final String action, final boolean mutates) {
        this.action = action;
        this.mutates = mutates;
    }

    public String getRequestIdentifier() {
        return requestIdentifier;
    }

    public DocumentWorkflowAction requestIdentifier(final String requestIdentifier) {

        this.requestIdentifier = requestIdentifier;
        return this;
    }

    public Map<String, Object> getEventPayload() {
        return eventPayload;
    }

    public DocumentWorkflowAction addEventPayload(final PayloadKey key, final Object value) {
        eventPayload.put(key.getKey(), value);
        return this;
    }

    public DocumentWorkflowAction addEventPayload(final String key, final Object value) {
        eventPayload.put(key, value);
        return this;
    }

    public String getAction() {
        return action;
    }

    public boolean isMutates() {
        return mutates;
    }

    public enum DocumentPayloadKey implements PayloadKey {
        TARGET_DATE("targetDate"),
        NAME("name"),
        DESTINATION("destination"),
        DATE("date"),
        TARGET_DOCUMENT("target"),
        REQUEST("request"),
        REASON("reason"),
        BRANCH_ID("branchId"),
        VERSION("version"),
        BRANCH_NAME("branchName"),
        FROZEN_NODE_ID("frozenNodeId"),
        FROM_DATE("fromDate"),
        TO_DATE("toDate"),
        STATE("state"),
        ;

        private final String key;

        DocumentPayloadKey(final String key) {
            this.key = key;
        }


        @Override
        public String getKey() {
            return key;
        }
    }

}
