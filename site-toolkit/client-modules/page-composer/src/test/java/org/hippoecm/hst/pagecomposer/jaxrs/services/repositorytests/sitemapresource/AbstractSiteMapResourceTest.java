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

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.PagesHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.AbstractMountResourceTest;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorFactory;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertFalse;

public abstract class AbstractSiteMapResourceTest extends AbstractPageComposerTest {

    protected MountResource mountResource;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mountResource = AbstractMountResourceTest.createResource();
        // create users
        final Node users = session.getNode("/hippo:configuration/hippo:users");

        final Node bob = users.addNode("bob", "hipposys:user");
        bob.setProperty("hipposys:password", "bob");
        final Node alice = users.addNode("alice", "hipposys:user");
        alice.setProperty("hipposys:password", "alice");

        final Node adminGroup = session.getNode("/hippo:configuration/hippo:groups/admin");
        Value[] adminMembers = adminGroup.getProperty("hipposys:members").getValues();

        Value[] extra = new Value[2];
        extra[0] = session.getValueFactory().createValue("bob");
        extra[1] = session.getValueFactory().createValue("alice");

        final Value[] values = (Value[]) ArrayUtils.addAll(adminMembers, extra);
        adminGroup.setProperty("hipposys:members", values);

        // move 2 sitemap items to workspace, keep rest in unittestproject/hst:sitemap
        final Node workspaceNode = session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        workspaceNode.addNode("hst:sitemap");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/news");

        // create workspace pages
        workspaceNode.addNode("hst:pages");

        movePagesFromCommonToUnitTestProject();

        session.move("/hst:hst/hst:configurations/unittestproject/hst:pages/homepage",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/homepage");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:pages/newsoverview",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/newsoverview");

        session.save();
        createPreviewWithSiteMapWorkspace();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        session.refresh(false);
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        users.getNode("bob").remove();
        users.getNode("alice").remove();

        final Node adminGroup = session.getNode("/hippo:configuration/hippo:groups/admin");
        Value[] adminMembers = adminGroup.getProperty("hipposys:members").getValues();

        // remove bob and alice again
        Value[] original = (Value[]) ArrayUtils.subarray(adminMembers, 0, adminMembers.length - 2);

        adminGroup.setProperty("hipposys:members", original);
        session.save();
        super.tearDown();
    }

