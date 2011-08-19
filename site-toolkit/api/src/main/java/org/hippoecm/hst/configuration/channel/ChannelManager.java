/*
 *  Copyright 2011 Hippo.
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
     * List all managed channels, identified by their channel IDs
     * @return
     */
    Map<String, Channel> getChannels() throws ChannelException;

    /**
     * Persist a channel; will create the mounts, sites and configuration when the channel is new.
     * <p>
     * When invoking this method, an HstSubject context must be provided with the credentials necessary
     * to persist the channel.
     * </p>
     *
     * @param blueprintId blueprint that contains prototypes for mount, site and hst configuration
     * @param channel a channel instance to be persisted
     * @throws ChannelException when the channel id already exists, or the channel could not be persisted.
     */
    void persist(String blueprintId, Channel channel) throws ChannelException;

    /**
     * Save channel properties.  If the URL path of the new channel is not empty, all
     * path-steps except the last one should already map to an existing mount.
     * <p>
     * When invoking this method, an HstSubject context must be provided with the credentials necessary
     * to persist the channel.
     * </p>
     *
     * @param channel the channel to persist
     * @throws MountNotFoundException when all but the last path-step in the URL path of a new channel
     * do not map to existing mounts, or the URL path of an existing channel does not map to an existing mount.
     * @throws ChannelException when the channel could not be persisted.
     */
    void save(Channel channel) throws ChannelException;

    /**
     * The list of available blueprints
     * @return
     */
    List<Blueprint> getBlueprints() throws ChannelException;

    /**
     * Retrieve a blue print from it's ID.
     * @param id
     * @return
     */
    Blueprint getBlueprint(String id) throws ChannelException;

    /**
     * The channel info class for this channel.  Since this class comes from a separate
     * context, it cannot be deserialized.
     *
     * @param channel
     * @return
     */
    Class<? extends ChannelInfo> getChannelInfoClass(Channel channel) throws ChannelException;

    /**
     * The channel info for this channel.  It is an instance of the {@link #getChannelInfoClass} class.
     *
     * @param channel
     * @param <T>
     * @return
     * @throws ChannelException
     */
    <T extends ChannelInfo> T getChannelInfo(Channel channel) throws ChannelException;

    /**
     * The resource bundle for the channel info.  It contains the display names for fields
     * and values.
     */
    ResourceBundle getResourceBundle(Channel channel, Locale locale);

    List<HstPropertyDefinition> getPropertyDefinitions(Channel channel);
}
