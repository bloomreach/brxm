/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.repository.utils;

import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.selection.frontend.plugin.sorting.IListItemComparator;
import org.onehippo.forge.selection.frontend.plugin.sorting.SortBy;
import org.onehippo.forge.selection.frontend.plugin.sorting.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortUtils {

    private static final Logger log = LoggerFactory.getLogger(SortUtils.class);

    public static IListItemComparator getComparator(final String sortComparator, final String sortBy,
                                                    final String sortOrder) {

        if (StringUtils.isBlank(sortComparator)) {
            return null;
        }

        IListItemComparator comparator = null;
        try {
            final Class clazz = Class.forName(sortComparator);
            final Object instance = clazz.newInstance();
            if (instance instanceof IListItemComparator) {
                comparator = (IListItemComparator) instance;
                setSortOptions(comparator, sortBy, sortOrder);
            } else {
                log.error("Configured class " + sortComparator + " does not implement IListItemComparator, using NO " +
                        "comparator");
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.error(e.getClass().getSimpleName() + " for configured class " + sortComparator
                    + ", using NO comparator");
        }

        return comparator;
    }

    private static void setSortOptions(final IListItemComparator comparator, final String sortByConfig,
                                       final String sortOrderConfig) {

        final SortBy sortBy = StringUtils.isNotBlank(sortByConfig)
                ? SortBy.valueOf(sortByConfig)
                : SortBy.label;

        final SortOrder sortOrder = StringUtils.isNotBlank(sortOrderConfig)
                ? SortOrder.valueOf(sortOrderConfig)
                : SortOrder.ascending;

        comparator.setSortOptions(sortBy, sortOrder);
    }

    // no instantiation
    private SortUtils() {
    }
}
