/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.taxonomy.api;


import java.io.Serializable;

import org.onehippo.taxonomy.api.Taxonomies;

public interface TaxonomyManager extends Serializable {

    /**
     * Returns a Taxonomies object managed by this TaxonomyManager
     * @return {@link Taxonomies} object for this TaxonomyManager
     */
    Taxonomies getTaxonomies();

    /**
     * 
     * @return the absolute content path to the taxonomies node container
     */
    String getTaxonomiesContentPath();

    /**
     * invalidates the (some) part of the Taxonomies.
     * @param path 
     */
    void invalidate(String path);
}
