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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
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

public class TaxonomyClassification implements InterceptorEntity {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyClassification.class);

    private final Locale locale;
    private final Property documentProperty;
    private final Taxonomy taxonomy;
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final String PATH_SEPARATOR = "/";

    public TaxonomyClassification(final Property documentProperty, final Taxonomy taxonomy) {
        this.documentProperty = documentProperty;
        this.taxonomy = taxonomy;
        locale = getLocale(documentProperty);
    }

    public String getTaxonomyName() {
        return taxonomy.getName();
    }

    public List<KeyLabelPathValue> getTaxonomyValues() {
        return getKeyLabelPathValues(getKeys());
    }

    public List<KeyLabelPathValue> getTaxonomyAllValues() {
        return getKeyLabelPathValues(getEffectiveKeys(getKeys()));
    }

    private List<KeyLabelPathValue> getKeyLabelPathValues(final String[] keys) {
        return Arrays.stream(keys)
                .filter(StringUtils::isNotBlank)
                .map(key -> {
                    final Category category = taxonomy.getCategoryByKey(key);
                    if (category == null) {
                        log.warn("Could not locate category for key '{}'", key);
                        return new KeyLabelPathValue(key, null, null, null);
                    }

                    final String keyPath = (String.format("%s/%s/", category.getAncestors().size(), category.getPath()));
                    final CategoryInfo categoryInfo = category.getInfo(locale);
                    if (categoryInfo == null) {
                        log.warn("Label is missing for key '{}' and locale '{}'", key, locale);
                        return new KeyLabelPathValue(key, null, keyPath, null);
                    }

                    final StringBuilder labelPath = new StringBuilder().append(category.getAncestors().size())
                            .append(PATH_SEPARATOR);
                    for (Category ancestorCategory : category.getAncestors()) {
                        final CategoryInfo ancestorCategoryInfo = ancestorCategory.getInfo(locale);
                        if (ancestorCategoryInfo == null) {
                            log.warn("Label is missing for key '{}' and locale '{}'", ancestorCategory.getKey(), locale);
                        }
                        labelPath.append(ancestorCategoryInfo != null ? ancestorCategoryInfo.getName() : null)
                                .append(PATH_SEPARATOR);
                    }
                    labelPath.append(categoryInfo.getName()).append(PATH_SEPARATOR);
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

    /**
     * Creates an array consisting of the input keys plus all their ancestors keys. For each assigned key, find its
     * ancestors and add them to the returned array. A distinct() is done before returning the array with all the keys
     *
     * @param keys an array with the assigned keys of a document field
     * @return an array with all the assigned keys plus all their ancestor keys
     */
    private String[] getEffectiveKeys(final String[] keys) {
        return Stream.concat(
                Arrays.stream(keys),
                Arrays.stream(keys).map(taxonomy::getCategoryByKey).filter(Objects::nonNull)
                        .flatMap(category -> category.getAncestors().stream().map(Category::getKey)))   
                .distinct()
                .toArray(String[]::new);
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
