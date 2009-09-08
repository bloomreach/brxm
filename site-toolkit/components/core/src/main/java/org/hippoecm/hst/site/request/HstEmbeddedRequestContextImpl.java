/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.site.request;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.HstEmbeddedRequestContext;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * @version $Id$
 *
 */
public class HstEmbeddedRequestContextImpl implements HstEmbeddedRequestContext
{
    private String sitesContentPath;
    private String contentPath;
    private HstSite hstSite;
    private MatchedMapping matchedMapping;
    private ResolvedSiteMapItem resolvedSiteMapItem;
    private HstComponentConfiguration rootComponentConfig;
    
    public String getSitesContentPath()
    {
        return sitesContentPath;
    }
    public void setSitesContentPath(String sitesContentPath)
    {
        this.sitesContentPath = sitesContentPath;
    }
    public String getContentPath()
    {
        return contentPath;
    }
    public void setContentPath(String contentPath)
    {
        this.contentPath = contentPath;
    }
    public HstSite getHstSite()
    {
        return hstSite;
    }
    public void setHstSite(HstSite hstSite)
    {
        this.hstSite = hstSite;
    }
    public MatchedMapping getMatchedMapping()
    {
        return matchedMapping;
    }
    public void setMatchedMapping(MatchedMapping matchedMapping)
    {
        this.matchedMapping = matchedMapping;
    }
    public ResolvedSiteMapItem getResolvedSiteMapItem()
    {
        return resolvedSiteMapItem;
    }
    public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem)
    {
        this.resolvedSiteMapItem = resolvedSiteMapItem;
    }
    public HstComponentConfiguration getRootComponentConfig()
    {
        return rootComponentConfig;
    }
    public void setRootComponentConfig(HstComponentConfiguration rootComponentConfig)
    {
        this.rootComponentConfig = rootComponentConfig;
    }
    
    public String getSiteName()
    {
        return matchedMapping != null ? matchedMapping.getSiteName() : null;
    }
}
