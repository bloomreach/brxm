/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests;

import javax.jcr.Session;

import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.PagesHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuHelper;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;

public abstract class AbstractMountResourceTest extends AbstractPageComposerTest {

    protected MountResource mountResource;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mountResource = createResource();
    }

    public static MountResource createResource() {
        MountResource resource = new MountResource();
        final PageComposerContextService pageComposerContextService = new PageComposerContextService();
        resource.setPageComposerContextService(pageComposerContextService);
        final SiteMapHelper siteMapHelper = new SiteMapHelper();
        siteMapHelper.setPageComposerContextService(pageComposerContextService);
        resource.setSiteMapHelper(siteMapHelper);
        final PagesHelper pagesHelper = new PagesHelper();
        pagesHelper.setPageComposerContextService(pageComposerContextService);
        resource.setPagesHelper(pagesHelper);
        final SiteMenuHelper siteMenuHelper = new SiteMenuHelper();
        siteMenuHelper.setPageComposerContextService(pageComposerContextService);
        resource.setSiteMenuHelper(siteMenuHelper);
        return resource;
    }

    protected void mockNewRequest(Session jcrSession, String host, String pathInfo) throws Exception {
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(host, pathInfo);
        final String mountId = ctx.getResolvedMount().getMount().getIdentifier();
        ((HstMutableRequestContext) ctx).setSession(jcrSession);
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, mountId);
    }


}
