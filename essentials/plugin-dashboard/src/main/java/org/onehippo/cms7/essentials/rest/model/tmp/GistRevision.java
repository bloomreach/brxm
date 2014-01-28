/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest.model.tmp;

import java.io.Serializable;
import java.util.Date;

/**
 * @version "$Id$"
 */
public class GistRevision implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7863453407918499259L;

    private Date committedAt;

    private GistChangeStatus changeStatus;

    private String url;

    private String version;

    private User user;

    /**
     * @return committedAt
     */
    public Date getCommittedAt() {
        return committedAt;
    }

    /**
     * @param committedAt
     * @return this gist revision
     */
    public GistRevision setCommittedAt(Date committedAt) {
        this.committedAt = committedAt;
        return this;
    }

    /**
     * @return changeStatus
     */
    public GistChangeStatus getChangeStatus() {
        return changeStatus;
    }

    /**
     * @param changeStatus
     * @return this gist revision
     */
    public GistRevision setChangeStatus(GistChangeStatus changeStatus) {
        this.changeStatus = changeStatus;
        return this;
    }

    /**
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url
     * @return this gist revision
     */
    public GistRevision setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     * @return this gist revision
     */
    public GistRevision setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * @return user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user
     * @return this gist revision
     */
    public GistRevision setUser(User user) {
        this.user = user;
        return this;
    }
}
