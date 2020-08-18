/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.contentbean;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.dynamic.InterceptorEntity;
import org.hippoecm.hst.content.beans.standard.KeyLabelPathValue;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyClassification implements InterceptorEntity{

    private static final Logger log = LoggerFactory.getLogger(TaxonomyClassification.class);

    private final Locale locale;
    private final Property documentProperty;
    private final Taxonomy taxonomy;
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final String PATH_SEPERATOR = "/";

    public TaxonomyClassification(final Property documentProperty, final Taxonomy taxonomy) {
        this.documentProperty = documentProperty;
        this.taxonomy = taxonomy;
        locale = getLocale(documentProperty);
    }

    public String getTaxonomyName() {
        return taxonomy.getName();
    }

    public List<KeyLabelPathValue> getTaxonomyValues() {
        final String[] keys = getKeys();

        return Arrays.stream(keys).map(key -> {
            final Category category = taxonomy.getCategoryByKey(key);
            final String keyPath = category != null
                    ? (String.format("%s/%s/", category.getAncestors().size(), category.getPath()))
                    : null;
            final CategoryInfo categoryInfo = category.getInfo(locale);
            if (category == null || categoryInfo == null) {
                log.warn("Label is missing for key {} and locale {}", key, locale);
                return new KeyLabelPathValue(key, null, keyPath, null);
            }
            final StringBuilder labelPath = new StringBuilder().append(category.getAncestors().size())
                    .append(PATH_SEPERATOR);
            for (Category ancestorCategory : category.getAncestors()) {
                final CategoryInfo ancestorCategoryInfo = ancestorCategory.getInfo(locale);
                if (ancestorCategoryInfo == null) {
                    log.warn("Label is missing for key {} and locale {}", ancestorCategory.getKey(), locale);
                }
                labelPath.append(ancestorCategoryInfo != null ? ancestorCategoryInfo.getName() : null)
                        .append(PATH_SEPERATOR);
            }
            labelPath.append(categoryInfo.getName()).append(PATH_SEPERATOR);
            return new KeyLabelPathValue(key, categoryInfo.getName(), keyPath, labelPath.toString());
        }).collect(Collectors.toList());
    }

    private String[] getKeys() {
        try {
            if (documentProperty.isMultiple()) {
                return Arrays.stream(documentProperty.getValues()).map(this::getValue).toArray(String[]::new);
            } else {
                return new String[]{documentProperty.getString()};
            }
        } catch (RepositoryException e) {
            log.error("An error occured while retrieving taxonomy keys", e);
        }

        return new String[0];
    }

    private String getValue(final Value value) {
        try {
            return value.getString();
        } catch (RepositoryException e) {
            log.warn("Repository exception occured while getting the value of {}.", value.toString(), e);
            return null;
        }
    }

    private Locale getLocale(final Property documentProperty) {
        //Try getting the locale from HST request context, if it's available
        HstRequestContext hstRequestContext = RequestContextProvider.get();
        if (hstRequestContext != null) {
            return hstRequestContext.getPreferredLocale();
        }

        //Try from document node
        try {
            Node documentNode = documentProperty.getParent();
            if (documentNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                return new Locale(documentNode.getProperty(HippoTranslationNodeType.LOCALE).getString());
            }
        } catch (RepositoryException e) {
            log.error("Error while getting document locale", e);
        }

        return DEFAULT_LOCALE;
    }
}
