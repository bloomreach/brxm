/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.cms.blog;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlogImporterConfiguration {
    public static final Logger log = LoggerFactory.getLogger(BlogImporterConfiguration.class);

    private static final String PROP_CRONEXPRESSION = "cronExpression";
    private static final String PROP_ACTIVE = "active";
    private static final String PROP_RUNNOW = "runInstantly";

    private final String cronExpression;
    private final Boolean active;
    private final Boolean runNow;
    private final String blogBasePath;
    private final String authorsBasePath;
    private final String projectNamespace;
    private final List<String> urls;
    private final List<String> authors;

    public BlogImporterConfiguration(Node moduleConfigNode) throws RepositoryException {
        cronExpression = JcrUtils.getStringProperty(moduleConfigNode, PROP_CRONEXPRESSION, null);
        active = JcrUtils.getBooleanProperty(moduleConfigNode, PROP_ACTIVE, Boolean.FALSE);
        runNow = JcrUtils.getBooleanProperty(moduleConfigNode, PROP_RUNNOW, Boolean.FALSE);

        projectNamespace = JcrUtils.getStringProperty(moduleConfigNode, BlogImporterJob.PROJECT_NAMESPACE, null);
        blogBasePath = JcrUtils.getStringProperty(moduleConfigNode, BlogImporterJob.BLOGS_BASE_PATH, null);
        authorsBasePath = JcrUtils.getStringProperty(moduleConfigNode, BlogImporterJob.AUTHORS_BASE_PATH, null);
        urls = Arrays.asList(JcrUtils.getMultipleStringProperty(moduleConfigNode, BlogImporterJob.URLS, null));
        authors = Arrays.asList(JcrUtils.getMultipleStringProperty(moduleConfigNode, BlogImporterJob.AUTHORS, null));
        if (authors.size() != urls.size()) {
            log.error("Authors and URL size mismatch, no blogs will be imported.");
            authors.clear();
            urls.clear();
        }
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isRunNow() {
        return runNow;
    }

    public String getBlogBasePath() {
        return blogBasePath;
    }

    public String getAuthorsBasePath() {
        return authorsBasePath;
    }

    public String getProjectNamespace() {
        return projectNamespace;
    }

    public List<String> getUrls() {
        return Collections.unmodifiableList(urls);
    }

    public List<String> getAuthors() {
        return Collections.unmodifiableList(authors);
    }
}
