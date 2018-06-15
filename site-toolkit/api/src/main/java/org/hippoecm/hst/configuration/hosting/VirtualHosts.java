/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.hosting;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;


/**
 * The container interface for {@link VirtualHost}
 *
 */
public interface VirtualHosts {

    String DEFAULT_SCHEME = "http";

    /**
     * @deprecated since CMS 10.0, HST 2.30.00. Use {@link #isHstFilterExcludedPath(String)} instead
     */
    @Deprecated
    boolean isExcluded(String pathInfo);
    /**
     *
     * Some paths should not be handled by the hst framework request processing, eg /ping/
     *
     * When a path must be excluded, this method return true.
     *
     * @param pathInfo
     * @return true when the path must be excluded for matching to a host.
     */
    boolean isHstFilterExcludedPath(String pathInfo);

    /**
     * <p>This method tries to match a hstContainerURL to a flyweight {@link ResolvedSiteMapItem}. It does so, by first trying to match the
     * correct {@link ResolvedVirtualHost}. If it does find a {@link ResolvedVirtualHost}, the match is delegated to
     * {@link ResolvedVirtualHost#matchMount(String, String)}, which returns the {@link ResolvedMount}. This object
     * delegates to {@link ResolvedMount#matchSiteMapItem(String)} which in the end returns the {@link ResolvedSiteMapItem}. If somewhere
     * in the chain a match cannot be made a MatchException exception is thrown
     * </p>
     *
     * @param hstContainerURL the HstContainerURL
     * @return the resolvedSiteMapItem for this request
     * @throws MatchException when the matching cannot be done, for example because no valid virtual hosts are configured or when the request path does not match
     * a sitemap item
     * @deprecated since 2.28.00 (CMS 7.9) use {@link #matchMount(String, String, String)} instead and then
     * {@link ResolvedMount#matchSiteMapItem(String)}
     */
    @Deprecated
    ResolvedSiteMapItem matchSiteMapItem(HstContainerURL hstContainerURL) throws MatchException;


    /**
     * <p> This method tries to match a hostName, contextPath and requestPath to a flyweight {@link ResolvedMount}. It
     * does so, by first trying to match the correct {@link ResolvedVirtualHost}. If it does find a {@link
     * ResolvedVirtualHost}, the match is delegated to {@link ResolvedVirtualHost#matchMount(String, String)}, which
     * returns the {@link ResolvedMount}. If somewhere in the chain a match cannot be made, <code>null</code> will be
     * returned. If {@link org.hippoecm.hst.configuration.hosting.Mount#getContentPath()} ()} returns a non-null value,
     * the {@link Mount} can only match for the request if the {@link javax.servlet.http.HttpServletRequest#getContextPath()}
     * is equal to {@link org.hippoecm.hst.configuration.hosting.Mount#getContextPath()}. Note that the ROOT.war matches
     * to a {@link org.hippoecm.hst.configuration.hosting.Mount#getContextPath()} equal to empty String. When {@link
     * Mount#getContextPath()} returns <code>null</code> the {@link javax.servlet.http.HttpServletRequest#getContextPath()}
     * is ignored for matching. </p>
     *
     * @param hostName
     * @param contextPath the contextPath of the {@link javax.servlet.http.HttpServletRequest}
     * @param requestPath
     * @return the {@link ResolvedMount} for this hstContainerUrl or <code>null</code> when it can not be matched to a
     *         {@link Mount}
     * @throws MatchException
     */
    ResolvedMount matchMount(String hostName, String contextPath, String requestPath) throws MatchException;

    /**
     * <p>
     *  This method tries to match a request to a flyweight {@link ResolvedVirtualHost}
     * </p>
     * @param hostName
     * @return the resolvedVirtualHost for this hostName or <code>null</code> when it can not be matched to a virtualHost
     * @throws MatchException
     */
    ResolvedVirtualHost matchVirtualHost(String hostName) throws MatchException;

    /**
     * @return the hostname that is configured as default, or <code>null</code> if none is configured as default.
     */
    String getDefaultHostName();

