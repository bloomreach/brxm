/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.sitemapitemhandlers;

import java.util.Map;

import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;


public interface HstSiteMapItemHandlerConfiguration {

    /**
     * Returns the id for this sitemap item handler configuration. 
     * The id must be unique within the container {@link HstSiteMapItemHandlersConfiguration}, 
     * @return the id of this sitemap item handler configuration or <code>null</code> if no id set
     */
    String getId();

    /**
     * Return the name of this sitemap item handler configuration. 
     */
    String getName();

    /**
     * @return the fully-qualified class name of the class implementing the {@link HstSiteMapItemHandler} interface
     */
    String getSiteMapItemHandlerClassName();

    /**
     * @param name of the property
     * @return the value of the property or <code>null</code> when this property is not present
     */
    Object getProperty(String name);
    
    /**
     * @return the map of properties or an empty map when there are no properties
     */
    Map<String, Object> getProperties();
    
}
