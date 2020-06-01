/*
 * Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.onehippo.forge.selection.hst.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoAvailableTranslationsBean;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.forge.selection.hst.contentbean.ValueList;
import org.onehippo.forge.selection.hst.contentbean.ValueListItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default value list manager has a map of value list identifiers with corresponding locations (JCR paths)
 */
public class DefaultValueListManager implements ValueListManager {

    /** Logger */
    private static final Logger log = LoggerFactory.getLogger(DefaultValueListManager.class);

    /** Map from documentField to location relative to site content base bean. */
    private final Map<String, String> documentFieldLocationMapping;

    /**
     * Map of static options map, each entry of which is keyed by an identifier and valued
     * by a static options (id - text pairs) map.
     */
    private Map<String, Map<String, String>> mapOfStaticOptionsMap;

    public DefaultValueListManager(Map<String, String> documentFieldLocationMapping) {
        this.documentFieldLocationMapping = documentFieldLocationMapping;
    }

    /** {@inheritDoc} */
    @Override
    public ValueList getValueList(final HippoBean siteContentBaseBean, final String identifier) {
        return getValueList(siteContentBaseBean, identifier, null/*locale*/);
    }

    /** {@inheritDoc} */
    @Override
    public ValueList getValueList(final HippoBean hippoBean, final String identifier, final Locale locale) {
        String location = documentFieldLocationMapping.get(identifier);

        if (location == null) {
            log.warn("No value list location found by identifier {}, please adjust bootstrapping configuration", identifier);
            return null;
        }

        // the value list as configured by absolute or relative path
        ValueList valueList;
        if (location.startsWith("/")) {
            log.debug("Reading value list from absolute location {}", location);
            Object o = null;
            try {
                o = hippoBean.getObjectConverter().getObject(hippoBean.getNode().getSession(), location);
            } catch (ObjectBeanManagerException | RepositoryException e) {
                log.warn(e.getMessage(), e);
            }
            valueList = (o instanceof ValueList) ? (ValueList) o : null;
        } else {
            log.debug("Reading value list from relative location {} from base bean {}", location, hippoBean.getPath());
            valueList = hippoBean.getBean(location, ValueList.class);
        }

        // check availability of translations in a preferred locale
        if ((locale != null) && (valueList != null)){
            HippoAvailableTranslationsBean<ValueList> valueListTranslations = valueList.getAvailableTranslations(ValueList.class);
            if (valueListTranslations != null) {
                // first try full locale, then just the language
                if (valueListTranslations.hasTranslation(locale.toString())) {
                    valueList = valueListTranslations.getTranslation(locale.toString());
                }
                else if (valueListTranslations.hasTranslation(locale.getLanguage())) {
                    valueList = valueListTranslations.getTranslation(locale.getLanguage());
                }
            }
        }

        log.debug("Returning value list {}, locale is {}", ((valueList == null) ? "null" : valueList.getPath()), locale);
        return valueList;
    }

    /** {@inheritDoc} */
    @Override
    public ValueListItem getValueListItem(final HippoBean siteContentBaseBean, final String identifier, final String key) {
        return getValueListItem(siteContentBaseBean, identifier, null/*locale*/, key);
    }

    /** {@inheritDoc} */
    @Override
    public ValueListItem getValueListItem(final HippoBean siteContentBaseBean, final String identifier, final Locale locale, final String key) {
        ValueList valueList = getValueList(siteContentBaseBean, identifier, locale);

        if (valueList == null) {
            return null;
        }

        if (valueList.getItems() != null) {
            for (ValueListItem listItem : valueList.getItems()) {
                if (listItem.getKey().equals(key)) {
                    return listItem;
                }
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getValueListIdentifiers() {
        if (documentFieldLocationMapping == null) {
            log.debug("Returning empty list of value list identifiers");
            return Collections.emptyList();
        }
        final List<String> identifiers = Collections.unmodifiableList(new ArrayList<>(documentFieldLocationMapping.keySet()));
        log.debug("Returning list of value list identifiers: {}", identifiers);
        return identifiers;
    }


    /**
     * Getter for mappings between documentField and  location (relative to site content base bean.)
     * @return empty map if no mappings, an unmodifiable map otherwise
     */
    public Map<String, String> getDocumentFieldLocationMapping() {
        if (documentFieldLocationMapping == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(documentFieldLocationMapping);
    }

    public Map<String, Map<String, String>> getMapOfStaticOptionsMap() {
        return mapOfStaticOptionsMap;
    }

    public void setMapOfStaticOptionsMap(Map<String, Map<String, String>> mapOfStaticOptionsMap) {
        this.mapOfStaticOptionsMap = mapOfStaticOptionsMap;
    }

    public Map<String, String> getStaticOptionsMap(final String identifier) {
        Map<String, String> optionsMap = null;

        if (mapOfStaticOptionsMap != null) {
            optionsMap = mapOfStaticOptionsMap.get(identifier);
        }

        if (optionsMap != null) {
            return Collections.unmodifiableMap(optionsMap);
        }

        return Collections.emptyMap();
    }

}
