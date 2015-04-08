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

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface TaxonomyManager extends Serializable {

    /**
     * Returns a Taxonomies object managed by this TaxonomyManager
     * <p><strong>BEWARE: this implementation is going te be removed in near future</strong></p>
     * @return {@link Taxonomies} object for this TaxonomyManager
     * @deprecated use {@link TaxonomyManager#getTaxonomies(javax.jcr.Session)} or
     *   {@link TaxonomyManager#getTaxonomies(org.hippoecm.hst.core.request.HstRequestContext)}
     *
     */
    @Deprecated
    Taxonomies getTaxonomies();


    /**
     * Returns a Taxonomies object managed by this TaxonomyManager for specific user session
     *
     * @param session JCR session
     * @return {@link Taxonomies} object for this TaxonomyManager
     */
    Taxonomies getTaxonomies(Session session);


    /**
     * Same as {@link TaxonomyManager#getTaxonomies(javax.jcr.Session)}, uses liveuser session @{context.getSession()}
     * @param context  HstRequestContext instance
     * @return {@link Taxonomies} object for this TaxonomyManager
     */
    Taxonomies getTaxonomies(HstRequestContext context);

    /**
     * Taxonomy absolute content (container) path
     *
     * @return the absolute content path to the taxonomies node container
     */
    String getTaxonomiesContentPath();

    /**
     * invalidates the (some) part of the Taxonomies.
     *
     * @param path taxonomy path
     */
    void invalidate(String path);
}
