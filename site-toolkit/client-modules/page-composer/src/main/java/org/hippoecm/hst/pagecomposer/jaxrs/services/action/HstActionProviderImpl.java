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

import java.util.Set;
import java.util.stream.Stream;

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;

import static java.util.stream.Collectors.toSet;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.channel;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.page;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.xpage;

public class HstActionProviderImpl implements ActionProvider {

    @Override
    public Set<Action> getActions(final ActionProviderContext context) {
        final Stream<HstAction> channelActions = HstAction.actions(channel());
        final Stream<HstAction> pageActions = pageActions(context.getContextService());
        final Stream<HstAction> xpageActions = xPageActions(context.getContextService());
        return Stream.concat(channelActions, Stream.concat(pageActions, xpageActions))
                .map(hstAction -> hstAction.toAction(true))
                .collect(toSet());
    }

    private Stream<HstAction> xPageActions(final PageComposerContextService contextService) {
        return contextService.isExperiencePageRequest()
                ? HstAction.actions(xpage())
                : Stream.empty();
    }

    private Stream<HstAction> pageActions(final PageComposerContextService contextService) {
        return contextService.getEditingPreviewChannel().isConfigurationLocked()
                ? Stream.empty()
                : HstAction.actions(page());
    }
}
