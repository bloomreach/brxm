/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.channel;

import javax.jcr.Session;

import org.onehippo.cms7.services.hst.Channel;

/**
 * Management interface for {@link Channel}s.  Basic Channel operations are provided.
 */
public interface ChannelManager {

    /**
     * Persists a channel. Will create the mounts, sites and configuration when the channel is new.
     * <p>
     * When invoking this method, an HstSubject context must be provided with the credentials necessary
     * to persist the channel.
     * </p>
     * <p>
     * The persisted channel can be retrieved again via {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getChannelById(String, String)}
     * with the returned channel ID.
     * </p>
     *
     *
     * @param session the session used to persist the changes
     * @param blueprintId blueprint that contains prototypes for mount, site and hst configuration
     * @param channel a channel instance to be persisted
     * @return the channel ID of the created channel
     * @throws ChannelException with type @{link ChannelException.Type#MOUNT_NOT_FOUND} when all but the last path-step
     * in the URL path of a new channel do not map to existing mounts. The exception has one parameter: the absolute
     * JCR path of the missing mount.
     * @throws ChannelException with type @{link ChannelException.Type#MOUNT_EXISTS} when the mount of a new channel
     * already exists. The exception has one parameter: the absolute JCR path of the existing mount.
     * </ul>
     */
    String persist(final Session session, String blueprintId, Channel channel) throws ChannelException;

    /**
     * Save channel properties for the given host group. If the URL path of the new channel is not empty, all
     * path-steps except the last one should already map to an existing mount.
     *
     * @param session the session used to persist the changes
     * @param hostGroupName
     * @param channel
     * @throws ChannelException
     */
    void save(final Session session, final String hostGroupName, final Channel channel) throws ChannelException;

    /**
     * Can the current session create or modify channels.
     *
     * @return true when the user can create a channel, false otherwise
     */
    boolean canUserModifyChannels(final Session session);

}
