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

import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.onehippo.repository.branch.BranchConstants;

final class ActionStateProviderContextFactory {

    private final ChannelContextFactory channelContextFactory;
    private final PageContextFactory pageContextFactory;
    private final XPageContextFactory xPageContextFactory;

    ActionStateProviderContextFactory(
            final ChannelContextFactory channelContextFactory,
            final PageContextFactory pageContextFactory,
            final XPageContextFactory xPageContextFactory
    ) {
        this.channelContextFactory = channelContextFactory;
        this.pageContextFactory = pageContextFactory;
        this.xPageContextFactory = xPageContextFactory;
    }


    ActionStateProviderContext make(final ActionStateContext context) throws RepositoryException {
        final PageComposerContextService contextService = context.getContextService();
        return new ActionStateProviderContext()
                .setExperiencePageRequest(contextService.isExperiencePageRequest())
                .setMasterBranchSelected(BranchConstants.MASTER_BRANCH_ID.equals(contextService.getSelectedBranchId()))
                .setUserId(context.getUserId())
                .setBranchId(contextService.getSelectedBranchId())
                .setChannelContext(channelContextFactory.make(context))
                .setPageContext(pageContextFactory.make(context))
                .setXPageContext(xPageContextFactory.make(contextService));
    }

}
