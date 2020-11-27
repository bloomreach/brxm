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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ChannelService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onehippo.cms7.services.hst.Channel;

import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_ADMIN_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_WEBMASTER_PRIVILEGE_NAME;
import static org.hippoecm.hst.util.JcrSessionUtils.isInRole;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_CHANNEL_ID;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_FOLDERTYPE;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;
import static org.hippoecm.repository.HippoStdNodeType.NT_CM_XPAGE_FOLDER;
import static org.hippoecm.repository.HippoStdNodeType.NT_XPAGE_FOLDER;

final class ChannelContextFactory implements ComponentManagerAware {

    private static final Logger log = LoggerFactory.getLogger(ChannelContextFactory.class);

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
                // in the context of the page composer, actionContext.getContextService().getEditingMount()
                // returns the correct (preview) Mount taking branches into account
                .setXPageLayouts(channelService.getXPageLayouts(actionContext.getContextService().getEditingMount()))
                .setXPageTemplateQueries(
                        getXPageTemplateQueries(channel, channel.getContentRoot(), session))
                .setConfigurationLocked(channel.isConfigurationLocked());
    }

    /**
     *
     * @param channel the current Channel, which can be also the channel for a branch
     */
    private Map<String, String> getXPageTemplateQueries(final Channel channel, final String contentRootPath,
                                                        final Session session) throws RepositoryException {
        final String branchOf = channel.getBranchOf();

        // if branchOf not null, the value branchOf points to the (live) master channel id
        final String masterChannelId = branchOf == null ? channel.getId() : branchOf;
        // if -preview is not found, we already have the live channel id (substringBefore returns same string if -preview not found)
        final String masterLiveChannelId = substringBefore(masterChannelId, "-preview");

        final NodeIterator cmXPageFolders = queryCmXPageFolders(channelId, contentRootPath, session);
        if (cmXPageFolders.getSize() > 0) {
            final Node cmXPageFolder = cmXPageFolders.nextNode();
            final List<String> additionalCmXPageFolderPaths = new ArrayList<>();
            while (cmXPageFolders.hasNext()) {
                additionalCmXPageFolderPaths.add(cmXPageFolders.nextNode().getPath());
            }
            if (!additionalCmXPageFolderPaths.isEmpty()) {
                // At the moment only one node per channel in the contentRoot may have this mixin.
                // This requirement can be removed later if the UI supports multiple cm xpage folders.
                log.warn("Root xpage folder for channel {} not unique, using '{}'. Additional root xpage folder paths: {}",
                        masterLiveChannelId, cmXPageFolder.getPath(), additionalCmXPageFolderPaths);
            }
            return getTemplateQueryMap(cmXPageFolder);
        }
        final Node contentRoot = session.getNode(contentRootPath);
        for (Node child : new NodeIterable(contentRoot.getNodes())) {
            if (child.isNodeType(NT_XPAGE_FOLDER)) {
                final String channelIdProperty = JcrUtils.getStringProperty(child, HIPPOSTD_CHANNEL_ID, null);
                if (masterLiveChannelId.equals(channelIdProperty)) {
                    return getTemplateQueryMap(child);
                }
            }
        }
        return Collections.emptyMap();
    }

    private NodeIterator queryCmXPageFolders(String channelId, String contentRootPath, Session session) throws RepositoryException {
        final String statement = String.format(
                "/%s//element(*, %s)[@jcr:mixinTypes='%s', @%s='%s', @%s]",
                contentRootPath, NT_FOLDER, NT_CM_XPAGE_FOLDER, HIPPOSTD_CHANNEL_ID, channelId, HIPPOSTD_FOLDERTYPE);
        return session.getWorkspace().getQueryManager()
                .createQuery(statement, Query.XPATH)
                .execute()
                .getNodes();
    }

    private Map<String, String> getTemplateQueryMap(final Node xPageRootFolderNode) throws RepositoryException {
        final String[] folderTypes = JcrUtils.getMultipleStringProperty(xPageRootFolderNode, HIPPOSTD_FOLDERTYPE, new String[0]);
        final Optional<String> folderType = Stream.of(folderTypes)
                .filter(t -> StringUtils.endsWith(t, "-document"))
                .findFirst();
        if (folderType.isPresent()) {
            return Collections.singletonMap(folderType.get(), xPageRootFolderNode.getPath());
        } else {
            return Collections.emptyMap();
        }
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
