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

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.onehippo.repository.branch.BranchConstants;

public class ActionProviderContextFactory {

    private final ChannelActionContextFactory channelActionContextFactory;
    private final PageActionContextFactory pageActionContextFactory;
    private final XPageActionContextFactory xPageActionContextFactory;

    public ActionProviderContextFactory(
            ChannelActionContextFactory channelActionContextFactory,
            PageActionContextFactory pageActionContextFactory,
            XPageActionContextFactory xPageActionContextFactory
    ) {
        this.channelActionContextFactory = channelActionContextFactory;
        this.pageActionContextFactory = pageActionContextFactory;
        this.xPageActionContextFactory = xPageActionContextFactory;
    }

    public ActionProviderContext make(PageComposerContextService contextService, String siteMapItemUuid) throws RepositoryException {
        final String userId = contextService.getRequestContext().getSession().getUserID();
        return new ActionProviderContext()
                .setExperiencePageRequest(contextService.isExperiencePageRequest())
                .setMasterBranchSelected(BranchConstants.MASTER_BRANCH_ID.equals(contextService.getSelectedBranchId()))
                .setUserId(userId)
                .setBranchId(contextService.getSelectedBranchId())
                .setChannelActionContext(channelActionContextFactory.make(contextService))
                .setPageActionContext(pageActionContextFactory.make(contextService, siteMapItemUuid, userId))
                .setXPageActionContext(xPageActionContextFactory.make(contextService));
    }

}
