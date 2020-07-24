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
import javax.jcr.Session;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ChannelService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;

import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_ADMIN_PRIVILEGE_NAME;
import static org.hippoecm.hst.util.JcrSessionUtils.isInRole;

public class ChannelActionContextFactory implements ComponentManagerAware {

    private final ChannelService channelService;
    private boolean crossChannelPageCopySupported;

    public ChannelActionContextFactory(final ChannelService channelService) {
        this.channelService = channelService;
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        crossChannelPageCopySupported = componentManager.getContainerConfiguration()
                .getBoolean("cross.channel.page.copy.supported", false);
    }

    ChannelActionContext make(ActionContext actionContext) {
        final PageComposerContextService contextService = actionContext.getContextService();
        final ChannelActionContext channelActionContext = new ChannelActionContext()
                .setChannelAdmin(isChannelAdmin(contextService))
                .setCrossChannelPageCopySupported(crossChannelPageCopySupported)
                .setHasPrototypes(!hasPrototypes(contextService));
        channelService.getChannelByMountId(actionContext.getMountId(), actionContext.getHostGroup())
                .ifPresent(channel -> channelActionContext
                        .setChannel(channel)
                        .setDeletable(channelService.canChannelBeDeleted(channel) && channelService.isMaster(channel))
                        .setConfigurationLocked(channel.isConfigurationLocked()));
        return channelActionContext;
    }

    private boolean hasPrototypes(final PageComposerContextService contextService) {
        return !contextService.getEditingMount().getHstSite().getComponentsConfiguration().getPrototypePages().values().isEmpty();
    }

    private boolean isChannelAdmin(PageComposerContextService contextService) {
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

}
