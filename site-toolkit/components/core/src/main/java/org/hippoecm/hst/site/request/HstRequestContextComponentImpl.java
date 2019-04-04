/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstComponentWindowFilter;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenusManager;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.model.HstModel;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;

/**
 * HstRequestContextComponentImpl
 *
 * @version $Id$
 */
public class HstRequestContextComponentImpl implements HstRequestContextComponent {

    protected Repository repository;
    protected ContextCredentialsProvider contextCredentialsProvider;
    protected ContainerConfiguration config;
    private HstModelProvider hstModelProvider;
    private boolean cachingObjectConverter;
    private ContentBeansTool contentBeansTool;
    private HstURLFactory urlFactory;
    private HstSiteMenusManager siteMenusManager;
    private HstQueryManagerFactory hstQueryManagerFactory;
    private List<HstComponentWindowFilter> componentWindowFilters;
    private ContentTypeService contentTypeService;

    public HstRequestContextComponentImpl(final Repository repository,
                                          final ContextCredentialsProvider contextCredentialsProvider,
                                          final ContainerConfiguration config,
                                          final HstModelProvider hstModelProvider,
                                          final ContentTypeService contentTypeService) {
        this.repository = repository;
        this.contextCredentialsProvider = contextCredentialsProvider;
        this.config = config;
        this.hstModelProvider = hstModelProvider;
        this.contentTypeService = contentTypeService;
    }

    public HstMutableRequestContext create() {
        HstMutableRequestContext rc = new HstRequestContextImpl(repository, contextCredentialsProvider);
        rc.setContainerConfiguration(config);
        rc.setURLFactory(urlFactory);
        final HstModel hstModel = hstModelProvider.getHstModel();
        setCurrentContentTypes(rc);
        rc.setLinkCreator(hstModel.getHstLinkCreator());
        rc.setSiteMapMatcher(hstModel.getHstSiteMapMatcher());
        rc.setHstSiteMenusManager(siteMenusManager);
        rc.setHstQueryManagerFactory(hstQueryManagerFactory);
        rc.setContentBeansTool(contentBeansTool);
        rc.setCachingObjectConverter(cachingObjectConverter);
        rc.setComponentWindowFilters(componentWindowFilters);
        return rc;
    }

    private void setCurrentContentTypes(final HstMutableRequestContext rc) {
        try {
            if (contentTypeService == null) {
                contentTypeService = HippoServiceRegistry.getService(ContentTypeService.class);
            }
            if (contentTypeService == null) {
                throw new IllegalStateException("ContentTypeService is not available");
            }
            rc.setContentTypes(contentTypeService.getContentTypes());
        } catch (RepositoryException e) {
            throw new IllegalStateException("Fetching ContentTypeService failed", e);
        }
    }

    public void release(HstRequestContext context) {
        // dispose the request context to ensure all the stateful objects aren't to be reused again.
        ((HstMutableRequestContext) context).dispose();
    }

    public void setUrlFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }

    public void setSiteMenusManager(HstSiteMenusManager siteMenusManager) {
        this.siteMenusManager = siteMenusManager;
    }

    public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory) {
        this.hstQueryManagerFactory = hstQueryManagerFactory;
    }

    public void setContentBeansTool(ContentBeansTool contentBeansTool) {
        this.contentBeansTool = contentBeansTool;
    }

    public void setCachingObjectConverter(final boolean cachingObjectConverter) {
        this.cachingObjectConverter = cachingObjectConverter;
    }

    public void setComponentWindowFilters(final List<HstComponentWindowFilter> componentWindowFilters) {
        this.componentWindowFilters = componentWindowFilters;
    }
}
