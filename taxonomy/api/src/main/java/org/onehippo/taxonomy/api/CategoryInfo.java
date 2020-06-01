/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.api;

import java.util.Locale;
import java.util.Map;

public interface CategoryInfo {
    
    /**
     * @return the translated name of the {@link Category}
     */
    String getName();
    
    /**
     * @return the language of this translation
     * @deprecated use the {@link #getLocale()} to retrieve the language code from
     */
    @Deprecated
    String getLanguage();

    /**
     * @return the Locale of this translation or null if the locale cannot be determined
     */
    Locale getLocale();

    /**
     * @return the description for the {@link Category} or <code>null</code> when no description present
     */
    String getDescription();
    
    /**
     * @return a String array containing synonyms for the {@link Category} or an empty array if no synonyms configured
     */
    String[] getSynonyms();

    /**
     * Returns an unmodifiable property map.
     * <P>
     * <EM>
     *   Note: The implementation may return a lazy map which doesn't allow entry iterations,
     *   but which does allow value retrieval by keys.
     *   So, please don't depend any iteration on the returned map!
     * </EM>
     * </P>
     * @return the properties
     */
    Map<String, Object> getProperties();
    
    /**
     * Returns string property value
     * @param property property name
     * @return string property value
     */
    String getString(String property);

    /**
     * Returns string property value
     * @param property property name
     * @param defaultValue default value which is returned in case of the property value is null
     * @return the value
     */
    String getString(String property, String defaultValue);

    /**
     * Returns string array property value
     * @param property property name
     * @return string array property value
     */
    String[] getStringArray(String property);

}
