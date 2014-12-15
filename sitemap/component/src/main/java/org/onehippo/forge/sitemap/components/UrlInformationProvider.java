/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.forge.sitemap.components.model.ChangeFrequency;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Classes that extend this interface can be used to map your own domain model on the sitemap.org protocol domain model.
 * Make sure that your implemented methods are thread-safe! Multiple threads will access your methods at the same time.
 */
public interface UrlInformationProvider {
    /**
     * Returns the last modified date of this document
     * @param hippoBean the document to return the last modified date for
     * @return the last modified date
     */
    Calendar getLastModified(HippoBean hippoBean);

    /**
     * Returns the sitmap priority of this document
     * @param hippoBean the document to return the priority for
     * @return the sitemap priority
     */
    BigDecimal getPriority(HippoBean hippoBean);

    /**
     * Returns the change frequency of this document
     * @param hippoBean the document to return the change frequency for
     * @return the change frequency
     */
    ChangeFrequency getChangeFrequency(HippoBean hippoBean);

    /**
     * Returns the url for this document
     * @param hippoBean the document to create the url for
     * @param requestContext the current request context
     * @return the string representation of the url for this document
     */
    String getLoc(HippoBean hippoBean, HstRequestContext requestContext);

    /**
     * Returns the url for this document
     * @param hippoBean the document to create the url for
     * @param requestContext the current request context
     * @param mount the mount to create the link under
     * @return the string representation of the url for this document
     */
    String getLoc(HippoBean hippoBean, HstRequestContext requestContext, Mount mount);

    /**
     * Indicates whether this document should be included in the site map
     *
     * @param hippoBean the document
     * @return <code>true</code> if the document should be included, <code>false</code> otherwise
     */
    boolean includeDocumentInSiteMap(HippoBean hippoBean);

    /**
     * Indicates whether the child site map items for this document should be included in the site map.
     * This happens when a default/any matcher site map item has child site map items. This method indicates whether
     * those child site map items should also be processed.
     *
     * @param hippoBean the document
     * @return <code>true</code> if the child site map items of this document should be included, <code>false</code>
     * otherwise
     */
    boolean includeChildrenInSiteMap(HippoBean hippoBean);
}
