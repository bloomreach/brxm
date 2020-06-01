/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.util.TaxonomyUtil;

public class CategoryInfoImpl implements CategoryInfo {

    private String name;
    private Locale locale;
    private String[] synonyms;
    private String description;

    private Map<String, Object> properties;

	public CategoryInfoImpl(Node jcrNode) {

        // Use a ValueProvider, and make sure to clean it up
        final JCRValueProviderImpl jvp = new JCRValueProviderImpl(jcrNode);
        this.locale = TaxonomyUtil.toLocale(jvp.getName());
        this.name = jvp.getString(TaxonomyNodeTypes.HIPPOTAXONOMY_NAME);
        this.description = jvp.getString(TaxonomyNodeTypes.HIPPOTAXONOMY_DESCRIPTION);

        this.synonyms = jvp.getStrings(TaxonomyNodeTypes.HIPPOTAXONOMY_SYNONYMS);

        if (synonyms == null) {
            synonyms = ArrayUtils.EMPTY_STRING_ARRAY;
        }

        this.properties = jvp.getProperties();

        jvp.detach();
	}

    public String getName() {
        return name;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public String getDescription() {
        return description;
    }

    public String[] getSynonyms() {
        return synonyms;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public String getString(String property) {
        return (String) properties.get(property);
    }

    public String getString(String property, String defaultValue) {
        String value = getString(property);
        return (value != null ? value : defaultValue);
    }

    public String[] getStringArray(String property) {
        return (String[]) properties.get(property);
    }
}
