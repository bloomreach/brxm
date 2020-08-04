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

public class XPageStateContext {

    private String xPageId;
    private String xPageName;
    private String xPageState;
    private WorkflowRequest workflowRequest;
    private ScheduledRequest scheduledRequest;
    private String branchId;

    public String getXPageId() {
        return xPageId;
    }

    XPageStateContext setXPageId(final String xPageId) {
        this.xPageId = xPageId;
        return this;
    }

    public String getXPageName() {
        return xPageName;
    }

    XPageStateContext setXPageName(final String xPageName) {
        this.xPageName = xPageName;
        return this;
    }

    public String getXPageState() {
        return xPageState;
    }

    public XPageStateContext setXPageState(final String xPageState) {
        this.xPageState = xPageState;
        return this;
    }

    public WorkflowRequest getWorkflowRequest() {
        return workflowRequest;
    }

    public XPageStateContext setWorkflowRequest(final WorkflowRequest workflowRequest) {
        this.workflowRequest = workflowRequest;
        return this;
    }

    public ScheduledRequest getScheduledRequest() {
        return scheduledRequest;
    }

    public XPageStateContext setScheduledRequest(final ScheduledRequest scheduledRequest) {
        this.scheduledRequest = scheduledRequest;
        return this;
    }

    public String getBranchId() {
        return branchId;
    }

    public XPageStateContext setBranchId(final String branchId) {
        this.branchId = branchId;
        return this;
    }
}
