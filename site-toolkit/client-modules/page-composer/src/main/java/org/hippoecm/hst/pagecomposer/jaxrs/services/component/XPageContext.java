/*
 * Copyright 2020 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.ScheduledRequest;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.WorkflowRequest;

public class XPageContext {

    private String xPageId;
    private String xPageName;
    private String xPageState;
    private WorkflowRequest workflowRequest;
    private ScheduledRequest scheduledRequest;
    private String branchId;

    public String getXPageId() {
        return xPageId;
    }

    public String getXPageName() {
        return xPageName;
    }

    public String getXPageState() {
        return xPageState;
    }

    public WorkflowRequest getWorkflowRequest() {
        return workflowRequest;
    }

    public ScheduledRequest getScheduledRequest() {
        return scheduledRequest;
    }

    public String getBranchId() {
        return branchId;
    }

    XPageContext setXPageId(final String xPageId) {
        this.xPageId = xPageId;
        return this;
    }

    XPageContext setXPageName(final String xPageName) {
        this.xPageName = xPageName;
        return this;
    }

    XPageContext setXPageState(final String xPageState) {
        this.xPageState = xPageState;
        return this;
    }

    XPageContext setWorkflowRequest(final WorkflowRequest workflowRequest) {
        this.workflowRequest = workflowRequest;
        return this;
    }

    XPageContext setScheduledRequest(final ScheduledRequest scheduledRequest) {
        this.scheduledRequest = scheduledRequest;
        return this;
    }

    XPageContext setBranchId(final String branchId) {
        this.branchId = branchId;
        return this;
    }

}
