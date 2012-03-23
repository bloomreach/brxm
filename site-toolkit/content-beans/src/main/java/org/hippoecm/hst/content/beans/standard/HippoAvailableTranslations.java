/*
 *  Copyright 2010 Hippo.
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

import org.hippoecm.hst.content.beans.Node;

/**
 * This is a useful bean for finding available translations of one and the same bean (folder or document). Because there is already also 
 * nodetype hippo:translation used for translating a node name, see {@link HippoTranslation}, we use for the current class a name
 * that might be unexpected wrt to its backing primary node type name, namely  'hippotranslation:translations'
 *
 */
@Node(jcrType="hippotranslation:translations")
public class HippoAvailableTranslations<K extends HippoBean> extends HippoItem implements HippoAvailableTranslationsBean<K> {

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
        // use LinkedHashMap as we want to keep the order of the locales
        translations = new LinkedHashMap<String,K>();
        List<K> childBeans ;
        childBeans = getChildBeans(beanMappingClass);
        for(K child : childBeans) {
            // the child name is the locale
            translations.put(child.getName(), child);
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
}
