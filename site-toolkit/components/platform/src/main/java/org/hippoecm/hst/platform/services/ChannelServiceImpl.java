/*
*  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

package org.hippoecm.hst.platform.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.beans.InformationObjectsBuilder;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.platform.configuration.hosting.PageModelApiMount;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.PREFER_RENDER_BRANCH_ID;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_ADMIN_PRIVILEGE_NAME;
import static org.onehippo.cms7.services.context.HippoWebappContext.CMS_OR_PLATFORM;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

/**
 * Implementation of {@link ChannelService} for CMS to interact with {@link Channel} resources
 */
public class ChannelServiceImpl implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final HstModelRegistryImpl hstModelRegistry;
    private final PreviewDecorator previewDecorator;

    public ChannelServiceImpl(final HstModelRegistryImpl hstModelRegistry, final PreviewDecorator previewDecorator) {
        this.hstModelRegistry = hstModelRegistry;
        this.previewDecorator = previewDecorator;
    }

    @Override
    public List<Channel> getChannels(final String hostGroup, final boolean branchesIncluded) {
        if (hostGroup == null) {
            throw new IllegalArgumentException("host group is not allowed to be null");
        }

        final List<Channel> channels = new ArrayList<>();

        for (HstModel hstModel : hstModelRegistry.getModels().values()) {
            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();
            if (branchesIncluded) {
                channels.addAll(cloneChannels(virtualHosts.getChannels(hostGroup).values()));
            } else {
                final Set<Channel> masters = virtualHosts.getChannels(hostGroup).values().stream().filter(channel -> channel.getBranchId() == null).collect(Collectors.toSet());
                channels.addAll(cloneChannels(masters));
            }
        }

        return channels;
    }

    @Override
    public Channel getChannel(final String channelId, final String hostGroup) {
        return getChannels(hostGroup, true).stream().filter(channel -> channel.getId().equals(channelId)).findFirst().orElse(null);
    }

    private List<Channel> cloneChannels(final Collection<Channel> channels) {
        final List<Channel> cloned = new ArrayList<>();
        channels.stream().forEach(channel -> cloned.add(new Channel(channel)));
        return cloned;
    }

    @Override
    public List<Channel> getLiveChannels(final Session userSession, final String hostGroup) {
        return doGetChannels(Optional.of(userSession), hostGroup, false);
    }

    @Override
    public List<Channel> getLiveChannels(final String hostGroup) {
        return doGetChannels(Optional.empty(), hostGroup, false);
    }

    @Override
    public List<Channel> getPreviewChannels(final Session userSession, final String hostGroup) {
        return doGetChannels(Optional.of(userSession), hostGroup, true);
    }

    @Override
    public List<Channel> getPreviewChannels(final String hostGroup) {
        return doGetChannels(Optional.empty(), hostGroup, true);
    }

    private List<Channel> doGetChannels(final Optional<Session> userSession, final String hostGroup, final boolean preview) {

        if (hostGroup == null) {
            throw new IllegalArgumentException("host group is not allowed to be null");
        }

        final Map<String, Channel> channels = new HashMap<>();

        final HstRequestContext requestContext = RequestContextProvider.get();
        if (preview && requestContext != null) {
            requestContext.setAttribute(PREFER_RENDER_BRANCH_ID, MASTER_BRANCH_ID);
        }

        for (HstModel hstModel : hstModelRegistry.getModels().values()) {

            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();

            final List<Mount> mountsByHostGroup = virtualHosts.getMountsByHostGroup(hostGroup);
            for (Mount mount : mountsByHostGroup) {

                if (mount.isPreview()) {
                    log.debug("Skipping explicit preview mounts");
                    continue;
                }

                // below bit dirty check. Perhaps cleaner in the future to introduce something like
                // 'Channel#isCanonical()' to indicate whether the Channel object is to be used in channel mgr or
                // channels map or not
                if (mount instanceof PageModelApiMount) {
                    log.debug("Possible present PageModelApiMount channel is already represented by the parent mount");
                    continue;
                }

                final Mount useMount;
                if (preview) {
                    useMount = previewDecorator.decorateMountAsPreview(mount);
                } else {
                    useMount = mount;
                }

                final Channel channel = useMount.getChannel();
                if (channel == null) {
                    log.debug("No channel present for mount '{}'", mount);
                    continue;
                }

                final BiPredicate<Session, Channel> channelFilter = ((InternalHstModel) hstModel).getChannelFilter();

                if (userSession.isPresent()) {
                    if (channelFilter.test(userSession.get(), channel)) {
                        if (channels.containsKey(channel.getId())) {
                            log.error("Found channel with duplicate id. Skipping channel '{}' which has a duplicate id with '{}'",
                                    channel, channels.get(channel.getId()));
                        } else {
                            addClonedChannel(channels, channel);

                        }
                    } else {
                        log.info("Skipping channel '{}' because filtered out by channel filters.", channel.toString());
                    }
                } else {
                    // never return the HST model Channel instances but clone them!!
                    addClonedChannel(channels, channel);
                }
            }
        }

        return channels.values().stream().collect(Collectors.toList());
    }

    private void addClonedChannel(final Map<String, Channel> channels, final Channel channel) {
        // never return the HST model Channel instances but clone them!!
        final Channel clone = new Channel(channel);
        channels.put(channel.getId(), clone);
    }

    @Override
    public String persist(final Session userSession, final String blueprintId, final Channel channel) throws ChannelException {

        // this is the hst model where the channel will be created
        final InternalHstModel hstModel = hstModelRegistry.getInternalHstModel(channel.getContextPath());

        try {
            final boolean isChannelAdmin = userSession.getAccessControlManager().hasPrivileges(hstModel.getConfigurationRootPath(),
                    new Privilege[]{userSession.getAccessControlManager().privilegeFromName(CHANNEL_ADMIN_PRIVILEGE_NAME)});

            if (!isChannelAdmin) {
                throw new ChannelException(String.format("User '%s' is not allowed to create new channel within %s",
                        userSession.getUserID(), hstModel.getConfigurationRootPath()));
            }

            return hstModel.getChannelManager().persist(userSession, blueprintId, channel);
        } catch (RepositoryException e) {
            throw new ChannelException("Error while persisting a new channel - Channel:" , e);
        } catch (ChannelException ce) {
            log.warn("Error while persisting a new channel - Channel: {} - {} : {}", channel, ce.getClass().getName(), ce.toString());
            throw ce;
        }
    }

    @Override
    public Properties getChannelResourceValues(final String hostGroup, final String channelId, final String language) throws ChannelException {
        for (HstModel hstModel : hstModelRegistry.getModels().values()) {
            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();
            final Channel channel = virtualHosts.getChannelById(hostGroup, channelId);
            if (channel != null){
                return InformationObjectsBuilder.buildResourceBundleProperties(virtualHosts.getResourceBundle(channel, new Locale(language)));
            }
        }
        throw new ChannelException(String.format("Cannot find channel for id '%s' and for host group '%s'", channelId, hostGroup));

    }

    @Override
    public Map<String, XPageLayout> getXPageLayouts(final String channelId) {

        for (HstModel hstModel : hstModelRegistry.getModels().values()) {

            HippoWebappContext context = HippoWebappContextRegistry.get().getContext(hstModel.getVirtualHosts().getContextPath());

            if (context == null) {
                continue;
            }
            if (CMS_OR_PLATFORM.contains(context.getType())) {
                continue;
            }

            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();

            // just find the first Mount which have a matching channel id, and create the XPageLayout from that
            // channel (it doesn't matter through which hostgroup the channel is found

            // note that 'channelId' can be for a branch as well!

            final Optional<Map<String, HstComponentConfiguration>> xPageLayoutComponents = virtualHosts.getHostGroupNames().stream()
                    .filter(hostgroupName -> virtualHosts.getChannelById(hostgroupName, channelId) != null)
                    .flatMap(hostgroupName -> virtualHosts.getMountsByHostGroup(hostgroupName).stream()).collect(Collectors.toList())
                    .stream()
                    .filter(mount -> mount.getChannel() != null && channelId.equals(mount.getChannel().getId()))
                    .map(mount -> mount.getHstSite().getComponentsConfiguration().getXPages())
                    .findFirst();

            if (!xPageLayoutComponents.isPresent()) {
                log.info("Cannot find any XPageLayouts for channel '{}'", channelId);
                continue;
            } else {
                return xPageLayoutComponents.get().values().stream()
                        .map(componentConfiguration -> new XPageLayout(componentConfiguration.getId(),
                                componentConfiguration.getLabel(),
                                ((HstComponentConfigurationService)componentConfiguration).getJcrTemplateNode()))
                        .filter(xPageLayout -> xPageLayout.getJcrTemplateNode() != null)
                        .collect(Collectors.toMap(xpageLayout -> xpageLayout.getKey(), xpageLayout -> xpageLayout));
            }
        }

        return Collections.emptyMap();
    }
}
