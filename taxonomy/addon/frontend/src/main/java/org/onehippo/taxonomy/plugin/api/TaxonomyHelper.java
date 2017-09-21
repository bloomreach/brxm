/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.api;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.LocaleUtils;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;

/**
 * Helper class with common operations on a taxonomy tree.
 */
public final class TaxonomyHelper {

    private TaxonomyHelper() {
    }

    public static String getCategoryName(final Category category, final Locale locale) {
        if (locale != null) {
            final List<Locale.LanguageRange> documentLocale = Locale.LanguageRange.parse(locale.toLanguageTag());
            final Map<Locale, ? extends CategoryInfo> availableTranslationsMap = category.getInfosByLocale();
            final Locale matchingLocale = Locale.lookup(documentLocale, availableTranslationsMap.keySet());
            if (matchingLocale != null) {
                return availableTranslationsMap.get(matchingLocale).getName();
            }
        }
        return category.getName();
    }

    /**
     * @deprecated use {@link #getCategoryName(Category, Locale)} instead
     */
    @Deprecated
    public static String getCategoryName(Category category, String language) {
        return getCategoryName(category, LocaleUtils.toLocale(language));
    }

}
