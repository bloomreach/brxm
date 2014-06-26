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

import org.onehippo.cms7.essentials.dashboard.utils.DocumentTemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdditionalTaxonomyBuilder {
    private static Logger log = LoggerFactory.getLogger(AdditionalTaxonomyBuilder.class);
    public static final String DAO = "taxonomy.classification.dao";
    private Session session;
    private Node node;
    private String taxonomyClassificationDao;
    private String taxonomyName;
    private String documentType;
    private String prefix;
    private Node parent;

    public void setSession(final Session session) {
        this.session = session;
    }

    public void setTaxonomyName(final String taxonomyName) {
        this.taxonomyName = taxonomyName;
    }

    public void setTaxonomyClassificationDao(final String taxonomyClassificationDao) {
        this.taxonomyClassificationDao = taxonomyClassificationDao;
    }

    public Node getNode() {
        return node;
    }

    public void build(){
        try {
            String name = ServiceNameBuilder.getParentPath(prefix, documentType);
            parent = session.getNode(name);
            node = parent.addNode(taxonomyName, "frontend:plugin");
            node.setProperty("mode","${mode}");
            node.setProperty("model.compareTo","${model.compareTo}");
            node.setProperty("taxonomy.id","service.taxonomy");
            node.setProperty("taxonomy.name",taxonomyName);
            node.setProperty("wicket.id", "${cluster.id}.left.item");
            node.setProperty("plugin.class","org.onehippo.taxonomy.plugin.TaxonomyPickerPlugin");
            node.setProperty(DAO,taxonomyClassificationDao);
            node.setProperty("wicket.model","${wicket.model}");

        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
    }


}
