/*
 * Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.frontend.plugin.sorting;

import java.io.Serializable;
import java.util.Comparator;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.plugin.Config;

/**
 * Comparator that gets a config object.
 */
public interface IListItemComparator extends Comparator<ListItem>, Serializable {

    /**
     * @param config as used by Wicket CMS plugins
     * @deprecated use {@link IListItemComparator#setSortOptions(SortBy, SortOrder)} setSort} instead. This method will
     * be removed in version 14.0.0.
     */
    @Deprecated
    void setConfig(IPluginConfig config);

    default void setSortOptions(SortBy sortBy, SortOrder sortOrder) {
        final JavaPluginConfig javaPluginConfig = new JavaPluginConfig();

        javaPluginConfig.put(Config.SORT_BY, sortBy.toString());
        javaPluginConfig.put(Config.SORT_ORDER, sortOrder.toString());

        setConfig(javaPluginConfig);
    }
}
