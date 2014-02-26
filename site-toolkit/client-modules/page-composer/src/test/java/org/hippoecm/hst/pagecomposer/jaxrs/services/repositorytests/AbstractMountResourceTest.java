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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.codehaus.jackson.map.Module;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.PagesHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuHelper;
import org.hippoecm.repository.util.JcrUtils;
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
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, host, pathInfo);
        final String mountId = ctx.getResolvedMount().getMount().getIdentifier();
        ((HstMutableRequestContext) ctx).setSession(jcrSession);
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, mountId);
        setMountIdOnHttpSession(request, mountId);
    }

    protected void createWorkspaceWithTestContainer() throws RepositoryException {
        final Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject");
        final Node workspace = unitTestConfigNode.addNode("hst:workspace", "hst:workspace");
        final Node containers = workspace.addNode("hst:containers", "hst:containercomponentfolder");

        final Node containerNode = containers.addNode("testcontainer", "hst:containercomponent");
        containerNode.setProperty("hst:xtype", "HST.vBox");
    }

    protected void movePagesFromCommonToUnitTestProject() throws RepositoryException {

        // use the 'testcontainer' component from workspace otherwise it won't be part of the hst model, hence, no changes
        // in it will be 'seen'
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:pages");
    }
    protected void addReferencedContainerToHomePage() throws RepositoryException {
        final Node container = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:pages/homepage")
                .addNode("container", "hst:containercomponentreference");
        container.setProperty("hst:referencecomponent", "testcontainer");
    }

    protected String addCatalogItem() throws RepositoryException {
        Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject");
        final Node catalog = unitTestConfigNode.addNode("hst:catalog", "hst:catalog");
        final Node catalogPackage = catalog.addNode("testpackage", "hst:containeritempackage");
        final Node catalogItem = catalogPackage.addNode("testitem", "hst:containeritemcomponent");
        catalogItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE, "thankyou");
        catalogItem.setProperty("hst:xtype", "HST.Item");
        return catalogItem.getIdentifier();
    }



}
