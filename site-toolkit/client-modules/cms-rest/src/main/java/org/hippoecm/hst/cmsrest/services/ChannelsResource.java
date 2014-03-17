/*
*  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.cmsrest.filter.ChannelFilter;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.rest.ChannelService;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.HstPropertyDefinitionInfo;
import org.hippoecm.hst.rest.beans.InformationObjectsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ChannelService} for CMS to interact with {@link Channel} resources
 */
public class ChannelsResource extends BaseResource implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelsResource.class);

    @Override
	public List<Channel> getChannels() {
        final List<Channel> channels = new ArrayList<>();
        // do not use HstServices.getComponentManager().getComponent(HstManager.class.getName()) to get to
        // virtualhosts object since we REALLY need the hst model instance for the current request!!
        HstRequestContext requestContext = RequestContextProvider.get();
        final VirtualHosts virtualHosts = requestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts();
        final List<Mount> mountsForHostGroup = virtualHosts.getMountsByHostGroup(virtualHosts.getChannelManagerHostGroupName());
        for (Mount mount : mountsForHostGroup) {
            if (!(mount instanceof ContextualizableMount)) {
                log.debug("Skip mount '{}' because not a ContextualizableMount (but instead for example a custom Mount used for" +
                        "cms rest communication)", mount);
                continue;
            }
            final Channel previewChannel = ((ContextualizableMount)mount).getPreviewChannel();
            if (previewChannel == null) {

                log.debug("Skipping link for mount '{}' since it does not have a channel", mount.getName());
                continue;
            }
            if (channelFilter.apply(previewChannel)) {
                log.info("Skipping channel '{}' because filtered out by channel filters", previewChannel.toString());
                channels.add(previewChannel);
            }
        }

        return channels;
	}

    @Override
    public void save(Channel channel) throws ChannelException {
        try {
            if (!channel.isPreview()) {
                log.warn("Error while trying to save channel {}: Can only save preview channels", channel);
                throw new ChannelException("Error while trying to save channel + " +channel.getId()+" : Can only save preview channels ");
            }
            channelManager.save(channel);
        } catch (ChannelException ce) {
            log.warn("Error while saving a channel - Channel: {} - {} : {}", new Object[]{channel, ce.getClass().getName(), ce.toString()});
            throw ce;
        }
    }

    @Override
    public String persist(String blueprintId, Channel channel) throws ChannelException {
        try {
            return channelManager.persist(blueprintId, channel);
        } catch (ChannelException ce) {
            log.warn("Error while persisting a new channel - Channel: {} - {} : {}", new Object[]{channel, ce.getClass().getName(), ce.toString()});
            throw ce;
        }
    }

    @Override
    public List<HstPropertyDefinitionInfo> getChannelPropertyDefinitions(String id) {
        return InformationObjectsBuilder.buildHstPropertyDefinitionInfos(getVirtualHosts().getPropertyDefinitions(id));
    }

    @Override
    public Channel getChannel(String id) throws ChannelException {
        final Channel channel = getVirtualHosts().getChannelById(id);
        if (channel == null) {
            log.warn("Failed to retrieve a channel with id '{}'",id);
            throw new ChannelException("Failed to retrieve a channel with id '" + id + "'");
        }
        return channel;
    }

    @Override
    public boolean canUserModifyChannels() {
        return channelManager.canUserModifyChannels();
    }

    @Override
    public ChannelInfoClassInfo getChannelInfoClassInfo(String id) throws ChannelException {
        try {
            Class<? extends ChannelInfo> channelInfoClass = getVirtualHosts().getChannelInfoClass(id);
            ChannelInfoClassInfo channelInfoClassInfo = null;
            if (channelInfoClass != null) {
                channelInfoClassInfo = InformationObjectsBuilder.buildChannelInfoClassInfo(channelInfoClass);
            }
            return channelInfoClassInfo;
        } catch (ChannelException e) {
            log.warn("Failed to retrieve channel info class for channel with id '" + id + "'", e);
            throw e;
        }
    }

    @Override
    public Properties getChannelResourceValues(String id, String language) throws ChannelException {
        Channel channel = getVirtualHosts().getChannelById(id);
        if (channel == null) {
            log.warn("Cannot find channel for id '{}'", id);
            throw new ChannelException("Cannot find channel for id '"+id+"'");
        }
        return InformationObjectsBuilder.buildResourceBundleProperties(getVirtualHosts().getResourceBundle(channel, new Locale(language)));
    }

}
