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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Channel implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_DEVICE = "default";

    private String id;
    private String name;
    private String type;
    private String hostname;
    private String contextPath;
    private String cmsPreviewPrefix;
    private String mountPath;

    private String url; //Probably not needed for all channels ?

    private String hstMountPoint;
    private String hstConfigPath;
    private String contentRoot;
    private boolean composerModeEnabled;
    private final Map<String, Object> properties = new HashMap<String, Object>();
    private String channelInfoClassName;
    private String mountId;
    private String locale;
    private boolean previewHstConfigExists;
    private Set<String> changedBySet = new HashSet<String>();
    private String defaultDevice = DEFAULT_DEVICE;
    private List<String> devices = Collections.EMPTY_LIST;
    private int hashCode;

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
        if ( (this.id != null) && (this.id != "") ) {
            throw new IllegalStateException("Channel id has been already set. It can not be changed.");
        }

        this.id = id;
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

    public void setMountPath(final String mountPath) {
        this.mountPath = mountPath;
    }

    public String getMountPath() {
        return this.mountPath;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getChannelInfoClassName() {
        return channelInfoClassName;
    }

    public void setChannelInfoClassName(String channelInfoClassName) {
        this.channelInfoClassName = channelInfoClassName;
    }

    public void setMountId(final String mountId) {
        this.mountId = mountId;
    }

    public String getMountId() {
        return this.mountId;
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

    /**
     * @return all users that have a lock on the channel or some part of the channel. If no users have a lock, and empty set
     * is returned
     */
    public Set<String> getChangedBySet() {
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
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (contextPath != null ? contextPath.hashCode() : 0);
        result = 31 * result + (cmsPreviewPrefix != null ? cmsPreviewPrefix.hashCode() : 0);
        result = 31 * result + (mountPath != null ? mountPath.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (hstMountPoint != null ? hstMountPoint.hashCode() : 0);
        result = 31 * result + (hstConfigPath != null ? hstConfigPath.hashCode() : 0);
        result = 31 * result + (contentRoot != null ? contentRoot.hashCode() : 0);
        result = 31 * result + (composerModeEnabled ? 1 : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (channelInfoClassName != null ? channelInfoClassName.hashCode() : 0);
        result = 31 * result + (mountId != null ? mountId.hashCode() : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (changedBySet != null ? changedBySet.hashCode() : 0);
        result = 31 * result + (defaultDevice != null ? defaultDevice.hashCode() : 0);
        result = 31 * result + (devices != null ? devices.hashCode() : 0);
        hashCode = result;
        return hashCode;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof Channel)) {
            return false;
        } else {
            Channel that = (Channel) other;
            return id.equals(that.id);
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("Channel{");
        b.append("id=").append(id);
        b.append(",name=").append(name);
        b.append(",type=").append(type);
        b.append(",url=").append(url);
        b.append(",hstConfigPath=").append(hstConfigPath);
        b.append(",contentRoot=").append(contentRoot);
        b.append(",locale=").append(locale);
        b.append(",contextPath=").append(contextPath);
        b.append(",cmsPreviewPrefix=").append(cmsPreviewPrefix);
        b.append(",mountPath=").append(mountPath);
        b.append(",changedBySet=").append(changedBySet);
        b.append(",devices=").append(devices);
        b.append(",defaultDevice=").append(defaultDevice);
        b.append('}');

        return b.toString();
    }

}
