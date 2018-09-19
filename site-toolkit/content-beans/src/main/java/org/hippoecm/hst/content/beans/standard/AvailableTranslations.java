/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.service.translation.HippoTranslationBeanService;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a useful POJO/BEAN (non jcr node backed bean, not extending {@link HippoItem} or implementing {@link HippoBean})
 * for finding available translations of one and the same bean (folder or document). 
 *
 */

public class AvailableTranslations<K extends HippoBean> implements HippoAvailableTranslationsBean<K> {

    private static final Logger log = LoggerFactory.getLogger(AvailableTranslations.class);

    final private Node node;
    private Map<String, K> translations;
    private Class<K> beanMappingClass;

    public AvailableTranslations(Node node) {
        this.node = node;
    }

    public List<String> getAvailableLocales() {
        populate();
        return new ArrayList<String>(translations.keySet());
    }

    public K getTranslation(String locale) {
        populate();
        return translations.get(locale);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<K> getTranslations() {
        populate();
        return new ArrayList(translations.values());
    }

    public boolean hasTranslation(String locale) {
        populate();
        return translations.get(locale) != null;
    }

    @SuppressWarnings("unchecked")
    private void populate() {
        if (translations != null) {
            return;
        }
        try {
            populateTranslations();
        } catch (RepositoryException e) {
            log.warn("Exception while trying to fetch translations.", e);
        }
    }

    /**
     * Sets the <code>beanMappingClass</code> for this {@link org.hippoecm.hst.content.beans.standard.HippoAvailableTranslationsBean}. Only translations of type
     * <code>beanMappingClass</code> will be returned
     * @param beanMappingClass the type of class the available translations should be off
     */
    public void setBeanMappingClass(Class<K> beanMappingClass) {
        this.beanMappingClass = beanMappingClass;
    }

    private void populateTranslations() throws RepositoryException {
        if (!node.hasProperty(HippoTranslationNodeType.ID)) {
            log.debug("No translations for '{}' since property '{}' not available", node.getPath(),
                    HippoTranslationNodeType.ID);
            translations = Collections.emptyMap();
            return;
        }

        final HippoTranslationBeanService translationBeanService = HstServices.getComponentManager()
                .getComponent(HippoTranslationBeanService.class.getName());

        String translationId = node.getProperty(HippoTranslationNodeType.ID).getString();
        translations = translationBeanService.getTranslationBeans(node.getSession(), translationId, beanMappingClass);
    }
}
