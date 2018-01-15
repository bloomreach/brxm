/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.blog.model;

import java.util.List;

public class BlogDaemonModuleConfiguration {

    private boolean active;
    private boolean runInstantly;
    private String blogsBasePath;
    private String authorsBasePath;
    private String projectNamespace;
    private String cronExpression;
    private String cronExpressionDescription;
    private int maxDescriptionLength;
    private List<URL> urls;

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public boolean isRunInstantly() {
        return runInstantly;
    }

    public void setRunInstantly(final boolean runInstantly) {
        this.runInstantly = runInstantly;
    }

    public String getBlogsBasePath() {
        return blogsBasePath;
    }

    public void setBlogsBasePath(final String blogsBasePath) {
        this.blogsBasePath = blogsBasePath;
    }

    public String getAuthorsBasePath() {
        return authorsBasePath;
    }

    public void setAuthorsBasePath(final String authorsBasePath) {
        this.authorsBasePath = authorsBasePath;
    }

    public String getProjectNamespace() {
        return projectNamespace;
    }

    public void setProjectNamespace(final String projectNamespace) {
        this.projectNamespace = projectNamespace;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getCronExpressionDescription() {
        return cronExpressionDescription;
    }

    public void setCronExpressionDescription(final String cronExpressionDescription) {
        this.cronExpressionDescription = cronExpressionDescription;
    }

    public int getMaxDescriptionLength() {
        return maxDescriptionLength;
    }

    public void setMaxDescriptionLength(final int maxDescriptionLength) {
        this.maxDescriptionLength = maxDescriptionLength;
    }

    public List<URL> getUrls() {
        return urls;
    }

    public void setUrls(final List<URL> urls) {
        this.urls = urls;
    }

    public static class URL {
        private String value; // the URL
        private String author;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(final String author) {
            this.author = author;
        }
    }
}
