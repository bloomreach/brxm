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
package org.hippoecm.hst.core.request;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

/**
 * Implementations of this interface are a request flyweight instance of the {@link SiteMount} object, where possible wildcard property placeholders have been filled in, similar
 * to the {@link ResolvedSiteMapItem} and {@link HstSiteMapItem}
 */
public interface ResolvedSiteMount {

    /**
     * @return the {@link ResolvedVirtualHost} for this ResolvedSiteMount
     */
    ResolvedVirtualHost getResolvedVirtualHost();
    
    /**
     * @return the backing request independent {@link SiteMount} item for this resolvedSiteMount instance
     */
    SiteMount getSiteMount();

    /**
     * matches the current request, {@link ResolvedVirtualHost} and {@link ResolvedSiteMount} to a {@link ResolvedSiteMapItem} item or <code>null</code> when
     * it cannot be matched
     * @param request
     * @return the ResolvedSiteMapItem for the current request
     * @throws MatchException 
     */
    ResolvedSiteMapItem matchSiteMapItem(HttpServletRequest request) throws MatchException;
    
}
