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

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;

public class BlogImporterConfiguration {
    public static final Logger log = LoggerFactory.getLogger(BlogImporterConfiguration.class);

    private static final String PROP_CRONEXPRESSION = "cronExpression";
    private static final String PROP_ACTIVE = "active";
    private static final String PROP_RUNNOW = "runInstantly";

    private String cronExpression;
    private Boolean active;
    private Boolean runNow;
    private String blogBasePath;
    private String authorsBasePath;
    private String projectNamespace;
    private String[] urls;
    private String[] authors;

    public BlogImporterConfiguration(Node moduleConfigNode) throws RepositoryException {
        cronExpression = JcrUtils.getStringProperty(moduleConfigNode, PROP_CRONEXPRESSION, null);
        active = JcrUtils.getBooleanProperty(moduleConfigNode, PROP_ACTIVE, Boolean.FALSE);
        runNow = JcrUtils.getBooleanProperty(moduleConfigNode, PROP_RUNNOW, Boolean.FALSE);

        projectNamespace = JcrUtils.getStringProperty(moduleConfigNode, BlogImporterJob.PROJECT_NAMESPACE, null);
        blogBasePath = JcrUtils.getStringProperty(moduleConfigNode, BlogImporterJob.BLOGS_BASE_PATH, null);
        authorsBasePath = JcrUtils.getStringProperty(moduleConfigNode, BlogImporterJob.AUTHORS_BASE_PATH, null);
        urls = readStrings(moduleConfigNode, BlogImporterJob.URLS);
        authors = readStrings(moduleConfigNode, BlogImporterJob.AUTHORS);
        if (authors.length != urls.length) {
            log.error("Authors and URL size mismatch, no blogs will be imported.");
            authors = ArrayUtils.EMPTY_STRING_ARRAY;
            urls = ArrayUtils.EMPTY_STRING_ARRAY;
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

    public String[] getUrls() {
        return urls;
    }

    public String[] getAuthors() {
        return authors;
    }

    private String[] readStrings(final Node node, final String propertyName) {
        try {
            if (node.hasProperty(propertyName)) {
                final Property property = node.getProperty(propertyName);
                final List<String> retVal = new ArrayList<>();
                if (property.isMultiple()) {
                    final Value[] values = property.getValues();
                    for (Value value : values) {
                        final String myUrl = value.getString();
                        retVal.add(myUrl);
                    }
                }
                return retVal.toArray(new String[retVal.size()]);
            }
        } catch (RepositoryException e) {
            log.error("Error reading property", e);
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }
}
