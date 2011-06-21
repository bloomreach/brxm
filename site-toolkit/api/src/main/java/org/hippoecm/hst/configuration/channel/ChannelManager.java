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
import java.util.Map;

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
     * Creates a channel with a unique ID.  The channel will not be persisted until save is invoked.
     *
     * @param blueprintId
     * @return
     * @throws ChannelException when the blueprint is not valid
     */
    Channel createChannel(String blueprintId) throws ChannelException;

    /**
     * Persist a channel; will create the mounts, sites and configuration when the channel is new,
     * when the channel already exists, only the properties may have changed.
     *
     * @param channel
     * @throws ChannelException
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
}
