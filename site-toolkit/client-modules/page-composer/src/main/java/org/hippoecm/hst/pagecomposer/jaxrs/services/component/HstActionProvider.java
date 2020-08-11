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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_CLOSE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_DISCARD_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_MANAGE_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_SETTINGS;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_MOVE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.XPAGE_NEW;

public final class HstActionProvider {

    public Set<Action> getActions(final ActionStateProviderContext context) {
        return Stream.of(
                channelActions(context),
                pageActions(context),
                xPageActions(context)
        ).flatMap(Set::stream).collect(toSet());
    }

    private Set<Action> channelActions(ActionStateProviderContext actionProviderContext) {

        final ChannelContext context = actionProviderContext.getChannelContext();
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

    private Set<Action> xPageActions(ActionStateProviderContext context) {
        return context.isExperiencePageRequest()
                ? getXPageActions()
                : Collections.emptySet();
    }

    private Set<Action> getXPageActions() {
        // TODO (meggermont): decide which actions we really need
        return Stream.of(HstAction.XPAGE_DELETE, XPAGE_MOVE, XPAGE_NEW)
                .map(action -> action.toAction(false))
                .collect(toSet());
    }

    private Set<Action> pageActions(ActionStateProviderContext context) {
        return context.getChannelContext().isConfigurationLocked()
                || context.isExperiencePageRequest()
                ? Collections.emptySet()
                : getPageActions(context);
    }

    private Set<Action> getPageActions(ActionStateProviderContext context) {

        final Set<Action> actions = new HashSet<>();
        final PageContext pageContext = context.getPageContext();
        final ChannelContext channelContext = context.getChannelContext();

        actions.add(HstAction.PAGE_COPY.toAction(
                !pageContext.isLocked()
                        && (channelContext.hasWorkspace()
                        || channelContext.isCrossChannelPageCopySupported())));

        actions.add(HstAction.PAGE_DELETE.toAction(
                !pageContext.isHomePage()
                        && !pageContext.isLocked()
                        && !pageContext.isInherited()
                        && pageContext.isWorkspaceConfigured()
        ));

        actions.add(HstAction.PAGE_MOVE.toAction(
                !pageContext.isHomePage()
                        && pageContext.isWorkspaceConfigured()
                        && !pageContext.isInherited()
                        && !pageContext.isLocked()
        ));

        actions.add(HstAction.PAGE_NEW.toAction(
                channelContext.hasWorkspace()
                        && !channelContext.hasPrototypes()));

        actions.add(HstAction.PAGE_PROPERTIES.toAction(
                !pageContext.isHomePage()
                        && !pageContext.isLocked()
                        && !pageContext.isInherited()
                        && pageContext.isWorkspaceConfigured()));

        return actions;
    }

}
