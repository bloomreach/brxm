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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.beans.InformationObjectsBuilder;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;
import org.hippoecm.hst.site.request.MountDecoratorImpl;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ChannelService} for CMS to interact with {@link Channel} resources
 */
public class ChannelServiceImpl implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelServiceImpl.class);
    private HstModelRegistryImpl hstModelRegistry;

    public ChannelServiceImpl(final HstModelRegistryImpl hstModelRegistry) {
        this.hstModelRegistry = hstModelRegistry;
    }


    @Override
    public List<Channel> getChannels(final String cmsHost) {
        final List<Channel> channels = new ArrayList<>();

        // TODO move the mount decorator impl to platform
        final MountDecoratorImpl mountDecorator = new MountDecoratorImpl();

        for (HstModel hstModel : hstModelRegistry.getModels().values()) {

            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();

            String hostGroupNameForCmsHost = ResourceUtil.getHostGroupNameForCmsHost(virtualHosts, cmsHost);

            if (hostGroupNameForCmsHost == null) {
                log.info("Cannot match cms host '{}' for hst virtualhosts for context path '{}'", cmsHost, virtualHosts.getContextPath());
                continue;
            }
            final List<Mount> mountsByHostGroup = virtualHosts.getMountsByHostGroup(hostGroupNameForCmsHost);
            for (Mount mount : mountsByHostGroup) {

                final Mount previewMount = mountDecorator.decorateMountAsPreview(mount);

                final Channel channel = previewMount.getChannel();
                if (channel == null) {
                    log.debug("No channel present for mount '{}'", mount);
                    continue;
                }
                // TODO HSTTWO-4359 we need to come up with an alternative for 'channelFilter' Most likely we can just
                // TODO plugin channelFilter BUT it cannot use the HstRequestContext since we do not have one anymore
//                if (channelFilter.apply(channel)) {
//                    log.debug("Including channel '{}' because passes filters.", channel.toString());
//                    channels.add(channel);
//                } else {
//                    log.info("Skipping channel '{}' because filtered out by channel filters.", channel.toString());
//                }
                channels.add(channel);
            }
        }

        return channels;
    }

    @Override
    public String persist(final Session userSession, final String blueprintId, final Channel channel) throws ChannelException {
        // TODO HSTTWO-4359  FIX persist
//        try {
//            return channelManager.persist(blueprintId, channel);
//        } catch (ChannelException ce) {
//            log.warn("Error while persisting a new channel - Channel: {} - {} : {}", channel, ce.getClass().getName(), ce.toString());
//            throw ce;
//        }
        return null;
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
