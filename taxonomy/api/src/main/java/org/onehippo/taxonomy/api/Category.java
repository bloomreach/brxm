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

import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface Category {

    /**
     * 
     * @return the key for this category
     */ 
    String getKey();
    
    /**
     * 
     * @return the path of this category below the {@link Taxonomy}, consisting of the names of the ancestor
     *         categories separated by '/'.
     */
    String getPath();
    
    /**
     * 
     * @return the name of this category
     */
    String getName();

    /**
     * Implementation may return an unmodifiable List as runtime {@link HstComponent}'s should not be able to modify this list.This also
     * holds in case of an empty list.
     * @return List containing all {@link Category} directly below this {@link Category}. If none found, an empty List is returned
     */
    List<? extends Category> getChildren();
    
    /**
     * 
     * @return the parent Category or <code>null</code> when there is no parent item
     */
    Category getParent();
    
    /**
     * 
     * @return ordered list of ancestors where the top ancestor is the first in the list
     */
    List<? extends Category> getAncestors();
    
    /**
     * @param language
     * @return translation for the specified language
     * @deprecated use {@link #getInfo(Locale)} instead.
     */
    @Deprecated
    CategoryInfo getInfo(String language);

    /**
     * Get the Category Info for a specific Locale.
     *
     * @param locale the Locale you want the information for.
     * @return translation of a Category for the requested Locale
     */
    CategoryInfo getInfo(Locale locale);

    /**
     * @return returns unmodifiable translations map keyed by language names.
     * @deprecated use {@link #getInfosByLocale()} instead
     */
    @Deprecated
    Map<String, ? extends CategoryInfo> getInfos();

    /**
     * @return returns unmodifiable translations map keyed by Locales.
     */
    Map<Locale, ? extends CategoryInfo> getInfosByLocale();

    /**
     * 
     * @return the {@link Taxonomy} that contains this item
     */
    Taxonomy getTaxonomy();
}
