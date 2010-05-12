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

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.container.HstContainerURL;

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
     * @return the named pipeline to be used for this SiteMount or <code>null</code> when the default pipeline is to be used
     */
    String getNamedPipeline();
    
    /**
     * Returns the mountPath from the backing {@link SiteMount} where possible wildcard values might have been replaced. When there is no 
     * mountPath, an empty String will be returned. When the mountPath is non-empty, it always starts with a  <code>"/"</code>
     * @see SiteMount#getMountPath()
     * @return the resolved mountPath for this ResolvedSiteMount
     */
    String getResolvedMountPath();

    /**
     * matches the current hstContainerURL, {@link ResolvedVirtualHost} and {@link ResolvedSiteMount} to a {@link ResolvedSiteMapItem} item or throws a 
     * {@link MatchException} or {@link NotFoundException} when cannot resolve to a sitemap item
     * @param HstContainerURL hstContainerURL
     * @return the ResolvedSiteMapItem for the current hstContainerURL 
     * @throws MatchException 
     */
    ResolvedSiteMapItem matchSiteMapItem(HstContainerURL hstContainerURL) throws MatchException;
    
}
