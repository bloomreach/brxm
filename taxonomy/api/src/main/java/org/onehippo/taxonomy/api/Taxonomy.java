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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public interface Taxonomy extends Serializable {

    /**
     * 
     * @return the name of this taxonomy
     */
    String getName();

    /**
     * @return the supported locales for each category from this taxonomy
     * @deprecated use {@link #getLocaleObjects()} instead
     */
    @Deprecated
    String [] getLocales();

    List<Locale> getLocaleObjects();

    /**
     * Implementation should return an unmodifiable List as runtime {@link HstComponent}'s should not be able to modify this list. This also
     * holds in case of an empty list.
     * @return List containing all {@link Category} directly below the root {@link Taxonomy}. If none found, an empty List is returned
     */
    List<? extends Category> getCategories();

    /**
     * Implementation should return an unmodifiable List as runtime {@link HstComponent}'s should not be able to modify this list. This also
     * holds in case of an empty list.
     * @return  List containing all {@link Category} descendants below the root (tree as flat list).  If none found, an empty List is returned
     */
    List<? extends Category> getDescendants();

    /**
     * @param relPath the relative path to the Taxonomy root
     * @return return the {@link Category} for this relPath, or <code>null</code> if none found
     */
    Category getCategory(String relPath);

    /**
     * @param key
     * @return return the {@link Category} for this key, or <code>null</code> if none found
     */
    Category getCategoryByKey(String key);

}
