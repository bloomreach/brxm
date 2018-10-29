/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.addon.workflow.categories;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.categories.CategoriesBuilder;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoriesBuilderImpl implements CategoriesBuilder {

    private static final Logger log = LoggerFactory.getLogger(CategoriesBuilderImpl.class);

    private Node node;

    @Override
    public boolean useVersionCategories() {
       return isFrozenNode();
    }


    @Override
    public CategoriesBuilder node(final Node node) {
        this.node = node;
        return this;

    }

    @Override
    public CategoriesBuilder config(final IPluginConfig config) {
        return this;
    }

    @Override
    public CategoriesBuilder context(final IPluginContext context) {
        return this;
    }


    private boolean isFrozenNode(){
        try {
            return node.isNodeType(JcrConstants.NT_FROZEN_NODE);
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }
        return false;
    }

}
