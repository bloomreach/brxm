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

import java.util.List;

public interface HippoAvailableTranslationsBean extends HippoBean {

    /**
     * It returns the available translations. 
     * @return the {@link List} of available locale's and an empty {@link List} is no locale's are present
     */
    List<String> getAvailableLocales();
    
    /**
     * @param locale
     * @return <code>true</code> when the the translation for <code>locale</code> is present
     */
    boolean hasTranslations(String locale);
    
    /**
     * @return the List of all {@link HippoBean}'s and an empty {@link List} if no translations found
     */
    List<HippoBean> getTranslations();
    
    /**
     * @param locale
     * @return returns the {@link HippoBean} for <code>locale</code> and <code>null</code> if not present
     */
    HippoBean getTranslation(String locale);
    
    /**
     * @param <T> the return type
     * @param locale the locale the translation should be in
     * @param beanMappingClass the returned type must be of <code>beanMappingClass</code>. 
     * @return A {@link HippoBean} with this <code>locale</code> and of type <code>beanMappingClass</code>. <code>null</code> if there is not {@link HippoBean} with correct locale or correct <code>beanMappingClass</code> 
     */
    <T extends HippoBean> T getTranslation(String locale, Class<T> beanMappingClass);
}