    /**
     * This is the global setting for every {@link VirtualHost} / {@link Mount} whether contextPath should be in the URL or not
     * @return <code>true</code> when the created url should have the contextPath in it
     */
    boolean isContextPathInUrl();

    /**
     * @return the context path of the webapp for this hst configuration
     */
    String getContextPath();

    /**
     * This is the global setting for every {@link VirtualHost} / {@link Mount} whether the port number should be in the URL or not
     * @return <code>true</code> when the created url should have the port number in it
     */
    boolean isPortInUrl();

    /**
     * @return the locale of this VirtualHosts object or <code>null</code> if no locale is configured
     */
    String getLocale();

    /**
     * Returns the {@link Mount} for this <code>hostGroupName</code>, <code>alias<code> and <code>type<code> having {@link Mount#getType()} equal to <code>type</code>. Returns <code>null</code> when no match
     *
     * @param hostGroupName
     * @param alias the alias the mount must have
     * @param type  the type (for example preview, live, composer) the siteMount must have.
     * @return the {@link Mount} for this <code>hostGroupName</code>, <code>alias<code> and <code>type<code> having {@link Mount#getType()} equal to <code>type</code>. Returns <code>null</code> when no match
     */
    Mount getMountByGroupAliasAndType(String hostGroupName, String alias, String type);

    /**
     * @param hostGroupName
     * @return the List<{@link Mount}> belonging to <code>hostGroupName</code> or <code>null</code> when there are no {@link Mount} for <code>hostGroupName</code>
     */
    List<Mount> getMountsByHostGroup(String hostGroupName);

    /**
     * @return return the list of all hostGroupNames
     */
    List<String> getHostGroupNames();

    /**
     * @param uuid
     */
    Mount getMountByIdentifier(String uuid);

    /**
     * The cmsPreviewPrefix will never start or end with a slash and will never be <code>null</code>
     * @return the configured cmsPreviewPrefix with leading and trailing slashes removed. It will never be <code>null</code>. If configured
     * to be empty, it will be ""
     */
     String getCmsPreviewPrefix();

     /**
      * @return the node name of the hst:sites that will be managed by the {@link ChannelManager}. If not configured it returns <code>hst:sites</code>
      */
     String getChannelManagerSitesName();

    /**
     * @return <code>true</code> when diagnostics about request processing is enabled for the client IP address.
     * If <code>ip</code> is <code>null</code>, then the <code>ip</code> address of the request won't be taken into account
     * to determine whether or not the diagnostics is enabled.
     */
    boolean isDiagnosticsEnabled(String ip);

    /**
     * If {@link #isDiagnosticsEnabled(String)} returns {@code true}, only until {@link #getDiagnosticsDepth()} the
     * call hierarchy timings will be logged. Default value returned is {@code -1} meaning no limit
     * @return the depth until where to log and -1 if not limit
     */
    int getDiagnosticsDepth();

    /**
     * If {@link #isDiagnosticsEnabled(String)} returns {@code true}, only log if the {@link org.hippoecm.hst.diagnosis.Task}
     * took longer than or equal to {@link #getDiagnosticsThresholdMillis()}. Default threshold of {@code -1} meaning no threshold
     * @return the threshold value configured and {@code -1} if not configured meaning no threshold.
     */
    long getDiagnosticsThresholdMillis();

    /**
     * @return the threshold value for a task to get logged separately. If not configured {@code -1} is returned meaning
     * no threshold for subtask diagnostics
     */
    long getDiagnosticsUnitThresholdMillis();

    /**
     * @deprecated Use {@link #getDefaultResourceBundleIds()} instead.
     * @return the first item of default resource bundle IDs or null if not configured or empty.
     */
    @Deprecated
    String getDefaultResourceBundleId();

    /**
     * @return default resource bundle IDs for all sites to use, for example { "org.example.resources.MyResources" }, or empty array
     * when not configured
     */
    String [] getDefaultResourceBundleIds();

    /**
     * @return <code>true</code> when the channel manager can skip authentication required for mounts or sitemapitems.
     */
    boolean isChannelMngrSiteAuthenticationSkipped();

