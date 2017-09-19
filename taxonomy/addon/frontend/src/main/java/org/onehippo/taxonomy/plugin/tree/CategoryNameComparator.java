/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.taxonomy.plugin.tree;

import java.util.Comparator;
import java.util.Locale;

import org.apache.commons.lang.LocaleUtils;
import org.onehippo.taxonomy.api.Category;

/**
 * DefaultCategoryComparator.
 * <p>
 * By default, this compares two <code>Category</code> instances by its localized name.
 * </p>
 */
public class CategoryNameComparator implements Comparator<Category> {

    private Locale locale;

    /**
     * @deprecated use {@link #CategoryNameComparator(Locale)} instead
     */
    @Deprecated
    public CategoryNameComparator(final String language) {
        this(LocaleUtils.toLocale(language));
    }

    public CategoryNameComparator(final Locale locale) {
        this.locale = locale;
    }

    @Override
    public int compare(Category category1, Category category2) {
        String name1 = category1.getInfo(locale).getName();
        String name2 = category2.getInfo(locale).getName();
        return name1.compareTo(name2);
    }

}
