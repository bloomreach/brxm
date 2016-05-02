/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.List;
import java.util.function.Predicate;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.InformationObjectsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelServiceImpl implements ChannelService {
    private static final Logger log = LoggerFactory.getLogger(ChannelServiceImpl.class);

    @Override
    public ChannelInfoClassInfo getChannelInfo(final String channelId) throws ChannelException {
        try {
            Class<? extends ChannelInfo> channelInfoClass = getVirtualHosts().getChannelInfoClass(getCurrentVirtualHost().getHostGroupName(), channelId);
            ChannelInfoClassInfo channelInfoClassInfo = null;
            if (channelInfoClass != null) {
                channelInfoClassInfo = InformationObjectsBuilder.buildChannelInfoClassInfo(channelInfoClass);
            }
            return channelInfoClassInfo;
        } catch (ChannelException e) {
            if (log.isDebugEnabled()) {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e);
            } else {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e.toString());
            }
            throw e;
        }
    }

    @Override
    public Channel getChannel(final String channelId) {
        final VirtualHost virtualHost = getCurrentVirtualHost();
        return getVirtualHosts().getChannelById(virtualHost.getHostGroupName(), channelId);
    }

    private VirtualHost getCurrentVirtualHost() {
        return RequestContextProvider.get().getResolvedMount().getMount().getVirtualHost();
    }

    private VirtualHosts getVirtualHosts() {
        return RequestContextProvider.get().getVirtualHost().getVirtualHosts();
    }
}
