/**
 * Copyright 2010 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.channelmanager.channels;

import java.io.Serializable;

public class Channel implements Serializable {

    private String title;
    private String url; //Probably not needed for all channels ?
    private String type; //Channel type - preview/live.
    private String hstConfigPath;
    private String contentRoot;
    private String mount; //To be used for rendering template composer

    private Channel parent;  //The parent channel, null if this is the root

    public Channel(String title) {
        this.title = title;
    }

    public String getMount() {
        return mount;
    }

    public void setMount(String mount) {
        this.mount = mount;
    }

    public Channel getParent() {
        return parent;
    }

    public void setParent(Channel parent) {
        this.parent = parent;
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
        if (this.parent == null) return "/";
        return parent.getTitle() + this.title + "/";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
