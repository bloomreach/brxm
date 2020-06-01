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

import java.util.Collections;
import java.util.List;

public interface HippoAvailableTranslationsBean<K extends HippoBean> {

    /**
     * Returns the available translations.
     * @return the {@link List} of available locales and an empty {@link List} is no locales are present
     */
    List<String> getAvailableLocales();
    
    /**
     * @param locale
     * @return <code>true</code> when the translation for <code>locale</code> is present
     */
    boolean hasTranslation(String locale);
    
    /**
     * @return the {@link List} of all translations or an empty {@link List} if no translations found
     * @throws ClassCastException when the bean for <code>locale</code> cannot be cast to <code>K</code>
     */
    List<K> getTranslations() throws ClassCastException;

    /**
     * @param locale the locale for the translation
     * @return returns translation for <code>locale</code> and <code>null</code> if not present
     * @throws ClassCastException when the bean for <code>locale</code> cannot be cast to <code>K</code>
     */
     K getTranslation(String locale) throws ClassCastException;

     /**
      * A No-operation instance of a HippoAvailableTranslationsBean 
      */
     final static class NoopTranslationsBean<K extends HippoBean> implements HippoAvailableTranslationsBean<K> {

        public List<String> getAvailableLocales() {
            return Collections.EMPTY_LIST;
        }

        public K getTranslation(String locale) throws ClassCastException {
            return null;
        }

        public List<K> getTranslations() throws ClassCastException {
            return null;
        }

        public boolean hasTranslation(String locale) {
            return false;
        }
         
     }
}
