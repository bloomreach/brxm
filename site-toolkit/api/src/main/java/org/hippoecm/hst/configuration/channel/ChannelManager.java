/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

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
     * The persisted channel can be retrieved again via {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getChannelById(String)}
     * with the returned channel ID.
     * </p>
     *
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
    String persist(String blueprintId, Channel channel) throws ChannelException;

    /**
     * Save channel properties.  If the URL path of the new channel is not empty, all
     * path-steps except the last one should already map to an existing mount.
     * <p>
     * When invoking this method, an HstSubject context must be provided with the credentials necessary
     * to persist the channel.
     * </p>
     *
     * @param channel the channel to persist
     * @throws ChannelException with type @{link ChannelException.Type#MOUNT_NOT_FOUND} when all but the last path-step
     * in the URL path of a new channel do not map to existing mounts, or the URL path of an existing channel does not
     * map to an existing mount. The exception has one parameter: the absolute JCR path of the missing mount.
     * @throws ChannelException with type {@link ChannelException.Type#UNKNOWN}} when the channel could not be persisted.
     */
    void save(Channel channel) throws ChannelException;


    /**
     * List all managed channels, identified by their channel IDs
     * @return
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getChannels()}
     */
    @Deprecated
    Map<String, Channel> getChannels() throws ChannelException;

    /**
     * @return the channel configured at the given <code>channelPath</code> and <code>null</code> if no such channel exists
     * @throws ChannelException in case of invalid <code>channelPath</code>
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getChannelByJcrPath(String)}
     */
    @Deprecated
    Channel getChannelByJcrPath(String channelPath) throws ChannelException;

    /**
     * Get a {@link Channel} given its id
     * @param id - {@link Channel} id
     * @return {@link Channel} which has this id
     * @throws ChannelException - When an error happens while retrieving the {@link Channel}
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getChannelById(String)}
     */
    @Deprecated
    Channel getChannelById(String id) throws ChannelException;

    /**
     * The list of available blueprints
     * @return
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getBlueprints()}
     */
    @Deprecated
    List<Blueprint> getBlueprints() throws ChannelException;

    /**
     * Retrieve a blue print from it's ID.
     * @param id
     * @return
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getBlueprint(String)}
     */
    @Deprecated
    Blueprint getBlueprint(String id) throws ChannelException;

    /**
     * The channel info class for this channel.  Since this class comes from a separate
     * context, it cannot be deserialized.
     *
     * @param channel - {@link Channel} for which {@link ChannelInfo} is going to be retrieved
     * @return The {@link ChannelInfo} {@link Class} type of {@link Channel}
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getChannelInfoClass(Channel)}
     */
    @Deprecated
    Class<? extends ChannelInfo> getChannelInfoClass(Channel channel) throws ChannelException;

    /**
     * The channel info class for this channel identified by id.
     *
     * @param id - {@link Channel} id
     * @return The {@link ChannelInfo} {@link Class} type of {@link Channel} identified by id
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getChannelInfoClass(String)}
     */
    @Deprecated
    Class<? extends ChannelInfo> getChannelInfoClass(String id) throws ChannelException;

    /**
     * The channel info for this channel.  It is an instance of the {@link #getChannelInfoClass} class.
     *
     * @param channel
     * @param <T>
     * @return
     * @throws ChannelException
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getChannelInfo(Channel)}
     */
    @Deprecated
    <T extends ChannelInfo> T getChannelInfo(Channel channel) throws ChannelException;

    /**
     * The resource bundle for the channel info.  It contains the display names for fields
     * and values.
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getResourceBundle(Channel, Locale)}
     */
    @Deprecated
    ResourceBundle getResourceBundle(Channel channel, Locale locale);

    /**
     * Get {@link Channel} property definitions given a {@link Channel} object instance
     *
     * @param channel - {@link Channel} for which property definitions are going to be retrieved
     * @return {@link List} of {@link HstPropertyDefinition}
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getPropertyDefinitions(Channel)}
     */
    @Deprecated
    List<HstPropertyDefinition> getPropertyDefinitions(Channel channel);

    /**
     * Get {@link Channel} property definitions given a {@link Channel} id
     *
     * @param channelId - {@link Channel} id for which property definitions are going to be retrieved
     * @return {@link List} of {@link HstPropertyDefinition}
     * @deprecated sine 7.9.0 use {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#getPropertyDefinitions(String)}
     */
    @Deprecated
    List<HstPropertyDefinition> getPropertyDefinitions(String channelId);

    /**
     * Can the current user (set in HstSubject) create or modify channels.
     *
     * @return true when the user can create a channel, false otherwise
     */
    boolean canUserModifyChannels();

    /**
     * Adds channel manager listeners.
     * @param channelManagerEventListeners
     */
    void addChannelManagerEventListeners(ChannelManagerEventListener ... channelManagerEventListeners);

    /**
     * Removes channel manager listeners.
     * @param channelManagerEventListeners
     */
    void removeChannelManagerEventListeners(ChannelManagerEventListener ... channelManagerEventListeners);

}
