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

public class ComponentInfoProviderContext {

    private boolean experiencePageRequest;
    private boolean masterBranchSelected;

    private String userId;
    private String branchId;

    private ChannelContext channelContext;
    private PageContext pageContext;
    private XPageContext xPageContext;

    public boolean isMasterBranchSelected() {
        return masterBranchSelected;
    }

    public boolean isExperiencePageRequest() {
        return experiencePageRequest;
    }

    public String getUserId() {
        return userId;
    }

    public String getBranchId() {
        return branchId;
    }

    public ChannelContext getChannelContext() {
        return channelContext;
    }

    public PageContext getPageContext() {
        return pageContext;
    }

    public XPageContext getXPageContext() {
        return xPageContext;
    }

    ComponentInfoProviderContext setMasterBranchSelected(final boolean masterBranchSelected) {
        this.masterBranchSelected = masterBranchSelected;
        return this;
    }

    ComponentInfoProviderContext setExperiencePageRequest(final boolean experiencePageRequest) {
        this.experiencePageRequest = experiencePageRequest;
        return this;
    }

    ComponentInfoProviderContext setUserId(final String userId) {
        this.userId = userId;
        return this;
    }

    ComponentInfoProviderContext setBranchId(final String branchId) {
        this.branchId = branchId;
        return this;
    }

    ComponentInfoProviderContext setChannelContext(ChannelContext channelContext) {
        this.channelContext = channelContext;
        return this;
    }

    ComponentInfoProviderContext setPageContext(final PageContext pageContext) {
        this.pageContext = pageContext;
        return this;
    }

    ComponentInfoProviderContext setXPageContext(final XPageContext xPageContext) {
        this.xPageContext = xPageContext;
        return this;
    }
}
