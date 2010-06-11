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
package org.hippoecm.hst.core.internal;

import javax.jcr.Session;

import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

/**
 * @version $Id$
 *
 */
public interface HstMutableRequestContext extends HstRequestContext {

	public void setContextNamespace(String contextNamespace);

	public void setSession(Session session);

	public void setResolvedSiteMount(ResolvedSiteMount resolvedSiteMount);

	public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem);
	
	public void setTargetComponentPath(String targetComponentPath);

	public void setBaseURL(HstContainerURL baseURL);

	public void setURLFactory(HstURLFactory urlFactory);

	public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher);

	public void setLinkCreator(HstLinkCreator linkCreator);

	public void setHstSiteMenus(HstSiteMenus siteMenus);

	public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory);

	public void setContainerConfiguration(ContainerConfiguration containerConfiguration);
	
	public void setEmbeddingContextPath(String embeddingContextPath);

	public void setResolvedEmbeddingSiteMount(ResolvedSiteMount resolvedEmbeddingSiteMount);
}
