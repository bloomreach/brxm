/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.action;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;

final class ActionProviderContextImpl implements ActionProviderContext {

    private final String branchId;
    private final String channelId;
    private final String userId;
    private final PageComposerContextService contextService;

    public ActionProviderContextImpl(PageComposerContextService contextService) {
        try {
            this.userId = contextService.getRequestContext().getSession().getUserID();
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
        this.branchId = contextService.getSelectedBranchId();
        final String liveOrPreviewChannelId = contextService.getEditingPreviewChannel().getId();
        if (liveOrPreviewChannelId.endsWith("-preview")) {
            this.channelId = liveOrPreviewChannelId.substring(0, liveOrPreviewChannelId.lastIndexOf("-preview"));
        } else {
            this.channelId = liveOrPreviewChannelId;
        }
        this.contextService = contextService;
    }

    @Override
    public String getBranchId() {
        return branchId;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public PageComposerContextService getContextService() {
        return contextService;
    }
}
