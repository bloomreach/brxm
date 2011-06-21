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

public class Channel implements Serializable {

    public final static String UNKNOWN_BLUEPRINT = "<unknown-blueprint>";

    private final String bluePrintId;
    private final String id;

    private String title;
    private String url; //Probably not needed for all channels ?
    private String type; //Channel type - preview/live.
    private String hstConfigPath;
    private String contentRoot;
    private boolean composerModeEnabled;

    /**
     * Constructor of a Channel.  Should normally only be invoked by the Channel manager implementation
     * to guarantee uniqueness of the id.
     */
    public Channel(String bluePrintId, String id) {
        this.bluePrintId = bluePrintId;
        this.id = id;
    }

    /**
     * The Blue Print ID for this channel, or {@value UNKNOWN_BLUEPRINT} if no blueprint is available for this channel.
     * @return Blue Print ID
     */
    public String getBlueprintId() {
        return bluePrintId != null ? bluePrintId : UNKNOWN_BLUEPRINT;
    }

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

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    /**
     *
     * @param url
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
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", type='" + type + '\'' +
                ", hstConfigPath='" + hstConfigPath + '\'' +
                ", contentRoot='" + contentRoot + '\'' +
                '}';
    }
}
