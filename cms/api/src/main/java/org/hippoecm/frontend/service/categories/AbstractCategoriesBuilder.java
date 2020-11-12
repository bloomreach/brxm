/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.hippoecm.frontend.service.categories;

import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.RepositoryRuntimeException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.repository.util.JcrUtils;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public abstract class AbstractCategoriesBuilder implements CategoriesBuilder {
    protected Node node;
    protected String[] versionCategories;
    protected String[] workflowCategories;
    protected String[] xpageCategories;
    protected IPluginContext context;

    @Override
    public CategoriesBuilder node(final Node node) {
        Objects.requireNonNull(node);
        this.node = node;
        return this;
    }

    @Override
    public CategoriesBuilder versionCategories(final String[] versionCategories) {
        Objects.requireNonNull(versionCategories);
        this.versionCategories = versionCategories;
        return this;
    }

    @Override
    public CategoriesBuilder workflowCategories(final String[] workflowCategories) {
        Objects.requireNonNull(workflowCategories);
        this.workflowCategories = workflowCategories;
        return this;
    }

    @Override
    public CategoriesBuilder xpageCategories(final String[] xpageCategories) {
        Objects.requireNonNull(xpageCategories);
        this.xpageCategories = xpageCategories;
        return this;
    }

    @Override
    public CategoriesBuilder context(final IPluginContext context) {
        Objects.requireNonNull(context);
        this.context = context;
        return this;
    }

    protected boolean isFrozenNode(final Node node) {
        return isType(node, NT_FROZEN_NODE);
    }

    protected boolean isXpage(final Node node) {
        if (!isHandle(node)) {
            return false;
        }

        try {
            final Node variant = node.getNode(node.getName());
            return isType(variant, "hst:xpagemixin");
        } catch (RepositoryException e) {
            final String errorMessage = String.format("Could not determine if node:{path:{%s}} is type xpage",
                    JcrUtils.getNodePathQuietly(node));
            throw new RepositoryRuntimeException(errorMessage, e);
        }
    }


    protected boolean isHandle(final Node node) {
        return isType(node, NT_HANDLE);
    }

    private boolean isType(final Node node, final String type) {
        try {
            return node.isNodeType(type);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(String.format("Could not determine nodetype of node:{path:{%s}}",
                    JcrUtils.getNodePathQuietly(node)), e);
        }
    }
}
