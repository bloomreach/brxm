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

    public final static String UNKNOWN_BLUEPRINT = "<unknown-blueprint>";

    private final String bluePrintId;
    private final String id;

    private String name;
    private String hostname;
    private String subMountPath; // TODO find proper name
    private String url; //Probably not needed for all channels ?
    private String type; //Channel type - preview/live.
    private String hstConfigPath;
    private String contentRoot;
    private boolean composerModeEnabled;
    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Constructor of a Channel.  Should normally only be invoked by the Channel manager implementation
     * to guarantee uniqueness of the id.
     *
     * @param bluePrintId the ID of the channel's blueprint
     * @param id the unique ID of this channel
     */
    public Channel(String bluePrintId, String id) {
        this.bluePrintId = bluePrintId;
        this.id = id;
    }

    /**
     * The Blueprint ID for this channel, or {@value #UNKNOWN_BLUEPRINT} if no blueprint is available for this channel.
     *
     * @return the blueprint ID of this channel.
     */
    public String getBlueprintId() {
        return bluePrintId != null ? bluePrintId : UNKNOWN_BLUEPRINT;
    }

    /**
     * @return the unique ID of this channel
     */
    public String getId() {
        return id;
    }

    public String getContentRoot() {
        return contentRoot;
    }

    public void setContentRoot(String contentRoot) {
        this.contentRoot = contentRoot;
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

    /**
     * FIXME: does this need to be exposed?
     * @return
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public void setSubMountPath(final String subMountPath) {
        this.subMountPath = subMountPath;
    }

    public String getSubMountPath() {
        return this.subMountPath;
    }

    public Map<String, Object> getProperties() {
        return properties;
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
        return "Channel{" +
                "id=" + id +
                ", bluePrint=" + bluePrintId +
                ", title='" + name + '\'' +
                ", url='" + url + '\'' +
                ", type='" + type + '\'' +
                ", hstConfigPath='" + hstConfigPath + '\'' +
                ", contentRoot='" + contentRoot + '\'' +
                '}';
    }
}
