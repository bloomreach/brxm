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
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ChannelService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.hst.Channel;

import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_ADMIN_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_WEBMASTER_PRIVILEGE_NAME;
import static org.hippoecm.hst.util.JcrSessionUtils.isInRole;

final class ChannelContextFactory implements ComponentManagerAware {

    private final ChannelService channelService;
    private boolean crossChannelPageCopySupported;

    ChannelContextFactory(final ChannelService channelService) {
        this.channelService = channelService;
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        crossChannelPageCopySupported = componentManager.getContainerConfiguration()
                .getBoolean("cross.channel.page.copy.supported", false);
    }

    ChannelContext make(ActionStateContext actionContext) throws RepositoryException {
        final PageComposerContextService contextService = actionContext.getContextService();
        final ChannelContext channelContext = new ChannelContext()
                .setChannelAdmin(isChannelAdmin(contextService))
                .setChannelWebmaster(isWebmaster(contextService))
                .setCrossChannelPageCopySupported(crossChannelPageCopySupported)
                .setHasPrototypes(!hasPrototypes(contextService));

        final Optional<Channel> channelOptional = channelService.getChannelByMountId(actionContext.getMountId(),
                                                                                     actionContext.getHostGroup());
        if (!channelOptional.isPresent()) {
            return channelContext;
        }

        final Channel channel = channelOptional.get();
        final Session session = contextService.getRequestContext().getSession();
        return channelContext
                .setChannel(channel)
                .setDeletable(channelService.canChannelBeDeleted(channel) && channelService.isMaster(channel))
                .setXPageLayouts(channelService.getXPageLayouts(channelContext.getChannelId()))
                .setXPageTemplateQueries(
                        getXPageTemplateQueries(channelContext.getChannelId(), channel.getContentRoot(), session))
                .setConfigurationLocked(channel.isConfigurationLocked());
    }


    private Map<String, String> getXPageTemplateQueries(final String channelId, final String contentRootPath,
                                                        final Session session) throws RepositoryException {
        final Node contentRoot = session.getNode(contentRootPath);
        final NodeIterator nodes = contentRoot.getNodes();
        while (nodes.hasNext()) {
            final Node child = nodes.nextNode();
            if (!child.isNodeType(HippoStdNodeType.NT_XPAGE_FOLDER)) {
                continue;
            }

            final Property channelIdProperty = JcrUtils.getPropertyIfExists(child,
                    HippoStdNodeType.HIPPOSTD_CHANNEL_ID);
            if (channelIdProperty == null || !channelId.equals(channelIdProperty.getString())) {
                continue;
            }

            final Property folderType = JcrUtils.getPropertyIfExists(child, HippoStdNodeType.HIPPOSTD_FOLDERTYPE);
            if (folderType == null) {
                continue;
            }

            for (final Value folderTypeValue : folderType.getValues()) {
                final String folderTypeString = folderTypeValue.getString();
                if (StringUtils.endsWith(folderTypeString, "-document")) {
                    return Collections.singletonMap(folderTypeString, child.getPath());
                }
            }
        }

        return Collections.emptyMap();
    }

    private boolean hasPrototypes(final PageComposerContextService contextService) {
        return !contextService.getEditingMount().getHstSite().getComponentsConfiguration().getPrototypePages().values().isEmpty();
    }

    private boolean isChannelAdmin(PageComposerContextService contextService) {
        return doIsInRole(contextService, CHANNEL_ADMIN_PRIVILEGE_NAME);
    }


    private boolean isWebmaster(final PageComposerContextService contextService) {
        return doIsInRole(contextService, CHANNEL_WEBMASTER_PRIVILEGE_NAME);
    }

    public boolean doIsInRole(PageComposerContextService contextService, final String inRole) {
        try {
            final Session session = contextService.getRequestContext().getSession(false);
            final boolean inRoleLive = isInRole(session, contextService.getEditingLiveConfigurationPath(), inRole);
            if (contextService.hasPreviewConfiguration()) {
                return inRoleLive && isInRole(session, contextService.getEditingPreviewConfigurationPath(), inRole);
            }
            return inRoleLive;
        } catch (RepositoryException e) {
            return false;
        }
    }

}
