/*
 *  Copyright 2008 Hippo.
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

import org.hippoecm.hst.configuration.HstSite;

/**
 * Implementations should be able to match a path (pathInfo) in combination with a <code>{@link HstSite}</code> object to a
 * <code>{@link ResolvedSiteMapItem}</code>. Typically a <code>ResolvedSiteMapItem</code> is a request context based instance of a
 * <code>{@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem}</code>, where possibly property placeholders in <code>{@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem#getParameters()}</code>
 * are replaced by their request context sensitive values. For example, a ${1} parameter value might be replaced by the value that matched
 * to the first <code>HstSiteMapItem</code> containing a wildcard. 
 * <p/>
 * Note: the {@link ResolvedSiteMapItem} is accessible by every <code>HstComponent</code> instance through the <code>HstRequestContext</code>
 * <p/>
 * When no <code>HstSiteMapItem</code> can be found to match the <code>pathInfo</code>, the implementation can return null for the 
 * {@link #match(String, HstSite)}, but is also allowed to return some catch all <code>ResolvedSiteMapItem</code>
 */
public interface HstSiteMapMatcher {
    
    /**
     * method to match the pathInfo for the hstSite to a <code>HstSiteMapItem</code> if possible.
     * 
     * @param pathInfo the pathInfo that should be matched in the <code>HstSiteMapItem</code> tree
     * @param resolvedSiteMount the current <code>ResolvedSiteMount</code> that must matches the request serverName and pathInfo
     * @return a ResolvedSiteMapItem and <code>null</code> if non matched
     */
    ResolvedSiteMapItem match(String pathInfo, ResolvedSiteMount resolvedSiteMount);
  
    /**
     * method that can be called if some event is triggered. For example if the <code>HstSiteMapMatcher</code> implementing class
     * holds a cache that needs to be flushed after a change in the <code>HstSiteMap</code> configuration
     */
    void invalidate();
}


