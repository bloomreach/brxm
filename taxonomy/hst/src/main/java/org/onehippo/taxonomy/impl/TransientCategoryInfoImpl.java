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
package org.onehippo.taxonomy.impl;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.util.TaxonomyUtil;

/**
 * Used for the category which is not yet translated so doesn't have category info.
 * @version $Id$
 */
public class TransientCategoryInfoImpl implements CategoryInfo {

    private String name;
    private Locale locale;
    private String description;
    private String [] synonyms;
    private Map<String, Object> properties;

    public TransientCategoryInfoImpl(Category category) {
        this.name = category.getName();
    }

    public String getName() {
        return name;
    }

    /**
     * @deprecated use {@link #getLocale()} to retrieve the language code from
     */
    @Deprecated
    public String getLanguage() {
        return getLocale().getLanguage();
    }

    /**
     * @deprecated use {@link #setLocale(Locale)} to set the language code
     */
    @Deprecated
    public void setLanguage(String language) {
        this.locale = TaxonomyUtil.toLocale(language);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getSynonyms() {
        if (synonyms == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        return synonyms;
    }

    public void setSynonyms(String[] synonyms) {
        this.synonyms = synonyms;
    }

    public Map<String, Object> getProperties() {
        if (properties == null) {
            return Collections.emptyMap();
        }

        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getString(String property) {
        return (String) getProperties().get(property);
    }

    public String getString(String property, String defaultValue) {
        String value = getString(property);
        return (value != null ? value : defaultValue);
    }

    public String[] getStringArray(String property) {
        return (String []) getProperties().get(property);
    }

}
