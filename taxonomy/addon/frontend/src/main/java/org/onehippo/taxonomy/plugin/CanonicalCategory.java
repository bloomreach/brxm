/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin;

import java.io.Serializable;
import java.util.Locale;

import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;

public class CanonicalCategory implements Serializable {

    private final Taxonomy taxonomy;

    private final String key;
    private final String language;

    public CanonicalCategory(Taxonomy taxonomy, String key, Locale locale) {
        this(taxonomy, key, locale.getLanguage());
    }

    public CanonicalCategory(Taxonomy taxonomy, String key, String language) {
        this.taxonomy = taxonomy;
        this.key = key;
        this.language = language;
    }

    public String getName() {
        Category category = taxonomy.getCategoryByKey(key);
        if (category != null) {
            return TaxonomyHelper.getCategoryName(category, language);
        }
        return null;
    }

    public String getKey() {
        return key;
    }
}