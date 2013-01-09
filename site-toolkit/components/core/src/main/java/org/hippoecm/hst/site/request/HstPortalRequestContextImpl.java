/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.core.request.ResolvedMount;

/**
 * @version $Id$
 *
 */
public class HstPortalRequestContextImpl implements HstPortalRequestContext
{
    private ResolvedSiteMapItem resolvedSiteMapItem;
    private ResolvedMount resolvedEmbeddingMount;
    private String embeddingContextPath;
    private String pathInfo;
    
    
	public HstPortalRequestContextImpl(ResolvedSiteMapItem resolvedSiteMapItem, String embeddingContextPath, ResolvedMount resolvedEmbeddingMount, String pathInfo) {
		this.resolvedSiteMapItem = resolvedSiteMapItem;
		this.embeddingContextPath = embeddingContextPath;
		this.resolvedEmbeddingMount = resolvedEmbeddingMount;
		this.pathInfo = pathInfo;
	}

	public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
		this.resolvedSiteMapItem = resolvedSiteMapItem;
	}
	
	public void setEmbeddingContextPath(String embeddingContextPath) {
		this.embeddingContextPath = embeddingContextPath;
	}
	
	public void setResolvedEmbeddingMount(
			ResolvedMount resolvedEmbeddingMount) {
		this.resolvedEmbeddingMount = resolvedEmbeddingMount;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public ResolvedSiteMapItem getResolvedSiteMapItem() {
		return resolvedSiteMapItem;
	}
	
	public String getEmbeddingContextPath() {
		return embeddingContextPath;
	}

	public ResolvedMount getResolvedEmbeddingMount() {
		return resolvedEmbeddingMount;
	}
	
	public String getPathInfo() {
		return pathInfo;
	}
}
