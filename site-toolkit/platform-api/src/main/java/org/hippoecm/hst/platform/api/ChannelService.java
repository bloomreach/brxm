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
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;

public interface ChannelService {

	/**
	 * @see #getLiveChannels(Session, String) and #getPreviewChannels(Session, String) without filtering applied,
	 * now with branches included if {@code branchesIncluded} is true, both preview and live channels
	 */
	List<Channel> getChannels(String hostGroup, boolean branchesIncluded);

	/**
	 * @return the channel from {@link #getChannels(String, boolean)} where the channelId can be preview or live. Returns
	 * {@code null} if no such channel present
	 */
	Channel getChannel(String channelId, String hostGroup);


	/**
	 * <p>
	 * List all managed live master (no branches) channels, possibly filtered by what the {@code userSession} is allowed to see
	 * (via org.hippoecm.hst.platform.api.model.InternalHstModel#getChannelFilter()), identified by their channel IDs.
	 * </p>
	 * <p>
	 *     If you need to get hold of all channels without
	 *     org.hippoecm.hst.platform.api.model.InternalHstModel#getChannelFilter() filters being applied, use
	 *     {@link #getLiveChannels(String)}
	 * </p>
	 *
	 * @param userSession - the jcr session of the current user
	 * @param hostGroup the host group for which the channels should be returned
	 * @return {@link List} of {@link Channel}s of all available live channels for {@code userSession}, empty list otherwise.
	 * Also note that a clone of the {@link Channel} objects of the {@link org.hippoecm.hst.platform.model.HstModel}
	 * are returned to avoid direct modification of the backing hst model in case a setter on a {@link Channel} object is invoked
	 * @throws IllegalArgumentException if {@code userSession} or {@code hostGroup} is {@code null}
	 */
	List<Channel> getLiveChannels(Session userSession, String hostGroup);

	/**
	 * @see #getLiveChannels(Session, String) without filtering applied
	 */
	List<Channel> getLiveChannels(String hostGroup);

	/**
	 * <p>
	 * 	List all managed preview master (no branches) channels, possibly filtered by what the {@code userSession} is allowed to see
	 * 	(via org.hippoecm.hst.platform.api.model.InternalHstModel#getChannelFilter()), identified by their channel IDs.
	 * </p>
	 * <p>
	 *     If you need to get hold of all channels without
	 *     org.hippoecm.hst.platform.api.model.InternalHstModel#getChannelFilter() filters being applied, use
	 *     {@link #getPreviewChannels(String)}
	 * </p>
	 *
	 * @param userSession - the jcr session of the current user
	 * @param hostGroup the host group for which the channels should be returned
	 * @return {@link List} of {@link Channel}s of all available preview channels for {@code userSession}, empty list otherwise.
	 * Note that if for a {@link Channel} there is both a live <b>and</b> preview version, the <b>preview</b> version
	 * is returned and otherwise the live. Also note that a clone of the {@link Channel} objects of the
	 * {@link org.hippoecm.hst.platform.model.HstModel} are returned to avoid direct modification of the backing hst
	 * model in case a setter on a {@link Channel} object is invoked
	 * @throws IllegalArgumentException if {@code userSession} or {@code hostGroup} is {@code null}
	 */
	List<Channel> getPreviewChannels(Session userSession, String hostGroup);

	/**
	 * @see #getPreviewChannels(Session, String) without filtering applied
	 */
	List<Channel> getPreviewChannels(String hostGroup);

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

	/**
	 *
	 * @param channelId the *live* channelId is expected, not the preview (typically has -preview appended)
	 * @return Map of the available {@link XPageLayout}s for {@code Channel} with id {@code channelId} where the keys are
	 * the id of the XPageLayout (which is equal to the XPageLayout HstComponentConfiguration id).
	 * If none found an empty collection is returned
	 */
	Map<String, XPageLayout> getXPageLayouts(String channelId);

	/**
	 * Same as {@link #getXPageLayouts(String) } only now for a Mount instance where the Mount can be a 'preview decorated mount'
	 * @see #getXPageLayouts(String)
	 */
	Map<String, XPageLayout> getXPageLayouts(Mount mount);

}
