/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.model;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.ITaxonomyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyModel extends LoadableDetachableModel<Taxonomy> {

    static final Logger log = LoggerFactory.getLogger(TaxonomyModel.class);

    private final IPluginContext context;
    private final IPluginConfig config;
    private final String serviceId;
    private String taxonomyName;
    private final JcrNodeModel taxonomyDocumentNodeModel;

    public TaxonomyModel(IPluginContext context, IPluginConfig config) {
        this(context, config, null, null);
    }

    public TaxonomyModel(IPluginContext context, IPluginConfig config, String serviceId, String taxonomyName) {
        this(context, config, serviceId, taxonomyName, null);
    }

    public TaxonomyModel(IPluginContext context, IPluginConfig config, String serviceId, String taxonomyName, JcrNodeModel taxonomyDocumentNodeModel) {
        this.context = context;
        this.config = config;

        if (serviceId != null) {
            this.serviceId = serviceId;
        } else {
            this.serviceId = config.getString(ITaxonomyService.SERVICE_ID, ITaxonomyService.DEFAULT_SERVICE_TAXONOMY_ID);
        }

        if (taxonomyDocumentNodeModel != null) {
            try {
                this.taxonomyName = taxonomyDocumentNodeModel.getNode().getName();
            } catch (RepositoryException e) {
                log.error("Failed to get the taxonomy variant node name.", e);
                this.taxonomyName = taxonomyName;
            }
        } else if (taxonomyName != null) {
            this.taxonomyName = taxonomyName;
        } else {
            this.taxonomyName = config.getString(ITaxonomyService.TAXONOMY_NAME);
        }

        this.taxonomyDocumentNodeModel = taxonomyDocumentNodeModel;
    }

    public IPluginContext getPluginContext() {
        return context;
    }

    public IPluginConfig getPluginConfig() {
        return config;
    }

    @Override
    protected Taxonomy load() {
        if (taxonomyDocumentNodeModel != null) {
            return context.getService(serviceId, ITaxonomyService.class).getTaxonomy(taxonomyDocumentNodeModel.getNode());
        } else {
            if (StringUtils.isBlank(taxonomyName)) {
                log.info("No configured/chosen taxonomy name. Found '{}'", taxonomyName);
                return null;
            }

            return context.getService(serviceId, ITaxonomyService.class).getTaxonomy(taxonomyName);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TaxonomyModel) {
            TaxonomyModel that = (TaxonomyModel) obj;
            return StringUtils.equals(that.serviceId, serviceId) && StringUtils.equals(that.taxonomyName, taxonomyName);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return StringUtils.defaultString(serviceId).hashCode() ^ StringUtils.defaultString(taxonomyName).hashCode() ^ 8767243;
    }

    @Override
    protected void onDetach() {
        if (taxonomyDocumentNodeModel != null) {
            taxonomyDocumentNodeModel.detach();
        }
    }
}
