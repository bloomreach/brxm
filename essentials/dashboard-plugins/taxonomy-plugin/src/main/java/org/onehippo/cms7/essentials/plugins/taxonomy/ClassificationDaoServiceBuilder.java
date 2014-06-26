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
package org.onehippo.cms7.essentials.plugins.taxonomy;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class ClassificationDaoServiceBuilder {
    public static final String FIELD_PATH = "fieldPath";
    public static final String PLUGIN_CLASS = "plugin.class";
    public static final String TAXONOMY_CLASSIFICATION_DAO = "taxonomy.classification.dao";
    private Session session;
    private String fieldPath;
    private String taxonomyClassificationDao;
    private String classificationDaoServiceName;
    private Node node;

    public void setSession(final Session session) {
        this.session = session;
    }

    public void setFieldPath(final String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public void setTaxonomyClassificationDao(final String taxonomyClassificationDao) {
        this.taxonomyClassificationDao = taxonomyClassificationDao;
    }

    public void setClassificationDaoServiceName(final String classificationDaoServiceName) {
        this.classificationDaoServiceName = classificationDaoServiceName;
    }

    public void build() {
        try {
            Node parent = session.getNode("/hippo:configuration/hippo:frontend/cms/cms-services");
            node = parent.addNode(classificationDaoServiceName,"frontend:plugin");
            node.setProperty(PLUGIN_CLASS, "org.onehippo.taxonomy.plugin.MixinClassificationDaoPlugin");
            node.setProperty(FIELD_PATH,fieldPath);
            node.setProperty(TAXONOMY_CLASSIFICATION_DAO, taxonomyClassificationDao);

        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public Node getNode() {
        return node;
    }



}
