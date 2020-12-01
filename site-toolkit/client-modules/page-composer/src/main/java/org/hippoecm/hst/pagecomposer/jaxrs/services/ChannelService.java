/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;

public interface ChannelService {

    ChannelInfoDescription getChannelInfoDescription(final String channelId, final String locale, final String hostGroup) throws ChannelException, RepositoryException;

    Channel getChannel(Session session, String channelId, final String hostGroup) throws ChannelException;

    void saveChannel(Session session, String channelId, Channel channel, final String hostGroup) throws RepositoryException, ChannelException;

    /**
     * This method returns only the channels from a single hst webapp which is on purpose since it is used for the
     * cross channel page copy which is not supported over cross webapp
     * @return the List of {@link Channel}s of the *single* webapp for which currently a channel is rendered in the channel mgr
     */
    List<Channel> getChannels(boolean previewConfigRequired, boolean workspaceRequired, boolean skipBranches,
                              boolean skipConfigurationLocked, String hostGroup, String privilegeAllowed);

    Optional<Channel> getChannelByMountId(final String mountId, final String hostGroup);

    boolean canChannelBeDeleted(String channelId, final String hostGroup) throws ChannelException, RepositoryException;

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

    Set<XPageLayout> getXPageLayouts(final Mount mount);
}
