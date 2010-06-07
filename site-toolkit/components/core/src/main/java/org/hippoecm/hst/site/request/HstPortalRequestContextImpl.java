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

import org.hippoecm.hst.core.request.HstPortalRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;

/**
 * @version $Id$
 *
 */
public class HstPortalRequestContextImpl implements HstPortalRequestContext
{
    private ResolvedSiteMapItem resolvedSiteMapItem;
    private ResolvedSiteMount resolvedEmbeddingSiteMount;
    private String pathInfo;
    
    
	public HstPortalRequestContextImpl(ResolvedSiteMapItem resolvedSiteMapItem, ResolvedSiteMount resolvedEmbeddingSiteMount, String pathInfo) {
		this.resolvedSiteMapItem = resolvedSiteMapItem;
		this.resolvedEmbeddingSiteMount = resolvedEmbeddingSiteMount;
		this.pathInfo = pathInfo;
	}

	public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
		this.resolvedSiteMapItem = resolvedSiteMapItem;
	}

	public void setResolvedEmbeddingSiteMount(
			ResolvedSiteMount resolvedEmbeddingSiteMount) {
		this.resolvedEmbeddingSiteMount = resolvedEmbeddingSiteMount;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public ResolvedSiteMapItem getResolvedSiteMapItem() {
		return resolvedSiteMapItem;
	}
	
	public ResolvedSiteMount getResolvedEmbeddingSiteMount() {
		return resolvedEmbeddingSiteMount;
	}
	
	public String getPathInfo() {
		return pathInfo;
	}
}