    protected Session createSession(final String userName, final String password) throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials(userName, password.toCharArray()));
    }

    protected void createPreviewWithSiteMapWorkspace() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
        assertFalse("Preview config node should not exist yet.",
                session.nodeExists(previewConfigurationPath));

        ((HstMutableRequestContext) ctx).setSession(session);
        mountResource.startEdit();
        ModifiableRequestContextProvider.clear();
        // time for jcr events to arrive
        Thread.sleep(100);
    }

    public SiteMapItemRepresentation getSiteMapItemRepresentation(final Session requestSession,
                                                                  final String relPath) throws Exception {
        final SiteMapRepresentation representation = getSiteMapRepresentation(requestSession);

        int segmentPos = 0;
        String[] segments = relPath.split("/");

        SiteMapItemRepresentation itemPresentation = null;
        boolean match = false;
        while (segmentPos < segments.length) {
            if (segmentPos == 0) {
                for (SiteMapItemRepresentation siteMapItemRepresentation : representation.getChildren()) {
                    if (siteMapItemRepresentation.getName().equals(segments[segmentPos])) {
                        itemPresentation = siteMapItemRepresentation;
                        segmentPos++;
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return null;
                }
            } else {
                match = false;
                if (itemPresentation == null) {
                    return null;
                }
                SiteMapItemRepresentation current = itemPresentation;
                itemPresentation = null;
                for (SiteMapItemRepresentation childItemRepresentation : current.getChildren()) {
                    if (childItemRepresentation.getName().equals(segments[segmentPos])) {
                        itemPresentation = childItemRepresentation;
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return null;
                }
                segmentPos++;
            }
        }

        return itemPresentation;
    }

    public SiteMapRepresentation getSiteMapRepresentation(final Session requestSession) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");
        ((HstMutableRequestContext) ctx).setSession(requestSession);
        final HstSite site = mountResource.getPageComposerContextService().getEditingPreviewSite();
        final HstSiteMap siteMap = site.getSiteMap();
        // override the config identifier to have sitemap id
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, ((CanonicalInfo) siteMap).getCanonicalIdentifier());
        return new SiteMapRepresentation().represent(siteMap, getPreviewConfigurationPath(), site.getComponentsConfiguration());
    }

    protected SiteMapResource createResource() {

        final PagesHelper pagesHelper = new PagesHelper();
        pagesHelper.setPageComposerContextService(mountResource.getPageComposerContextService());
        final SiteMapHelper siteMapHelper = new SiteMapHelper();
        siteMapHelper.setPageComposerContextService(mountResource.getPageComposerContextService());
        siteMapHelper.setPagesHelper(pagesHelper);

        final SiteMapResource siteMapResource = new SiteMapResource();
        siteMapResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        siteMapResource.setSiteMapHelper(siteMapHelper);
        siteMapResource.setValidatorFactory(new ValidatorFactory());
        return siteMapResource;
    }


    protected String getPreviewConfigurationPath() {
        return mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath();
    }

    protected String getLiveConfigurationPath() {
        return mountResource.getPageComposerContextService().getEditingLiveSite().getConfigurationPath();
    }

    protected String getPreviewConfigurationWorkspacePath() {
        return getPreviewConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
    }

    protected String getLiveConfigurationWorkspacePath() {
        return getLiveConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
    }

    protected String getPreviewConfigurationWorkspaceSitemapPath() {
        return getPreviewConfigurationWorkspacePath() + "/" + HstNodeTypes.NODENAME_HST_SITEMAP;
    }

    protected String getLiveConfigurationWorkspaceSitemapPath() {
        return getLiveConfigurationWorkspacePath() + "/" + HstNodeTypes.NODENAME_HST_SITEMAP;
    }

    protected String getPreviewConfigurationWorkspacePagesPath() {
        return getPreviewConfigurationWorkspacePath() + "/" + HstNodeTypes.NODENAME_HST_PAGES;
    }

    protected String getLiveConfigurationWorkspacePagesPath() {
        return getLiveConfigurationWorkspacePath() + "/" + HstNodeTypes.NODENAME_HST_PAGES;
    }

    protected String getHomePageUUID() throws RepositoryException {
        String previewConfigurationPath = mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath();
        return session.getNode(previewConfigurationPath).getNode("hst:workspace/hst:pages/homepage").getIdentifier();
    }

    protected String getPrototypePageUUID() throws RepositoryException {
        String previewConfigurationPath = mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath();
        return session.getNode(previewConfigurationPath).getNode("hst:prototypepages/prototype-page").getIdentifier();
    }


    protected SiteMapItemRepresentation createSiteMapItemRepresentation(final String name,
                                                                        final String prototypeUUID) throws RepositoryException {
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName(name);
        newFoo.setComponentConfigurationId(prototypeUUID);
        newFoo.setRelativeContentPath("relFoo");
        Map<String, String> params = new HashMap<>();
        params.put("lux", "qux");
        newFoo.setLocalParameters(params);
        return newFoo;
    }


    protected Node addDefaultCatalogItem() throws RepositoryException {
        Node defaultCatalog = session.getNode("/hst:hst/hst:configurations/hst:default/hst:catalog");
        final Node catalogPackage = defaultCatalog.addNode("package", "hst:containeritempackage");
        final Node catalogItem = catalogPackage.addNode("catalog-item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        catalogItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
        return catalogItem;
    }

    protected ContainerComponentResource createContainerResource() {
        final ContainerComponentResource containerComponentResource = new ContainerComponentResource();
        containerComponentResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        ContainerHelper helper = new ContainerHelper();
        helper.setPageComposerContextService(mountResource.getPageComposerContextService());
        containerComponentResource.setContainerHelper(helper);
        return containerComponentResource;
    }

}
