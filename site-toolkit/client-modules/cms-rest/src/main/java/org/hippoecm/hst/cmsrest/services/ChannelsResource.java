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

package org.hippoecm.hst.cmsrest.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.rest.ChannelService;
import org.hippoecm.hst.rest.beans.ChannelDataset;
import org.hippoecm.hst.rest.beans.InformationObjectsBuilder;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ChannelService} for CMS to interact with {@link Channel} resources
 */
public class ChannelsResource extends BaseResource implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelsResource.class);

    @Override
    public ChannelDataset getChannels() {
        final ChannelDataset dataset = new ChannelDataset();
        final List<Channel> channels = new ArrayList<>();
        // do not use HstServices.getComponentManager().getComponent(HstManager.class.getName()) to get to
        // virtualhosts object since we REALLY need the hst model instance for the current request!!

        final List<Mount> mountsForHostGroup = getVirtualHosts().getMountsByHostGroup(getHostGroupNameForCmsHost());
        for (Mount mount : mountsForHostGroup) {
            if (!Mount.PREVIEW_NAME.equals(mount.getType())) {
                log.debug("Skipping non preview mount '{}'. This can be for example the 'composer' auto augmented mount.",
                        mount.toString());
                continue;
            }
            String requestContextPath = RequestContextProvider.get().getServletRequest().getContextPath();
            if (mount.getContextPath() == null || !mount.getContextPath().equals(requestContextPath)) {
                log.debug("Skipping mount '{}' because it can only be rendered for webapp '{}' and not for webapp '{}'",
                        mount.toString(), mount.getContextPath(), requestContextPath);
                continue;
            }
            final Channel channel = mount.getChannel();
            if (channel == null) {
                log.debug("Skipping link for mount '{}' since it does not have a channel", mount.getName());
                continue;
            }
            if (channelFilter.apply(channel)) {
                log.debug("Including channel '{}' because passes filters.", channel.toString());
                channels.add(channel);
            } else {
                log.info("Skipping channel '{}' because filtered out by channel filters.", channel.toString());
            }
        }

        dataset.setChannels(channels);
        return dataset;
    }

    @Override
    public String persist(String blueprintId, Channel channel) throws ChannelException {
        try {
            return channelManager.persist(blueprintId, channel);
        } catch (ChannelException ce) {
            log.warn("Error while persisting a new channel - Channel: {} - {} : {}", channel, ce.getClass().getName(), ce.toString());
            throw ce;
        }
    }

    @Override
    public boolean canUserModifyChannels() {
        return channelManager.canUserModifyChannels();
    }

    @Override
    public Properties getChannelResourceValues(String id, String language) throws ChannelException {
        Channel channel = getVirtualHosts().getChannelById(getHostGroupNameForCmsHost(), id);
        if (channel == null) {
            log.warn("Cannot find channel for id '{}'", id);
            throw new ChannelException("Cannot find channel for id '" + id + "'");
        }
        return InformationObjectsBuilder.buildResourceBundleProperties(getVirtualHosts().getResourceBundle(channel, new Locale(language)));
    }

}
