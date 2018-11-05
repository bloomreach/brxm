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
    protected IPluginContext context;

    @Override
    public CategoriesBuilder node(final Node node) {
        Objects.requireNonNull(node);
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
    public CategoriesBuilder context(final IPluginContext context) {
        Objects.requireNonNull(context);
        this.context = context;
        return this;
    }

    protected boolean isFrozenNode(final Node node) {
        assert node != null;
        return isType(node, NT_FROZEN_NODE);
    }

    protected boolean isHandle(final Node node) {
        assert node != null;
        return isType(node, NT_HANDLE);
    }

    private boolean isType(final Node node, final String type) {
        assert node != null;
        assert type != null;
        try {
            return node.isNodeType(type);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(String.format("Could not determine nodetype of node:{path:{%s}}", JcrUtils.getNodePathQuietly(node)), e);
        }
    }
}
