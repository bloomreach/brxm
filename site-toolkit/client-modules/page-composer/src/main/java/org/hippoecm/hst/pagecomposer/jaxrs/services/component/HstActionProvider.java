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
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.ScheduledRequest;

import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_CLOSE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_DISCARD_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_MANAGE_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.HstAction.CHANNEL_SETTINGS;

public final class HstActionProvider {

    public Map<NamedCategory, Boolean> getActions(final ActionStateProviderContext context) {
        final Map<NamedCategory, Boolean> actions = new HashMap<>();
        actions.putAll(channelActions(context));
        actions.putAll(pageActions(context));
        actions.putAll(xPageActions(context));
        return actions;
    }

    private Map<NamedCategory, Boolean> channelActions(final ActionStateProviderContext actionProviderContext) {

        final ChannelContext context = actionProviderContext.getChannelContext();
        final Map<NamedCategory, Boolean> channelAction = new HashMap<>();

        channelAction.put(CHANNEL_CLOSE, true);

        channelAction.put(CHANNEL_DELETE,
                context.isChannelAdmin()
                        && context.isDeletable()
                        && !context.isConfigurationLocked());

        channelAction.put(CHANNEL_DISCARD_CHANGES,
                context.getChangedBySet().contains(actionProviderContext.getUserId()));

        channelAction.put(CHANNEL_MANAGE_CHANGES,
                context.isChannelAdmin()
                        && !context.getChangedBySet().isEmpty()
                        && !context.isConfigurationLocked());

        if (actionProviderContext.isMasterBranchSelected()) {
            channelAction.put(CHANNEL_PUBLISH,
                    context.getChangedBySet().contains(actionProviderContext.getUserId()));
        }

        channelAction.put(CHANNEL_SETTINGS,
                context.hasCustomProperties());

        return channelAction;
    }

    private Map<NamedCategory, Boolean> xPageActions(final ActionStateProviderContext context) {
        return context.isExperiencePageRequest()
                ? getXPageActions(context)
                : Collections.emptyMap();
    }

    private Map<NamedCategory, Boolean> getXPageActions(final ActionStateProviderContext context) {
        final Map<NamedCategory, Boolean> actions = new HashMap<>();
        final XPageContext xPageContext = context.getXPageContext();

        xPageContext.isPublishable().ifPresent(publishable -> {
            actions.put(HstAction.XPAGE_PUBLISH, publishable);
            actions.put(HstAction.XPAGE_SCHEDULE_PUBLICATION, publishable);
        });

        xPageContext.isUnpublishable().ifPresent(unpublishable -> {
            actions.put(HstAction.XPAGE_UNPUBLISH, unpublishable);
            actions.put(HstAction.XPAGE_SCHEDULE_UNPUBLICATION, unpublishable);
        });

        xPageContext.isRequestPublication().ifPresent(requestPublication -> {
            actions.put(HstAction.XPAGE_REQUEST_PUBLICATION, requestPublication);
            actions.put(HstAction.XPAGE_REQUEST_SCHEDULE_PUBLICATION, requestPublication);
        });

        xPageContext.isRequestDepublication().ifPresent(requestDepublication -> {
            actions.put(HstAction.XPAGE_REQUEST_UNPUBLICATION, requestDepublication);
            actions.put(HstAction.XPAGE_REQUEST_SCHEDULE_UNPUBLICATION, requestDepublication);
        });

        xPageContext.isAcceptRequest().ifPresent(acceptRequest -> actions.put(HstAction.XPAGE_REQUEST_ACCEPT
                , acceptRequest));

        xPageContext.isCancelRequest().ifPresent(cancelRequest -> actions.put(HstAction.XPAGE_REQUEST_CANCEL
                , cancelRequest));

        xPageContext.isRejectRequest().ifPresent(rejectRequest -> actions.put(HstAction.XPAGE_REQUEST_REJECT
                , rejectRequest));

        xPageContext.isRejectedRequest().ifPresent(rejectedRequest -> actions.put(HstAction.XPAGE_REQUEST_REJECTED
                , rejectedRequest));

        actions.put(HstAction.XPAGE_RENAME, xPageContext.isRenameAllowed());
        actions.put(HstAction.XPAGE_COPY, xPageContext.isCopyAllowed());
        actions.put(HstAction.XPAGE_MOVE, xPageContext.isMoveAllowed());
        actions.put(HstAction.XPAGE_DELETE, xPageContext.isDeleteAllowed());

        final ScheduledRequest scheduledRequest = xPageContext.getScheduledRequest();
        if (scheduledRequest != null) {
            switch (scheduledRequest.getType()) {
                case "publish":
                    actions.put(HstAction.XPAGE_CANCEL_SCHEDULED_PUBLICATION, true);
                    break;
                case "depublish":
                    actions.put(HstAction.XPAGE_CANCEL_SCHEDULED_DEPUBLICATION, true);
                    break;
                default:
                    throw new IllegalArgumentException(scheduledRequest.getType());
            }
        }

        return actions;
    }

    private Map<NamedCategory, Boolean> pageActions(final ActionStateProviderContext context) {
        return   !context.getChannelContext().isChannelWebmaster()
                || context.getChannelContext().isConfigurationLocked()
                || context.isExperiencePageRequest()
                ? Collections.emptyMap()
                : getPageActions(context);
    }

    private Map<NamedCategory, Boolean> getPageActions(final ActionStateProviderContext context) {

        final Map<NamedCategory, Boolean> actions = new HashMap<>();
        final PageContext pageContext = context.getPageContext();
        final ChannelContext channelContext = context.getChannelContext();

        actions.put(HstAction.PAGE_COPY,
                !pageContext.isLocked()
                        && (channelContext.hasWorkspace()
                        || channelContext.isCrossChannelPageCopySupported())
        );

        actions.put(HstAction.PAGE_DELETE,
                !pageContext.isHomePage()
                        && !pageContext.isLocked()
                        && !pageContext.isInherited()
                        && pageContext.isWorkspaceConfigured()
        );

        actions.put(HstAction.PAGE_MOVE,
                !pageContext.isHomePage()
                        && pageContext.isWorkspaceConfigured()
                        && !pageContext.isInherited()
                        && !pageContext.isLocked()
        );

        actions.put(HstAction.PAGE_NEW,
                channelContext.hasWorkspace() && !channelContext.hasPrototypes());

        actions.put(HstAction.PAGE_PROPERTIES,
                !pageContext.isHomePage()
                        && !pageContext.isLocked()
                        && !pageContext.isInherited()
                        && pageContext.isWorkspaceConfigured()
        );

        return actions;
    }

}
