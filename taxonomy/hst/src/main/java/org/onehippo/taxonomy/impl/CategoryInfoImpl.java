/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import javax.jcr.Node;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;

public class CategoryInfoImpl extends AbstractJCRService implements CategoryInfo {

    private static final long serialVersionUID = 1L;

    private String name;
    private String language;
    private String[] synonyms;
    private String description;

    private Map<String, Object> properties;

    public CategoryInfoImpl(Node jcrNode) throws ServiceException {
        super(jcrNode);

        this.language = getValueProvider().getName();
        this.name = getValueProvider().getString(TaxonomyNodeTypes.HIPPOTAXONOMY_NAME);
        this.description = getValueProvider().getString(TaxonomyNodeTypes.HIPPOTAXONOMY_DESCRIPTION);

        this.synonyms = getValueProvider().getStrings(TaxonomyNodeTypes.HIPPOTAXONOMY_SYNONYMS);

        if (synonyms == null) {
            synonyms = ArrayUtils.EMPTY_STRING_ARRAY;
        }

        this.properties = this.getValueProvider().getProperties();
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public String getDescription() {
        return description;
    }

    public String[] getSynonyms() {
        return synonyms;
    }

    public Service[] getChildServices() {
        return new Service[0];
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
