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
package org.hippoecm.hst.configuration.model;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;

public interface HstManager {

    /**
     * @return the <code>VirtualHosts</code> 
     */
    VirtualHosts getVirtualHosts() throws RepositoryNotAvailableException;
    
    
    /**
     * @return the HstURLFactory
     */
    HstURLFactory getUrlFactory();
    
    /**
     * a HstSitesManager must contain a reference to the {@link HstSiteMapMatcher} that is being used. You can inject your own
     *  {@link HstSiteMapMatcher} implementation if needed
     * @return the global HstSiteMapMatcher implementation used for all the hosts & sites
     */
    HstSiteMapMatcher getSiteMapMatcher();
    
    /**
     * @return the siteMapItemHandler factory which can create {@link HstSiteMapItemHandler} instances
     */
    HstSiteMapItemHandlerFactory getSiteMapItemHandlerFactory();
    
    /**
     * Invalidates this HstSitesManager. Typically this invalidate is called after a received event indicating that for example
     * the backing hosts configuration has been changed.
     */
    void invalidate(String path);

}