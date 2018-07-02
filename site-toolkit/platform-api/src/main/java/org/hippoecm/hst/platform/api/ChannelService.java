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
	 * @param cmsHost the host over which the cms is accessed
     * @return {@link List} of {@link Channel}s of all available channels, empty list otherwise. Note that if for
     * a {@link Channel} there is both a live <b>and</b> preview version, the <b>preview</b> version is returned as
     * that is typically the version that is needed to work with through this {@link ChannelService}
     */
	List<Channel> getChannels(Session userSession, String cmsHost);

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
	 * Check whether user can modify {@link Channel}(s) or not for site webapp belonging to {@code contextPath}
	 *
	 * @param userSession the jcr session which must should be used for the check
	 * @return <code>true</code> if use can modify {@link Channel}, <code>false</code> otherwise
	 */
	// TODO CHANNELMGR-1971 this api method does not suffice and needs to be done differently
	boolean canUserModifyChannels(Session userSession);

    /**
     * Retrieve a {@link ResourceBundle} converted to {@link Properties} of {@link Channel} identified by an Id
	 *
	 * @param cmsHost the host over which the cms is accessed
     * @param channelId - {@link Channel} id
     * @param language - {@link Locale} language
     * @return {@link Properties} equivalent of a {@link Channel}'s {@link ResourceBundle}
     */
    Properties getChannelResourceValues(String cmsHost, String channelId, String language) throws ChannelException;

}
