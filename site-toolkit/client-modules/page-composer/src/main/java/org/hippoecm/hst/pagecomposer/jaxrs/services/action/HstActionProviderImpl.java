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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_CLOSE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DISCARD_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_MANAGE_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_SETTINGS;

public class HstActionProviderImpl implements ActionProvider {

    @Override
    public Set<Action> getActions(final ActionProviderContext context) {
        return Stream.of(
                channelActions(context),
                pageActions(context),
                xPageActions(context)
        ).flatMap(Set::stream).collect(toSet());
    }

    private Set<Action> channelActions(ActionProviderContext actionProviderContext) {

        final ChannelActionContext context = actionProviderContext.getChannelActionContext();
        final Set<Action> channelAction = new HashSet<>();

        channelAction.add(CHANNEL_CLOSE.toAction(true));

        channelAction.add(CHANNEL_DELETE.toAction(
                context.isChannelAdmin()
                        && context.isDeletable()
                        && !context.isConfigurationLocked()));

        channelAction.add(CHANNEL_DISCARD_CHANGES.toAction(
                context.getChangedBySet().contains(actionProviderContext.getUserId())));

        channelAction.add(CHANNEL_MANAGE_CHANGES.toAction(
                context.isChannelAdmin()
                        && !context.getChangedBySet().isEmpty()
                        && !context.isConfigurationLocked()));

        if (actionProviderContext.isMasterBranchSelected()) {
            channelAction.add(CHANNEL_PUBLISH.toAction(
                    context.getChangedBySet().contains(actionProviderContext.getUserId())));
        }

        channelAction.add(CHANNEL_SETTINGS.toAction(
                context.hasCustomProperties()));

        return channelAction;
    }

    private Set<Action> xPageActions(final ActionProviderContext context) {
        return context.isExperiencePageRequest()
                ? getXPageActions(context)
                : Collections.emptySet();
    }

    private Set<Action> getXPageActions(final ActionProviderContext context) {
        final Set<Action> actions = new HashSet<>();
        final XPageActionContext xpageActionContext = context.getXPageActionContext();

        if (xpageActionContext.isPublishable() != null) {
            actions.add(HstAction.XPAGE_PUBLISH.toAction(xpageActionContext.isPublishable()));
            actions.add(HstAction.XPAGE_SCHEDULE_PUBLICATION.toAction(xpageActionContext.isPublishable()));
        } else if (xpageActionContext.isRequestPublication() != null) {
            actions.add(HstAction.XPAGE_REQUEST_PUBLICATION.toAction(xpageActionContext.isRequestPublication()));
            actions.add(HstAction.XPAGE_REQUEST_SCHEDULE_PUBLICATION.toAction(xpageActionContext.isRequestPublication()));
        }

        if (xpageActionContext.isUnpublishable() != null) {
            actions.add(HstAction.XPAGE_UNPUBLISH.toAction(xpageActionContext.isUnpublishable()));
            actions.add(HstAction.XPAGE_SCHEDULE_UNPUBLICATION.toAction(xpageActionContext.isUnpublishable()));
        } else if (xpageActionContext.isRequestDepublication() != null) {
            actions.add(HstAction.XPAGE_REQUEST_UNPUBLICATION.toAction(xpageActionContext.isRequestDepublication()));
            actions.add(HstAction.XPAGE_REQUEST_SCHEDULE_UNPUBLICATION.toAction(xpageActionContext.isRequestDepublication()));
        }

        actions.add(HstAction.XPAGE_COPY.toAction(xpageActionContext.isCopyAllowed()));
        actions.add(HstAction.XPAGE_MOVE.toAction(xpageActionContext.isMoveAllowed()));
        actions.add(HstAction.XPAGE_DELETE.toAction(xpageActionContext.isDeleteAllowed()));

        return actions;
    }

    private Set<Action> pageActions(ActionProviderContext context) {
        return context.getChannelActionContext().isConfigurationLocked()
                || context.isExperiencePageRequest()
                ? Collections.emptySet()
                : getPageActions(context);
    }

    private Set<Action> getPageActions(ActionProviderContext context) {

        final Set<Action> actions = new HashSet<>();
        final PageActionContext pageActionContext = context.getPageActionContext();
        final ChannelActionContext channelActionContext = context.getChannelActionContext();

        actions.add(HstAction.PAGE_COPY.toAction(
                !pageActionContext.isLocked()
                        && (channelActionContext.hasWorkspace()
                        || channelActionContext.isCrossChannelPageCopySupported())));

        actions.add(HstAction.PAGE_DELETE.toAction(
                !pageActionContext.isHomePage()
                        && !pageActionContext.isLocked()
                        && !pageActionContext.isInherited()
                        && pageActionContext.isWorkspaceConfigured()
        ));

        actions.add(HstAction.PAGE_MOVE.toAction(
                !pageActionContext.isHomePage()
                        && pageActionContext.isWorkspaceConfigured()
                        && !pageActionContext.isInherited()
                        && !pageActionContext.isLocked()
        ));

        actions.add(HstAction.PAGE_NEW.toAction(
                channelActionContext.hasWorkspace()
                        && !channelActionContext.hasPrototypes()));

        actions.add(HstAction.PAGE_PROPERTIES.toAction(
                !pageActionContext.isHomePage()
                        && !pageActionContext.isLocked()
                        && !pageActionContext.isInherited()
                        && pageActionContext.isWorkspaceConfigured()));

        return actions;
    }

}
