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
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * @version "$Id$"
 */
public class Gist implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    @SerializedName("public")
    private boolean isPublic;

    private Date createdAt;

    private Date updatedAt;

    private int comments;

    private List<GistRevision> history;

    private Map<String, GistFile> files;

    private String description;

    private String gitPullUrl;

    private String gitPushUrl;

    private String htmlUrl;

    private String id;

    private String url;

    private User user;

    /**
     * @return isPublic
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * @param isPublic
     * @return this gist
     */
    public Gist setPublic(boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }

    /**
     * @return createdAt
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt
     * @return this gist
     */
    public Gist setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * @return updatedAt
     */
    public Date getUpdatedAt() {
        return createdAt;
    }

    /**
     * @param updatedAt
     * @return this gist
     */
    public Gist setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    /**
     * @return comments
     */
    public int getComments() {
        return comments;
    }

    /**
     * @param comments
     * @return this gist
     */
    public Gist setComments(int comments) {
        this.comments = comments;
        return this;
    }

    /**
     * @return history
     */
    public List<GistRevision> getHistory() {
        return history;
    }

    /**
     * @param history
     * @return this gist
     */
    public Gist setHistory(List<GistRevision> history) {
        this.history = history;
        return this;
    }

    /**
     * @return files
     */
    public Map<String, GistFile> getFiles() {
        return files;
    }

    /**
     * @param files
     * @return this gist
     */
    public Gist setFiles(Map<String, GistFile> files) {
        this.files = files;
        return this;
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     * @return this gist
     */
    public Gist setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * @return gitPullUrl
     */
    public String getGitPullUrl() {
        return gitPullUrl;
    }

    /**
     * @param gitPullUrl
     * @return this gist
     */
    public Gist setGitPullUrl(String gitPullUrl) {
        this.gitPullUrl = gitPullUrl;
        return this;
    }

    /**
     * @return gitPushUrl
     */
    public String getGitPushUrl() {
        return gitPushUrl;
    }

    /**
     * @param gitPushUrl
     * @return this gist
     */
    public Gist setGitPushUrl(String gitPushUrl) {
        this.gitPushUrl = gitPushUrl;
        return this;
    }

    /**
     * @return htmlUrl
     */
    public String getHtmlUrl() {
        return htmlUrl;
    }

    /**
     * @param htmlUrl
     * @return this gist
     */
    public Gist setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     * @return this gist
     */
    public Gist setId(String id) {
        this.id = id;
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
     * @return this gist
     */
    public Gist setUrl(String url) {
        this.url = url;
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
     * @return this gist
     */
    public Gist setUser(User user) {
        this.user = user;
        return this;
    }
}
