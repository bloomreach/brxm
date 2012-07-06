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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Channel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String type;
    private String hostname;
    private String contextPath;
    private String cmsPreviewPrefix;
    private String mountPath;

    private String url; //Probably not needed for all channels ?

    private String hstMountPoint;
    private String hstPreviewMountPoint;
    private String hstConfigPath;
    private String contentRoot;
    private boolean composerModeEnabled;
    private final Map<String, Object> properties = new HashMap<String, Object>();
    private String channelInfoClassName;
    private String mountId;
    private String locale;
    private String lockedBy;
    private Long lockedOn;

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
     * Copy constructor; create an independent copy of a channel.
     *
     * @param orig the original channel to copy
     */
    public Channel(Channel orig) {
        this.id = orig.id;

        this.name = orig.name;
        this.hostname = orig.hostname;
        this.mountPath = orig.mountPath;
        this.contextPath = orig.contextPath;
        this.cmsPreviewPrefix = orig.cmsPreviewPrefix;
        this.url = orig.url;
        this.type = orig.type;
        this.hstMountPoint = orig.hstMountPoint;
        this.hstPreviewMountPoint = orig.hstPreviewMountPoint;
        this.hstConfigPath = orig.hstConfigPath;
        this.contentRoot = orig.contentRoot;
        this.composerModeEnabled = orig.composerModeEnabled;
        this.properties.putAll(orig.properties);
        this.channelInfoClassName = orig.channelInfoClassName;
        this.mountId = orig.mountId;
        this.locale = orig.locale;
        this.lockedBy = orig.lockedBy;
        this.lockedOn = orig.lockedOn;
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

    public void setHstPreviewMountPoint(final String hstPreviewMountPoint) {
        this.hstPreviewMountPoint = hstPreviewMountPoint;
    }

    public String getHstPreviewMountPoint() {
        return this.hstPreviewMountPoint;
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

    /**
     * Retrieve this channel lock owner's userId. Returns null if the channel is not locked.
     * @return this channel lock owner's userId
     */
    public String getLockedBy() {
        return lockedBy;
    }

    /**
     * Set owner of this channel's lock. Set to null if the channel is not locked.
     * @param lockedBy this channel lock owner's userId
     */
    public void setLockedBy(final String lockedBy) {
        this.lockedBy = lockedBy;
    }

    /**
     * Retrieve the timestamp when the lock was set. Be warned that this method returns gives invalid results if the
     * channel is not locked.
     * @return timestamp in milliseconds of when channel lock was acquired
     */
    public Long getLockedOn() {
        return lockedOn;
    }

    /**
     * Set to null if the channel is not locked.
     * @param lockedOn timestamp in milliseconds of when channel lock was acquired
     */
    public void setLockedOn(final Long lockedOn) {
        this.lockedOn = lockedOn;
    }

    public int hashCode() {
        return id.hashCode() ^ 317;
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
        b.append(",lockedBy=").append(lockedBy);
        b.append(",lockedOn=").append(lockedOn);
        b.append('}');

        return b.toString();
    }

}
