/*
*  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.platform.api;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.onehippo.cms7.services.hst.Channel;

public interface ChannelService {

	/**
	 * List all managed channels, identified by their channel IDs
	 *
	 * @param userSession - the jcr session of the current user
	 * @param hostGroup the host group for which the channels should be returned
	 * @return {@link List} of {@link Channel}s of all available live channels, empty list otherwise. Also note that
	 * a clone of the {@link Channel} objects of the {@link org.hippoecm.hst.platform.model.HstModel} are returned to
	 * avoid direct modification of the backing hst model in case a setter on a {@link Channel} object is invoked
	 * @throws IllegalArgumentException if {@code userSession} or {@code hostGroup} is {@code null}
	 */
	List<Channel> getLiveChannels(Session userSession, String hostGroup);

	/**
	 *
	 * @param userSession the user session that should have access to the channel for {@code channelId}
	 * @param channelId the id of the channel to get
	 * @param hostGroup the host group to use
	 * @return the live {@link Channel} for {@code channelId} and throws a
	 * {@link org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException} if no such channel available
	 * for {@code userSession}.Also note that a clone of the {@link Channel} object of the
	 * {@link org.hippoecm.hst.platform.model.HstModel} is returned to
	 * avoid direct modification of the backing hst model in case a setter on a {@link Channel} object is invoked
	 */
	Channel getLiveChannel(Session userSession, String channelId, String hostGroup);


	/**
	 * List all managed channels, identified by their channel IDs
	 *
	 * @param userSession - the jcr session of the current user
	 * @param hostGroup the host group for which the channels should be returned
	 * @return {@link List} of {@link Channel}s of all available preview channels, empty list otherwise. Note that if for
	 * a {@link Channel} there is both a live <b>and</b> preview version, the <b>preview</b> version is returned and
	 * otherwise the live. Also note that a clone of the {@link Channel} objects of the
	 * {@link org.hippoecm.hst.platform.model.HstModel} are returned to
	 * avoid direct modification of the backing hst model in case a setter on a {@link Channel} object is invoked
	 * @throws IllegalArgumentException if {@code userSession} or {@code hostGroup} is {@code null}
	 */
	List<Channel> getPreviewChannels(Session userSession, String hostGroup);


	/**
	 *
	 * @param userSession the user session that should have access to the channel for {@code channelId}
	 * @param channelId the id of the channel to get
	 * @param hostGroup the host group to use
	 * @return the preview {@link Channel} for {@code channelId} and throws a
	 * {@link org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException} if no such channel available
	 * for {@code userSession}. Also note that a clone of the {@link Channel} object of the
	 * {@link org.hippoecm.hst.platform.model.HstModel} is returned to
	 * avoid direct modification of the backing hst model in case a setter on a {@link Channel} object is invoked
	 */
	Channel getPreviewChannel(Session userSession, String channelId, String hostGroup);

	/**
	 * Persist a new {@link Channel} object instance based on {@link Blueprint} identified by an Id
	 *
	 * @param userSession - the jcr session to persist the channel with
	 * @param blueprintId - The {@link Blueprint} id
	 * @param channel - {@link Channel} object instance
	 * @return The new {@link Channel}'s id
	 */
    String persist(Session userSession, String blueprintId, Channel channel) throws ChannelException;

    /**
     * Retrieve a {@link ResourceBundle} converted to {@link Properties} of {@link Channel} identified by an Id
	 *
	 * @param hostGroup the host group for which the resource values should be fetched
     * @param channelId - {@link Channel} id
     * @param language - {@link Locale} language
     * @return {@link Properties} equivalent of a {@link Channel}'s {@link ResourceBundle}
     */
    Properties getChannelResourceValues(String hostGroup, String channelId, String language) throws ChannelException;


}
