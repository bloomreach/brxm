/*
*  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.beans.InformationObjectsBuilder;
import org.hippoecm.hst.platform.api.model.PlatformHstModel;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public List<Channel> getLiveChannels(final Session userSession, final String hostGroup) {
        return doGetChannels(userSession, hostGroup, false);
    }

    @Override
    public List<Channel> getPreviewChannels(final Session userSession, final String hostGroup) {
        return doGetChannels(userSession, hostGroup, true);
    }

    private List<Channel> doGetChannels(final Session userSession, final String hostGroup, final boolean preview) {

        if (hostGroup == null || userSession == null) {
            throw new IllegalArgumentException("User session and host group are not allowed to be null");
        }

        final Map<String, Channel> channels = new HashMap<>();

        for (HstModel hstModel : hstModelRegistry.getModels().values()) {

            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();

            final List<Mount> mountsByHostGroup = virtualHosts.getMountsByHostGroup(hostGroup);
            for (Mount mount : mountsByHostGroup) {

                if (mount.isPreview()) {
                    log.debug("Skipping explicit preview mounts");
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

                final BiPredicate<Session, Channel> channelFilter = ((PlatformHstModel) hstModel).getChannelFilter();

                if (channelFilter.test(userSession, channel)) {
                    if (channels.containsKey(channel.getId())) {
                        log.error("Found channel with duplicate id. Skipping channel '{}' which has a duplicate id with '{}'",
                                channel, channels.get(channel.getId()));
                    } else {
                        // never return the HST model Channel instances but clone them!!
                        final Channel clone = new Channel(channel);
                        channels.put(channel.getId(), clone);
                    }
                } else {
                    log.info("Skipping channel '{}' because filtered out by channel filters.", channel.toString());
                }
            }
        }

        return channels.values().stream().collect(Collectors.toList());
    }

    @Override
    public String persist(final Session userSession, final String blueprintId, final Channel channel) throws ChannelException {

        final PlatformHstModel hstModel = hstModelRegistry.getPlatformHstModel(channel.getContextPath());

        try {
            return hstModel.getChannelManager().persist(userSession, blueprintId, channel);
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

}
