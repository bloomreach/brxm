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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.onehippo.cms7.services.hst.Channel;

import static java.util.stream.Collectors.toSet;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_CLOSE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DELETE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_DISCARD_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_MANAGE_CHANGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_PUBLISH;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstAction.CHANNEL_SETTINGS;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.xpage;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_ADMIN_PRIVILEGE_NAME;
import static org.hippoecm.hst.util.JcrSessionUtils.isInRole;

public class HstActionProviderImpl implements ActionProvider {

    @Override
    public Set<Action> getActions(final ActionProviderContext context) {
        return Stream.of(
                channelActions(context),
                pageActions(context),
                xPageActions(context)
        ).flatMap(Set::stream).collect(toSet());
    }

    private Set<Action> channelActions(final ActionProviderContext context) {

        final Set<Action> channelAction = new HashSet<>();
        final PageComposerContextService contextService = context.getContextService();

        channelAction.add(CHANNEL_CLOSE.toAction(true));

        final Channel previewChannel = contextService.getEditingPreviewChannel();
        final boolean master = previewChannel.getBranchOf() == null;
        final boolean channelAdmin = isChannelAdmin(context);
        final String userId = getUserId(context);
        channelAction.add(CHANNEL_DELETE.toAction(channelAdmin && previewChannel.isDeletable() && !previewChannel.isConfigurationLocked()));
        channelAction.add(CHANNEL_DISCARD_CHANGES.toAction(previewChannel.getChangedBySet().contains(userId)));
        channelAction.add(CHANNEL_MANAGE_CHANGES.toAction(channelAdmin && !previewChannel.isConfigurationLocked()));
        channelAction.add(CHANNEL_PUBLISH.toAction(master && !previewChannel.getChangedBySet().isEmpty()));
        channelAction.add(CHANNEL_SETTINGS.toAction(previewChannel.getHasCustomProperties()));

        return channelAction;
    }

    private Set<Action> xPageActions(final ActionProviderContext context) {
        final PageComposerContextService contextService = context.getContextService();
        return contextService.isExperiencePageRequest()
                ? HstAction.actions(xpage()).map(hstAction -> hstAction.toAction(true)).collect(toSet())
                : Collections.emptySet();
    }

    private Set<Action> pageActions(final ActionProviderContext context) {
        final PageComposerContextService contextService = context.getContextService();
        return contextService.getEditingPreviewChannel().isConfigurationLocked()
                || contextService.isExperiencePageRequest()
                ? Collections.emptySet()
                : getPageActions(context);
    }

    private Set<Action> getPageActions(final ActionProviderContext context) {
        final Channel previewChannel = context.getContextService().getEditingPreviewChannel();
        final Collection<HstComponentConfiguration> prototypePages = context.getContextService().getEditingPreviewSite().getComponentsConfiguration().getPrototypePages().values();
        final Set<Action> actions = new HashSet<>();
        actions.add(HstAction.PAGE_COPY.toAction(true));
        actions.add(HstAction.PAGE_DELETE.toAction(true));
        actions.add(HstAction.PAGE_MOVE.toAction(true));
        actions.add(HstAction.PAGE_NEW.toAction(
                previewChannel.isWorkspaceExists()
                        && prototypePages.stream().anyMatch(cfg -> !cfg.isInherited())));
        actions.add(HstAction.PAGE_PROPERTIES.toAction(true));
        actions.add(HstAction.PAGE_TOOLS.toAction(true));
        return actions;
    }

    private boolean isChannelAdmin(final ActionProviderContext context) {
        final PageComposerContextService contextService = context.getContextService();
        try {
            final Session session = contextService.getRequestContext().getSession(false);
            final boolean inRoleLive = isInRole(session, contextService.getEditingLiveConfigurationPath(), CHANNEL_ADMIN_PRIVILEGE_NAME);
            if (contextService.hasPreviewConfiguration()) {
                return inRoleLive && isInRole(session, contextService.getEditingPreviewConfigurationPath(), CHANNEL_ADMIN_PRIVILEGE_NAME);
            }
            return inRoleLive;
        } catch (RepositoryException e) {
            return false;
        }
    }

    private String getUserId(ActionProviderContext context) {
        try {
            return context.getContextService().getRequestContext().getSession(false).getUserID();
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }


}
