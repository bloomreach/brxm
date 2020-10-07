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
package org.onehippo.cms7.services.hst;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Channel implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Channel.class);
    private static final long serialVersionUID = 1L;
    private static final Pattern VIEWPORT_PATTERN = Pattern.compile("^([^:]+):(\\d+)(px)?$");

    public static final String DEFAULT_DEVICE = "default";

    private String id;
    private String name;
    private boolean channelSettingsEditable;
    private String type;
    private String hostname;
    private String hostGroup;
    private String contextPath;
    private String cmsPreviewPrefix;
    private String mountPath;
    private String channelPath;

    private String url; //Probably not needed for all channels ?

    // in case the channel mgr should load an SPA
    private String spaUrl;

    private String hstMountPoint;
    private String hstConfigPath;
    private String contentRoot;
    private boolean composerModeEnabled;
    private Map<String, Object> properties = new HashMap<>();
    private String channelInfoClassName;
    private List<String> channelInfoMixinNames;
    private String mountId;
    private String siteMapId;
    private String locale;
    private boolean previewHstConfigExists;
    private boolean workspaceExists;
    private boolean hasCustomProperties;
    private boolean deletable;

    // the set of users that have changes for the channel: Can be the channel node itself or some
    // hst configuration belonging to the channel
    private Set<String> changedBySet = new HashSet<>();
    private String defaultDevice = DEFAULT_DEVICE;
    private List<String> devices = Collections.emptyList();
    private Map<String, Integer> viewportMap = new HashMap<>();
    private boolean isPreview;
    private String channelNodeLockedBy;
    private String lastModifiedBy;
    // when the channel node got locked
    private Calendar lockedOn;
    private Calendar lastModified;

    private String branchId;
    private String branchOf;

    // if true the entire configuration is locked
    private boolean configurationLocked;

    /**
     * {@link Channel} default constructor it is required for REST de/serialization
     */
    public Channel() {
    }

    /**
     * Constructor of a Channel.  Should normally only be invoked by the Channel manager implementation
     * to guarantee uniqueness of the id.
     *
     * @param id the unique ID of this channel
     */
    public Channel(String id) {
        this.id = id;
    }

    // copy constructor
    public Channel(final Channel channel) {
        id = channel.id;
        name = channel.name;
        channelSettingsEditable = channel.channelSettingsEditable;
        configurationLocked = channel.configurationLocked;
        type = channel.type;
        hostname = channel.hostname;
        hostGroup = channel.hostGroup;
        contextPath = channel.contextPath;
        cmsPreviewPrefix = channel.cmsPreviewPrefix;
        mountPath = channel.mountPath;
        channelPath = channel.channelPath;
        url = channel.url;
        spaUrl = channel.spaUrl;
        hstMountPoint = channel.hstMountPoint;
        hstConfigPath = channel.hstConfigPath;
        contentRoot = channel.contentRoot;
        composerModeEnabled = channel.composerModeEnabled;
        // not a deep clone: Not a problem!
        Map<String, Object> mapClone = new HashMap<>();
        mapClone.putAll(channel.getProperties());
        setProperties(mapClone);
        channelInfoClassName = channel.channelInfoClassName;
        if (channel.channelInfoMixinNames != null) {
            channelInfoMixinNames = new ArrayList<>(channel.channelInfoMixinNames);
        }
        mountId = channel.mountId;
        siteMapId = channel.siteMapId;
        locale = channel.locale;
        previewHstConfigExists = channel.previewHstConfigExists;
        workspaceExists = channel.workspaceExists;
        hasCustomProperties = channel.hasCustomProperties;
        deletable = channel.deletable;
        // not a deep clone: Not a problem! Note even the same instance is used, this is because
        // channel.changedBySet can be a ChannelLazyLoadingChangedBySet
        changedBySet = channel.changedBySet;
        defaultDevice = channel.defaultDevice;
        devices = channel.devices;
        viewportMap = channel.viewportMap;
        isPreview = channel.isPreview();
        channelNodeLockedBy = channel.channelNodeLockedBy;
        lastModifiedBy = channel.lastModifiedBy;
        lockedOn = channel.lockedOn;
        lastModified = channel.lastModified;
        branchId = channel.branchId;
        branchOf = channel.branchOf;
    }

    /**
     * @return the unique ID of this channel
     */
    public String getId() {
        return id;
    }

    /**
     * Set the unique ID of this channel
     */
    public void setId(String id) throws IllegalStateException {
        if (this.id != null) {
            throw new IllegalStateException("Channel id has been already set. It can not be changed.");
        }

        this.id = id;
    }

    public boolean isChannelSettingsEditable() {
        return channelSettingsEditable;
    }

    public void setChannelSettingsEditable(final boolean channelSettingsEditable) {
        this.channelSettingsEditable = channelSettingsEditable;
    }

    public boolean isConfigurationLocked() {
        return configurationLocked;
    }

    public void setConfigurationLocked(final boolean configurationLocked) {
        this.configurationLocked = configurationLocked;
    }

    public String getContentRoot() {
        return contentRoot;
    }

    public void setContentRoot(String contentRoot) {
        this.contentRoot = contentRoot;
    }

    public String getHstMountPoint() {
        return hstMountPoint;
    }

    public void setHstMountPoint(final String hstMountPoint) {
        this.hstMountPoint = hstMountPoint;
    }

    public String getHstConfigPath() {
        return hstConfigPath;
    }

    public void setHstConfigPath(String hstConfigPath) {
        this.hstConfigPath = hstConfigPath;
    }

    /**
     * @return the human-readable name of this channel, or null if this channel does not have a name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the human-readable name of this channel.
     *
     * @param name the new name of this channel
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the fully qualified URL of this channel.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the fully qualified URL of this channel.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    public String getSpaUrl() {
        return spaUrl;
    }

    public void setSpaUrl(final String spaUrl) {
        this.spaUrl = spaUrl;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public boolean isComposerModeEnabled() {
        return composerModeEnabled;
    }

    public void setComposerModeEnabled(final boolean composerModeEnabled) {
        this.composerModeEnabled = composerModeEnabled;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(final String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public void setMountPath(final String mountPath) {
        this.mountPath = mountPath;
    }

    public String getMountPath() {
        return this.mountPath;
    }

    public void setChannelPath(final String channelPath) {
        this.channelPath = channelPath;
    }

    public String getChannelPath() {
        return channelPath;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Returns the Immutable collection of properties for this {@link Channel}.
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getChannelInfoClassName() {
        return channelInfoClassName;
    }

    public void setChannelInfoClassName(String channelInfoClassName) {
        this.channelInfoClassName = channelInfoClassName;
    }

    public List<String> getChannelInfoMixinNames() {
        return channelInfoMixinNames;
    }

    public void setChannelInfoMixinNames(List<String> channelInfoMixinNames) {
        this.channelInfoMixinNames = channelInfoMixinNames;
    }

    public void setMountId(final String mountId) {
        this.mountId = mountId;
    }

    public String getMountId() {
        return this.mountId;
    }

    public void setSiteMapId(final String sitemapId) {
        this.siteMapId = sitemapId;
    }

    public String getSiteMapId() {
        return this.siteMapId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(final String locale) {
        this.locale = locale;
    }

    public String getCmsPreviewPrefix() {
        return cmsPreviewPrefix;
    }

    public void setCmsPreviewPrefix(final String cmsPreviewPrefix) {
        this.cmsPreviewPrefix = cmsPreviewPrefix;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    public boolean isPreviewHstConfigExists() {
        return previewHstConfigExists;
    }

    public void setPreviewHstConfigExists(final boolean previewHstConfigExists) {
        this.previewHstConfigExists = previewHstConfigExists;
    }

    public boolean isWorkspaceExists() {
        return workspaceExists;
    }

    public void setWorkspaceExists(final boolean workspaceExists) {
        this.workspaceExists = workspaceExists;
    }

    public boolean getHasCustomProperties() {
        return hasCustomProperties;
    }

    public void setHasCustomProperties(final boolean hasCustomProperties) {
        this.hasCustomProperties = hasCustomProperties;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(final boolean deletable) {
        this.deletable = deletable;
    }

    /**
     * @return all *non-system* users that have a lock on the channel or some part of the channel. If no users have a
     * lock, and empty set is returned
     */
    public Set<String> getChangedBySet() {
        if (changedBySet instanceof HashSet) {
            return changedBySet;
        } else {
            // the changed by set might be some custom Set<String> implementation which cannot be
            // correctly deserialized when used by rest calls. Hence, we explicitly make it a HashSet now
            changedBySet = new HashSet<>(changedBySet);
        }
        return changedBySet;
    }

    /**
     * sets all users that have a lock on the channel or some part of the channel
     */
    public void setChangedBySet(final Set<String> changedBySet) {
        this.changedBySet = changedBySet;
    }

    public String getDefaultDevice() {
        return defaultDevice;
    }

    public List<String> getDevices() {
        return devices;
    }

    public void setDefaultDevice(String defaultDevice) {
        this.defaultDevice = defaultDevice;
    }

    public void setDevices(List<String> devices) {
        this.devices = devices;

        populateViewportMap();
    }

    private void populateViewportMap() {
        for (String device : devices) {
            final Matcher m = VIEWPORT_PATTERN.matcher(device);
            if (m.matches()) {
                try {
                    viewportMap.put(m.group(1), Integer.valueOf(m.group(2)));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse Integer {}", m.group(2), e);
                }
            }
        }
    }

    public Map<String, Integer> getViewportMap() {
        return viewportMap;
    }

    public void setPreview(final boolean preview) {
        isPreview = preview;
    }

    public boolean isPreview() {
        return isPreview;
    }

    public String getChannelNodeLockedBy() {
        return channelNodeLockedBy;
    }

    public void setChannelNodeLockedBy(final String channelNodeLockedBy) {
        this.channelNodeLockedBy = channelNodeLockedBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Calendar getLockedOn() {
        return lockedOn;
    }

    public void setLockedOn(final Calendar lockedOn) {
        this.lockedOn = lockedOn;
    }

    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Calendar lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return The id of this branch if this {@link Channel} is a branch and {@code null} otherwise
     */
    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    /**
     * @return The {@link #getId()} of the {@link Channel} of which this channel is a branch and {@code null} if this
     * channel is not a branch
     */
    public String getBranchOf() {
        return branchOf;
    }

    public void setBranchOf(final String branchOf) {
        this.branchOf = branchOf;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        return 31 * result + (mountId != null ? mountId.hashCode() : 0);
    }


    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof Channel)) {
            return false;
        } else {
            Channel that = (Channel) other;
            if (mountId != null) {
                return id.equals(that.id) && mountId.equals(that.mountId);
            } else {
                return id.equals(that.id);
            }
        }
    }

    @Override
    public String toString() {
        return "Channel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", channelSettingsEditable=" + channelSettingsEditable +
                ", configurationLocked=" + configurationLocked +
                ", type='" + type + '\'' +
                ", hostname='" + hostname + '\'' +
                ", hostGroup='" + hostGroup + '\'' +
                ", contextPath='" + contextPath + '\'' +
                ", cmsPreviewPrefix='" + cmsPreviewPrefix + '\'' +
                ", mountPath='" + mountPath + '\'' +
                ", channelPath='" + channelPath + '\'' +
                ", url='" + url + '\'' +
                ", previewUrl ='" + spaUrl + '\'' +
                ", hstMountPoint='" + hstMountPoint + '\'' +
                ", hstConfigPath='" + hstConfigPath + '\'' +
                ", contentRoot='" + contentRoot + '\'' +
                ", composerModeEnabled=" + composerModeEnabled +
                ", properties=" + properties +
                ", channelInfoClassName='" + channelInfoClassName + '\'' +
                ", channelInfoMixinNames=" + channelInfoMixinNames +
                ", mountId='" + mountId + '\'' +
                ", siteMapId='" + siteMapId + '\'' +
                ", locale='" + locale + '\'' +
                ", previewHstConfigExists=" + previewHstConfigExists +
                ", workspaceExists=" + workspaceExists +
                ", hasCustomProperties=" + hasCustomProperties +
                ", deletable=" + deletable +
                ", changedBySet=" + changedBySet +
                ", defaultDevice='" + defaultDevice + '\'' +
                ", devices=" + devices +
                ", viewportMap=" + viewportMap +
                ", isPreview=" + isPreview +
                ", channelNodeLockedBy='" + channelNodeLockedBy + '\'' +
                ", lastModifiedBy='" + lastModifiedBy + '\'' +
                ", lockedOn=" + lockedOn +
                ", lastModified=" + lastModified +
                ", branchId=" + branchId +
                ", branchOf=" + branchOf +
                '}';
    }

}
