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

import java.util.Optional;

import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.ScheduledRequest;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.WorkflowRequest;

public class XPageContext {

    private String xPageId;
    private String xPageName;
    private String xPageState;
    private WorkflowRequest workflowRequest;
    private ScheduledRequest scheduledRequest;
    private String branchId;
    private Boolean publishable;
    private Boolean unpublishable;
    private Boolean requestPublication;
    private Boolean requestDepublication;
    private Boolean copyAllowed;
    private Boolean moveAllowed;
    private Boolean deleteAllowed;

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

    public Optional<Boolean> isPublishable() {
        return Optional.ofNullable(publishable);
    }

    public XPageContext setPublishable(final Boolean publishable) {
        this.publishable = publishable;
        return this;
    }

    public Optional<Boolean> isUnpublishable() {
        return Optional.ofNullable(unpublishable);
    }

    public XPageContext setUnpublishable(final Boolean unpublishable) {
        this.unpublishable = unpublishable;
        return this;
    }

    public XPageContext setRequestPublication(final Boolean requestPublication) {
        this.requestPublication = requestPublication;
        return this;
    }

    public Optional<Boolean> isRequestPublication() {
        return Optional.ofNullable(requestPublication);
    }

    public XPageContext setRequestDepublication(final Boolean requestDepublication) {
        this.requestDepublication = requestDepublication;
        return this;
    }

    public Optional<Boolean> isRequestDepublication() {
        return Optional.ofNullable(requestDepublication);
    }

    public XPageContext setCopyAllowed(final Boolean copyAllowed) {
        this.copyAllowed = copyAllowed;
        return this;
    }

    public Boolean isCopyAllowed() {
        return copyAllowed;
    }

    public XPageContext setMoveAllowed(final Boolean moveAllowed) {
        this.moveAllowed = moveAllowed;
        return this;
    }

    public Boolean isMoveAllowed() {
        return moveAllowed;
    }

    public XPageContext setDeleteAllowed(final Boolean deleteAllowed) {
        this.deleteAllowed = deleteAllowed;
        return this;
    }

    public Boolean isDeleteAllowed() {
        return deleteAllowed;
    }
}
