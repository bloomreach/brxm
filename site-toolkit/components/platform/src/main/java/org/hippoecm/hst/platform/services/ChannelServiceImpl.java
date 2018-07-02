/*
*  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.BiPredicate;

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
    private HstModelRegistryImpl hstModelRegistry;
    private PreviewDecorator previewDecorator;

    public ChannelServiceImpl(final HstModelRegistryImpl hstModelRegistry, final PreviewDecorator previewDecorator) {
        this.hstModelRegistry = hstModelRegistry;
        this.previewDecorator = previewDecorator;
    }

    @Override
    public List<Channel> getChannels(final Session userSession, final String cmsHost) {
        final List<Channel> channels = new ArrayList<>();

        for (HstModel hstModel : hstModelRegistry.getModels().values()) {

            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();

            String hostGroupNameForCmsHost = ResourceUtil.getHostGroupNameForCmsHost(virtualHosts, cmsHost);

            if (hostGroupNameForCmsHost == null) {
                log.info("Cannot match cms host '{}' for hst virtualhosts for context path '{}'", cmsHost, virtualHosts.getContextPath());
                continue;
            }
            final List<Mount> mountsByHostGroup = virtualHosts.getMountsByHostGroup(hostGroupNameForCmsHost);
            for (Mount mount : mountsByHostGroup) {

                final Mount previewMount = previewDecorator.decorateMountAsPreview(mount);

                final Channel channel = previewMount.getChannel();
                if (channel == null) {
                    log.debug("No channel present for mount '{}'", mount);
                    continue;
                }

                final BiPredicate<Session, Channel> channelFilter = ((PlatformHstModel) hstModel).getChannelFilter();

                if (channelFilter.test(userSession, channel)) {
                    channels.add(channel);
                } else {
                    log.info("Skipping channel '{}' because filtered out by channel filters.", channel.toString());
                }
            }
        }

        return channels;
    }

    @Override
    public String persist(final Session userSession, final String blueprintId, final Channel channel) throws ChannelException {

        final PlatformHstModel hstModel = hstModelRegistry.getPlatformHstModel(channel.getContextPath());

        try {
            return hstModel.getChannelManager().persist(blueprintId, channel);
        } catch (ChannelException ce) {
            log.warn("Error while persisting a new channel - Channel: {} - {} : {}", channel, ce.getClass().getName(), ce.toString());
            throw ce;
        }
    }

    @Override
    public boolean canUserModifyChannels(final Session userSession) {
        return true;
        // TODO HSTTWO-4359  FIX this check
        // return channelManager.canUserModifyChannels();
    }

    @Override
    public Properties getChannelResourceValues(final String cmsHost, final String channelId, final String language) throws ChannelException {
        for (HstModel hstModel : hstModelRegistry.getModels().values()) {
            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();
            final Channel channel = virtualHosts.getChannelById(cmsHost, channelId);
            if (channel != null){
                return InformationObjectsBuilder.buildResourceBundleProperties(virtualHosts.getResourceBundle(channel, new Locale(language)));
            }
        }
        throw new ChannelException(String.format("Cannot find channel for id '%s' and for cms host '%s'", channelId, cmsHost));

    }

}
