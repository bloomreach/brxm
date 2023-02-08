/*
 * Copyright 2018-2023 Bloomreach
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

import javax.jcr.Node;

import org.hippoecm.frontend.plugin.IPluginContext;

public abstract class AbstractCategoriesService implements CategoriesService {

    @Override
    public String[] getCategories(final Node node, final IPluginContext context, final String[] workflowCategories, final String[] versionCategories) {
        return getCategoriesBuilder()
                .node(node)
                .context(context)
                .versionCategories(versionCategories)
                .workflowCategories(workflowCategories)
                .build();
    }

    protected abstract CategoriesBuilder getCategoriesBuilder();
}
