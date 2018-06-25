/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;

// TODO HSTTWO-4365 get rid of this, use org.hippoecm.hst.platform.api.ChannelService instead
public interface ChannelService {

    ChannelInfoDescription getChannelInfoDescription(final String channelId, final String locale) throws ChannelException;

    Channel getChannel(String channelId) throws ChannelException;

    void saveChannel(Session session, String channelId, Channel channel) throws RepositoryException, ChannelException;

    List<Channel> getChannels(boolean previewConfigRequired, boolean workspaceRequired, boolean skipBranches, boolean skipConfigurationLocked);

    Optional<Channel> getChannelByMountId(final String mountId);

    boolean canChannelBeDeleted(String channelId) throws ChannelException;

    boolean canChannelBeDeleted(Channel channel);

    boolean isMaster(Channel channel);

    /**
     * Validates conditions before return a deletable channel. This method should be called before
     * {@link #deleteChannel(Session, Channel, List<Mount>)}
     *
     * @param session
     * @param channel the channel to be deleted
     * @throws ChannelException
     * @throws RepositoryException
     */
    void preDeleteChannel(Session session, Channel channel, List<Mount> mountsOfChannel) throws ChannelException, RepositoryException;

    /**
     * Remove channel configurations nodes (hst:channel, hst:configuration, hst:site, hst:mount, hst:virtualhost)
     *
     * @param session
     * @param channel channel to be deleted
     * @param mountsOfChannel mounts binding to the channel
     * @throws RepositoryException
     * @throws ChannelException
     */
    void deleteChannel(Session session, Channel channel, List<Mount> mountsOfChannel) throws RepositoryException, ChannelException;

    /**
     * Find all mounts binding to the given channel
     */
    List<Mount> findMounts(final Channel channel);
}
