/*
 *  Copyright 2020 Bloomreach
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.state;

import org.hippoecm.hst.pagecomposer.jaxrs.services.state.util.ScheduledRequest;
import org.hippoecm.hst.pagecomposer.jaxrs.services.state.util.WorkflowRequest;

public final class XPageState {

    private String branchId;
    private String id;
    private String name;
    private String state;
    private WorkflowRequest workflowRequest;
    private ScheduledRequest scheduledRequest;

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public WorkflowRequest getWorkflowRequest() {
        return workflowRequest;
    }

    public void setWorkflowRequest(final WorkflowRequest workflowRequest) {
        this.workflowRequest = workflowRequest;
    }

    public ScheduledRequest getScheduledRequest() {
        return scheduledRequest;
    }

    public void setScheduledRequest(final ScheduledRequest scheduledRequest) {
        this.scheduledRequest = scheduledRequest;
    }

}
