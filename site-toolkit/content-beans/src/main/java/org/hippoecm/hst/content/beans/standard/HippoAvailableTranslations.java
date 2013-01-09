/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.index.Indexable;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a useful bean for finding available translations of one and the same bean (folder or document). Because there is already also 
 * nodetype hippo:translation used for translating a node name, see {@link HippoTranslation}, we use for the current class a name
 * that might be unexpected wrt to its backing primary node type name, namely  'hippotranslation:translations'
 *
 * @deprecated since 2.26.01 : Use {@link AvailableTranslations} pojo which is not backed by a jcr node instead and does NOT 
 * extend from {@link HippoItem} and does not implement {@link HippoBean} at all
 */
@Deprecated

@Indexable(ignore = true)
@Node(jcrType="hippotranslation:translations")
public class HippoAvailableTranslations<K extends HippoBean> extends HippoItem implements HippoAvailableTranslationsBean<K> {

    private static final Logger log = LoggerFactory.getLogger(HippoAvailableTranslations.class);
    
    private Map<String, K> translations;
    private Class<K> beanMappingClass;
    
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
        if(translations != null) {
            return;
        }

        if (getNode() == null) {
            log.debug("Cannot get translations for detached bean.");
            return;
        }
        try {
            populateTranslations(getNode());
        } catch(RepositoryException e) {
            log.warn("Exception while trying to fetch translations.", e);
        }
    }
    
    /**
     * Sets the <code>beanMappingClass</code> for this {@link HippoAvailableTranslationsBean}. Only translations of type
     * <code>beanMappingClass</code> will be returned
     * @param beanMappingClass the type of class the available translations should be off
     */
    public void setBeanMappingClass(Class<K> beanMappingClass) {
        this.beanMappingClass = beanMappingClass;
     }

    private void populateTranslations(final javax.jcr.Node translationNode) throws RepositoryException {
        javax.jcr.Node docNode = translationNode.getParent();
        // use LinkedHashMap as we want to keep the order of the locales
        translations = new LinkedHashMap<String,K>();
        if (!docNode.hasProperty(HippoTranslationNodeType.ID)) {
            log.debug("No translations for '{}' since property '{}' not available", docNode.getPath(), HippoTranslationNodeType.ID);
            return;
        }
        String id = docNode.getProperty(HippoTranslationNodeType.ID).getString();

        String xpath = "//element(*,"+HippoTranslationNodeType.NT_TRANSLATED+")["+HippoTranslationNodeType.ID+" = '"+id+"']";

        Query query = docNode.getSession().getWorkspace().getQueryManager().createQuery(xpath, "xpath");
        final QueryResult result = query.execute();
        final NodeIterator nodeIterator = result.getNodes();
        while (nodeIterator.hasNext()) {
            javax.jcr.Node translation = nodeIterator.nextNode();
            if (translation == null) {
                continue;
            }
            if (!translation.hasProperty(HippoTranslationNodeType.LOCALE)) {
                log.debug("Skipping node '{}' because does not contain property '{}'", translation.getPath(), HippoTranslationNodeType.LOCALE);
                continue;
            }
            String locale = translation.getProperty(HippoTranslationNodeType.LOCALE).getString();
            try {
                Object bean = this.objectConverter.getObject(translation);
                if (bean != null) {
                    if (beanMappingClass != null) {
                        if(beanMappingClass.isAssignableFrom(bean.getClass())) {
                            translations.put(locale,(K) bean);
                        } else {
                            log.debug("Skipping bean of type '{}' because not of beanMappingClass '{}'", bean.getClass().getName(), beanMappingClass.getName());
                        }
                    } else {
                        translations.put(locale, (K) bean);
                    }
                }
            } catch (ObjectBeanManagerException e) {
                log.warn("Skipping bean: {}", e);
            }
        }
    }
}