    /**
     * @param hostGroup the name of the host group to get the channels for
     * @return all managed channels for the <code></code>hostGroup</code>. Empty List in case the hostGroup does not
     * exist or has no channel. The keys in the map are the {@link Channel#getId()}'s
     */
    Map<String, Channel> getChannels(String hostGroup);

    /**
     * @return The map of all {@code hostGroup} names to the map of all the channels for that hostgroup
     */
    Map<String, Map<String, Channel>> getChannels();

    /**
     * @param hostGroup the name of the host group to get channel for
     * @return the channel configured at the given <code>channelPath</code> and <code>null</code> if no such channel exists
     * @throws IllegalArgumentException in case of invalid <code>channelPath</code>
     */
    Channel getChannelByJcrPath(String hostGroup, String channelPath);

    /**
     * For <code>hostGroup</code> get a {@link Channel} given its id
     * @param hostGroup the name of the host group to get channel for
     * @param id - {@link Channel} id
     * @return {@link Channel} which has this id or <code>null</code>
     */
    Channel getChannelById(String hostGroup, String id);

    /**
     * The list of available blueprints
     */
    List<Blueprint> getBlueprints();

    /**
     * Retrieve a blue print from it's ID.
     * @param id
     */
    Blueprint getBlueprint(String id);

    /**
     * The channel info class for this channel.  Since this class comes from a separate
     * context, it cannot be deserialized.
     *
     * @param channel - {@link Channel} for which {@link org.hippoecm.hst.configuration.channel.ChannelInfo} is going to be retrieved
     * @return The {@link org.hippoecm.hst.configuration.channel.ChannelInfo} {@link Class} type of {@link Channel}
     */
    Class<? extends ChannelInfo> getChannelInfoClass(Channel channel) throws ChannelException;

    /**
     * The channel info class for this channel identified by id.
     *
     * @param hostGroup the name of the host group to get channel for
     * @param id - {@link Channel} id
     * @return The {@link ChannelInfo} {@link Class} type of {@link Channel} identified by id
     */
    Class<? extends ChannelInfo> getChannelInfoClass(String hostGroup, String id) throws ChannelException;

    /**
     * The channel info mixin classes for this channel.  Since these classes come from a separate
     * context, it cannot be deserialized.
     *
     * @param channel - {@link Channel} for which {@link org.hippoecm.hst.configuration.channel.ChannelInfo} is going to be retrieved
     * @return List of {@link org.hippoecm.hst.configuration.channel.ChannelInfo} {@link Class} mixin types of {@link Channel}
     */
    List<Class<? extends ChannelInfo>> getChannelInfoMixins(Channel channel) throws ChannelException;

    /**
     * The channel info mixin classes for this channel identified by id.
     *
     * @param hostGroup the name of the host group to get channel for
     * @param id - {@link Channel} id
     * @return List of {@link ChannelInfo} {@link Class} mixin types of {@link Channel} identified by id
     */
    List<Class<? extends ChannelInfo>> getChannelInfoMixins(String hostGroup, String id) throws ChannelException;

    /**
     * The channel info for this channel.  It is an instance of the {@link #getChannelInfoClass} class.
     *
     * @param channel
     * @param <T>
     * @throws ChannelException
     */
    <T extends ChannelInfo> T getChannelInfo(Channel channel) throws ChannelException;

    /**
     * The resource bundle for the channel info.  It contains the display names for fields
     * and values.
     * @return The ResourceBundle or null if the resource bundle could not be found
     */
    ResourceBundle getResourceBundle(Channel channel, Locale locale);

    /**
     * Get {@link Channel} property definitions given a {@link Channel} object instance
     *
     * @param channel - {@link Channel} for which property definitions are going to be retrieved
     * @return {@link List} of {@link org.hippoecm.hst.configuration.channel.HstPropertyDefinition}
     */
    List<HstPropertyDefinition> getPropertyDefinitions(Channel channel);

    /**
     * Get {@link Channel} property definitions given a {@link Channel} id
     *
     * @param hostGroup the name of the host group to get channel for
     * @param channelId - {@link Channel} id for which property definitions are going to be retrieved
     * @return {@link List} of {@link HstPropertyDefinition}
     */
    List<HstPropertyDefinition> getPropertyDefinitions(String hostGroup, String channelId);


}
