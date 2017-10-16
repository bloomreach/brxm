/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

public enum DocumentWorkflowAction implements ActionAware {
    UNLOCK("unlock"),
    PUBLISH("publish"),
    CHECK_MODIFIED("checkModified"),
    OBTAIN_EDITABLE_INSTANCE("obtainEditableInstance"),
    COMMIT_EDITABLE_INSTANCE("commitEditableInstance"),
    DISPOSE_EDITABLE_INSTANCE("disposeEditableInstance"),
    REQUEST_DELETE("requestDelete"),
    REQUEST_DEPUBLICATION("requestDepublication"),
    REQUEST_PUBLICATION("requestPublication"),
    DELETE("delete"),
    RENAME("rename"),
    COPY("copy"),
    MOVE("move"),
    DEPUBLISH("depublish"),
    CANCEL_REQUEST("cancelRequest"),
    ACCEPT_REQUEST("acceptRequest"),
    REJECT_REQUEST("rejectRequest"),
    VERSION("version"),
    VERSION_RESTORE_TO("versionRestoreTo"),
    RESTORE_VERSION("restoreVersion"),
    LIST_VERSIONS("listVersions"),
    RETRIEVE_VERSION("retrieveVersion");

    private final String action;

    DocumentWorkflowAction(final String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

}
