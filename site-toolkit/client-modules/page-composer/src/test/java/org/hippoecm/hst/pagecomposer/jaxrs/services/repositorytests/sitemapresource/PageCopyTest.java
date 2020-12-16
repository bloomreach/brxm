/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPageRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Before;
import org.junit.Test;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_REF_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageCopyTest extends AbstractSiteMapResourceTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        createPreviewWithSiteMapWorkspace("localhost", "/subsite");
        session.save();
        // time for jcr events to arrive
        Thread.sleep(100);
    }

    @Test
    public void assert_setup_configuration_fixture() throws Exception {
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP + "/home"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP + "/home"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/" + NODENAME_HST_WORKSPACE));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/" + NODENAME_HST_WORKSPACE));
    }

    @Test
    public void page_copy_to_root_item_within_same_channel() throws Exception {
        copyHomePageWithinSameChannel(true, "copiedHome", null);
    }

    private void copyHomePageWithinSameChannel(final boolean publish, final String copyName, final SiteMapItemRepresentation targetParent) throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
        final Response copy = siteMapResource.copy(editingMount.getIdentifier(), home.getId(), (targetParent == null ? null : targetParent.getId()), copyName);
        assertEquals(OK.getStatusCode(), copy.getStatus());

        final String pathInfo;
        if (targetParent == null) {
            pathInfo = copyName;
        } else if (targetParent.getIsHomePage()){
            pathInfo = targetParent.getName() + "/" + copyName;
        } else {
            pathInfo = targetParent.getPathInfo() + "/" + copyName;
        }

        final String pageName = pathInfo.replaceAll("/", "-");
        final String previewSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/" + pathInfo;
        final String previewPageNodePath = "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/" + pageName;
        final String liveSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/" + pathInfo;
        final String livePageNodePath = "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/" + pageName;
        assertTrue(session.nodeExists(previewSiteMapItemNodePath));
        assertTrue(session.nodeExists(previewPageNodePath));
        assertFalse(session.nodeExists(liveSiteMapItemNodePath));
        assertFalse(session.nodeExists(livePageNodePath));
        assertEquals("admin", session.getNode(previewSiteMapItemNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("admin", session.getNode(previewPageNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        // no templates copied since within same channel, so templates are not locked (templates are inherited from common any way
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:templates").hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        if (publish) {
            mountResource.publish();
            assertTrue(session.nodeExists(previewSiteMapItemNodePath));
            assertTrue(session.nodeExists(previewPageNodePath));
            assertTrue(session.nodeExists(liveSiteMapItemNodePath));
            assertTrue(session.nodeExists(livePageNodePath));
            assertFalse(session.getNode(previewSiteMapItemNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
            assertFalse(session.getNode(previewPageNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        }
    }

    @Test
    public void page_copy_does_not_copy_refId() throws Exception {
        copyHomePageWithinSameChannel(true, "copiedHome", null);
        assertEquals("homeRefId", session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home").getProperty(SITEMAPITEM_PROPERTY_REF_ID).getString());
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/copiedHome").hasProperty(SITEMAPITEM_PROPERTY_REF_ID));
    }

    @Test
    public void page_copy_to_parent_item_within_same_channel() throws Exception {
        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "localhost", "/news");
        copyHomePageWithinSameChannel(true, "copiedHome", news);
    }

    @Test
    public void page_copy_to_itself_within_same_channel() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        copyHomePageWithinSameChannel(true, "copiedHome", home);
    }

    @Test
    public void non_workspace_page_can_be_copied() throws Exception {
        // move homepage out of 'hst:workspace config
        moveHomePageOutWorkspace();
        session.save();
        Thread.sleep(100);
        copyHomePageWithinSameChannel(true, "copiedHome", null);
    }

    private void moveHomePageOutWorkspace() throws RepositoryException {
        session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/home",
                "/hst:hst/hst:configurations/unittestproject/hst:sitemap/home");
        session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/home").remove();

        session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/homepage",
                "/hst:hst/hst:configurations/unittestproject/hst:pages/homepage");
        session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/homepage").remove();
    }


    @Test
    public void non_workspace_page_copy_with_reference_components_get_denormalized() throws Exception {
        moveHomePageOutWorkspace();

        final Node previewWorkspace = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace");
        final Node liveWorkspace = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
        final Node[] workspaces = {previewWorkspace, liveWorkspace};
        for (Node workspace : workspaces) {
            final Node containers = workspace.addNode("hst:containers", "hst:containercomponentfolder");
            final Node containerNode = containers.addNode("testcontainer", "hst:containercomponent");
            containerNode.setProperty("hst:xtype", "HST.vBox");
            final Node containerItem = containerNode.addNode("item", HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
            containerItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.Item");
            if (workspace == liveWorkspace) {
                // only the live has non-workspace pages
                final Node homePageContainer = session.getNode(workspace.getParent().getPath() + "/hst:pages/homepage")
                        .addNode("container", "hst:containercomponentreference");
                homePageContainer.setProperty("hst:referencecomponent", "testcontainer");
            }
        }
        session.save();
        Thread.sleep(100);
        copyHomePageWithinSameChannel(false, "copiedHome", null);

        // assert denormalization of the hst:containercomponentreference 'container' which now should have 'item' as child node
        // instead of a pointer to 'hst:workspace/hst:containers/testcontainer'.
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome/container/item"));

        // assert the container is locked
        assertEquals("admin", session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome/container")
                .getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        mountResource.publish();
        Thread.sleep(100);
        assertFalse(session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/copiedHome/container")
                .hasProperty(GENERAL_PROPERTY_LOCKED_BY));

    }

    private Mount getTargetMountByAlias(final String alias) {
        final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
        final VirtualHost virtualHost = editingMount.getVirtualHost();
        final Mount target = virtualHost.getVirtualHosts().getMountByGroupAliasAndType(virtualHost.getHostGroupName(), alias, "preview");
        return target;
    }

    @Test
    public void page_copy_creates_missing_workspace_config_nodes_if_missing() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount targetMount = getTargetMountByAlias("subsite");

        final String previewConfigurationPath = targetMount.getHstSite().getConfigurationPath();
        final String liveConfigurationPath = previewConfigurationPath.replace("-preview/", "/");
        final String[] configPaths = {previewConfigurationPath, liveConfigurationPath};
        for (String configPath : configPaths) {
            assertTrue(session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE));
            assertTrue("pages should exist MountResource.startEdit should had created them."
                    , session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_PAGES));
            assertTrue("sitemap should not yet exist MountResource.startEdit should had created them.", session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP));

        }
        session.getNode(liveConfigurationPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_PAGES).remove();
        session.getNode(liveConfigurationPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP).remove();
        session.save();

        Thread.sleep(100);
        getSiteMapItemRepresentation(session, "localhost", "/home");

        final Response copy = siteMapResource.copy(targetMount.getIdentifier(), home.getId(), null, "copy");
        assertEquals(OK.getStatusCode(), copy.getStatus());

        //  assert target channel has now also automatically added 'hst:sitemap' and 'hst:pages' below its workspace
        for (String configPath : configPaths) {
            assertTrue(session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE));
            assertTrue("pages should exist again.", session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_PAGES));
            assertTrue("sitemap should exist again.", session.nodeExists(configPath + "/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP));
        }

    }

    @Test
    public void page_copy_cross_channel() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount targetMount = getTargetMountByAlias("subsite");
        final Response copy = siteMapResource.copy(targetMount.getIdentifier(), home.getId(), null, "copy");
        assertEquals(OK.getStatusCode(), copy.getStatus());
        final String newSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:sitemap/copy";
        final String newPageNodePath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy";
        assertTrue(session.nodeExists(newSiteMapItemNodePath));
        assertTrue(session.nodeExists(newPageNodePath));
        assertEquals("admin", session.getNode(newSiteMapItemNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        assertEquals("admin", session.getNode(newPageNodePath).getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());
        assertFalse(session.nodeExists(newSiteMapItemNodePath.replace("-preview/", "/")));
        assertFalse(session.nodeExists(newPageNodePath.replace("-preview/", "/")));

        // before we can use 'mountResource' to publish, we first have to 'switch' current request context to 'subsite'
        getSiteMapItemRepresentation(session, "localhost", "/subsite");

        assertEquals("subsite",RequestContextProvider.get().getResolvedMount().getMount().getName());

        mountResource.publish();

        Thread.sleep(100);
        assertTrue(session.nodeExists(newSiteMapItemNodePath.replace("-preview/","/")));
        assertTrue(session.nodeExists(newPageNodePath.replace("-preview/","/")));
        assertFalse(session.getNode(newSiteMapItemNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
        assertFalse(session.getNode(newPageNodePath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
    }

    @Test
    public void page_copy_cross_channel_returns_representation_wrt_target_channel() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount targetMount = getTargetMountByAlias("subsite");
        final Response copy = siteMapResource.copy(targetMount.getIdentifier(), home.getId(), null, "copy");
        assertEquals(OK.getStatusCode(), copy.getStatus());
        ResponseRepresentation responseRepresentation = (ResponseRepresentation)copy.getEntity();
        assertEquals(SiteMapPageRepresentation.class, responseRepresentation.getData().getClass());
        SiteMapPageRepresentation siteMapPageRepresentation = (SiteMapPageRepresentation) responseRepresentation.getData();
        assertEquals("copy", siteMapPageRepresentation.getPathInfo());
        assertEquals("/subsite/copy", siteMapPageRepresentation.getRenderPathInfo());
    }

    @Test
    public void page_copy_cross_channel_pageNode_already_exists_results_in_counter_added() throws Exception {
        // first add the 'copy' page node already, which is created by xyz
        session.getNode("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:pages").addNode("copy", HstNodeTypes.NODETYPE_HST_COMPONENT);
        session.getNode("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages").addNode("copy", HstNodeTypes.NODETYPE_HST_COMPONENT);
        session.save();
        // time for jcr events to arrive
        Thread.sleep(100);

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "localhost", "/home");
        SiteMapResource siteMapResource = createResource();
        final Mount targetMount = getTargetMountByAlias("subsite");
        final Response copy = siteMapResource.copy(targetMount.getIdentifier(), home.getId(), null, "copy");
        assertEquals(OK.getStatusCode(), copy.getStatus());
        final String newSiteMapItemNodePath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:sitemap/copy";
        final String expectedPageNodePathWithCounter = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy-1";
        assertTrue(session.nodeExists(newSiteMapItemNodePath));
        assertTrue(session.nodeExists(expectedPageNodePathWithCounter));
        // and assert new sitemap item points to page with counter
        assertEquals("hst:pages/copy-1", session.getNode(newSiteMapItemNodePath).getProperty(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID).getString());
    }

    @Test
    public void page_copy_cross_channel_copies_missing_referenced_pages_as_well() throws Exception {
        moveTemplatesFromCommonToUnitTestProject();

        session.save();

        // wait for jcr events
        Thread.sleep(100);

        // hst:pages/newsoverview is not available in subsite (because of movePagesFromCommonToUnitTestProject)
        // hence when copying the 'news' page, we'd expect hst:pages/standardoverview to be added to the subsite.
        crossChannelCopyNewsOverview(MountAction.PUBLISH);
    }
    @Test
     public void discard_after_page_copy_cross_channel_removes_added_referenced_pages_as_well() throws Exception {
        moveTemplatesFromCommonToUnitTestProject();
        session.save();

        // wait for jcr events
        Thread.sleep(100);

        // hst:pages/newsoverview is not available in subsite (because of movePagesFromCommonToUnitTestProject)
        // hence when copying the 'news' page, we'd expect hst:pages/standardoverview to be added to the subsite.
        crossChannelCopyNewsOverview(MountAction.DISCARD);
    }

    enum MountAction {
        PUBLISH, DISCARD, NOOP
    }

    private void crossChannelCopyNewsOverview(final MountAction action) throws Exception {

        final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "localhost", "/news");
        SiteMapResource siteMapResource = createResource();
        final Mount targetMount = getTargetMountByAlias("subsite");

        assertFalse("no templates yet expected in subproject preview",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates"));
        assertFalse("no templates yet expected in subproject live",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:templates"));

        final Response copy = siteMapResource.copy(targetMount.getIdentifier(), news.getId(), null, "copy");

        assertEquals(OK.getStatusCode(), copy.getStatus());

        // cross copy is copied from non-workspace 'unittestproject' to the workspace of target channel
        final String previewPagesPath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages";
        final String livePagesPath = "/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:pages";
        assertTrue(session.nodeExists(previewPagesPath + "/standardoverview"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy"));
        // standardoverview should have been copied since it was missing in unittestsubproject
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/standardoverview"));

        assertTrue("as a result of the page copy, we expect templates in the subproject preview to be copied",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates"));

        assertTrue("as a result of the page copy, we expect templates in the subproject live to be present",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates"));


        final List<String> templateNodeNames = new ArrayList<>();
        for (Node template : new NodeIterable(session.getNode("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates").getNodes())) {
            assertTrue("all new templates are expected to be locked", template.hasProperty(GENERAL_PROPERTY_LOCKED_BY));
            templateNodeNames.add(template.getName());
        }

        assertFalse("hst:pages should NOT be locked, only the copied page plus references", session.getNode(previewPagesPath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));

        assertFalse(session.nodeExists(livePagesPath + "/standardoverview"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:pages/copy"));

        // before we can use 'mountResource' to publish, we first have to 'switch' current request context to 'subsite'
        getSiteMapItemRepresentation(session, "localhost", "/subsite");

        assertEquals("subsite", RequestContextProvider.get().getResolvedMount().getMount().getName());

        if (action == MountAction.PUBLISH) {
            mountResource.publish();

            Thread.sleep(100);
            assertFalse("hst:pages should be unlocked", session.getNode(previewPagesPath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy"));
            assertTrue(session.nodeExists(previewPagesPath + "/standardoverview"));
            assertTrue(session.nodeExists(livePagesPath + "/standardoverview"));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:pages/copy"));

            for (String templateNodeName : templateNodeNames) {
                final String absLivePath = "/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:templates/" + templateNodeName;
                final String absPreviewPath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates/" + templateNodeName;
                for (String absPath : new String[]{absLivePath, absPreviewPath}) {
                    assertTrue(session.nodeExists(absPath));
                    assertFalse(session.getNode(absPath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));
                }
            }

        } else if (action == MountAction.DISCARD) {
            mountResource.discardChanges();
            Thread.sleep(100);
            assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy"));
            assertFalse(session.nodeExists(previewPagesPath + "/standardoverview"));
            assertFalse(session.nodeExists(livePagesPath + "/standardoverview"));
            assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:pages/copy"));
            // the newly created templates node stays there
            assertTrue("even after discard the hst:templates root node stays",
                    session.nodeExists( "/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:templates"));
            assertTrue("even after discard the hst:templates root node stays",
                    session.nodeExists( "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates"));

            assertEquals(0L, session.getNode("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates").getNodes().getSize());
            assertEquals(0L, session.getNode("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:templates").getNodes().getSize());

        } else if (action == MountAction.NOOP) {
            // do publication or discard
        }
    }

    protected void moveAbstractPagesFromCommonToUnitTestProject() throws RepositoryException {
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:abstractpages", "/hst:hst/hst:configurations/unittestproject-preview/hst:abstractpages");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages", "/hst:hst/hst:configurations/unittestproject/hst:abstractpages");

    }

    @Test
    public void page_copy_cross_channel_copies_missing_referenced_abstract_pages_as_well() throws Exception {
        moveAbstractPagesFromCommonToUnitTestProject();
        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        Thread.sleep(100);
        assertFalse("hst:abstractpages was expected to not even exist before the copy page", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:abstractpages"));
        crossChannelCopyNewsOverview(MountAction.PUBLISH);
        // now make sure that 'hst:abstractpages/basepage is also available in preview and live subsite config
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:abstractpages/basepage"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:abstractpages/basepage"));
    }

    @Test
    public void discard_changes_after_page_copy_cross_channel_removes_added_referenced_abstract_pages_as_well() throws Exception {
        moveAbstractPagesFromCommonToUnitTestProject();
        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        Thread.sleep(100);
        assertFalse("hst:abstractpages was expected to not exist before the copy page", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:abstractpages"));
        assertFalse("hst:abstractpages was expected to not exist before the copy page", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:abstractpages"));

        crossChannelCopyNewsOverview(MountAction.DISCARD);

        // due to the discard, the 'hst:abstractpages' node is expected to be there, but without any children
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:abstractpages"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:abstractpages"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:abstractpages"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:abstractpages"));

        assertEquals(0L, session.getNode("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:abstractpages").getNodes().getSize());
        assertEquals(0L, session.getNode("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:abstractpages").getNodes().getSize());
    }

    protected void moveComponentsFromCommonToUnitTestProject() throws RepositoryException {
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:components", "/hst:hst/hst:configurations/unittestproject-preview/hst:components");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:components", "/hst:hst/hst:configurations/unittestproject/hst:components");
    }

    @Test
    public void page_copy_cross_channel_copies_missing_inherited_pages_and_components_as_well() throws Exception {
        moveComponentsFromCommonToUnitTestProject();
        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        Thread.sleep(100);
        crossChannelCopyNewsOverview(MountAction.PUBLISH);
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:components/overview"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:components/overview"));

        // now a delicate assertion: Since hst:abstractpages are present in 'common', but they now referenced 'hst:components'
        // that have moved from 'common' to 'unittestproject', all components referenced from abstractpages cannot be found any more
        // We do *ON PURPOSE* not copy the hst:components from inherited abstractpages, since a normal HST configuration
        // setup should not have common abstractpages referencing hst:components that are not present in 'common' (or an inherited
        // configuration is an odd setup) but only in downstream configuration (unittestproject) is an odd setup
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:components/header"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:components/leftmenu"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:components/header"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:components/leftmenu"));
    }

    @Test
    public void discard_changes_page_copy_cross_channel_removes_added_components_as_well() throws Exception {
        moveComponentsFromCommonToUnitTestProject();
        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        Thread.sleep(100);
        crossChannelCopyNewsOverview(MountAction.DISCARD);
        assertTrue("main config nodes stay there even after discard since they are not locked",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:components"));
        assertTrue("main config nodes are even added to live config without publish",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:components"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:components"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:components"));
    }

    @Test
    public void page_copy_cross_channel_copies_missing_inherited_pages_abstract_pages_and_components_as_well() throws Exception {
        moveAbstractPagesFromCommonToUnitTestProject();
        moveComponentsFromCommonToUnitTestProject();
        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        Thread.sleep(100);
        assertFalse("hst:abstractpages was expected to not even exist before the copy page", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:abstractpages"));
        assertFalse("hst:components was expected to not even exist before the copy page", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:components"));
        assertFalse("workspace hst:abstractpages was expected to not even exist before the copy page",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:abstractpages"));
        assertFalse("workspace hst:components was expected to not even exist before the copy page",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:components"));
        crossChannelCopyNewsOverview(MountAction.PUBLISH);
        // now make sure that 'hst:abstractpages/basepage is also available in preview and live workspace subsite config
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:abstractpages/basepage"));
        assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:abstractpages/basepage"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:abstractpages/basepage"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:abstractpages/basepage"));

        // now also the referenced components are expected to be copied, since the abstract pages are explicitly in 'unittestproject'
        // now as well
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:components/header"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:components/leftmenu"));
    }

    protected void moveTemplatesFromCommonToUnitTestProject() throws RepositoryException {
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:templates", "/hst:hst/hst:configurations/unittestproject-preview/hst:templates");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:templates", "/hst:hst/hst:configurations/unittestproject/hst:templates");
    }

    @Test
    public void page_copy_cross_channel_copies_missing_templates_as_well() throws Exception {
        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        Thread.sleep(100);
        assertFalse("hst:templates was expected to not even exist before the copy page",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:templates"));
        assertFalse("workspace hst:templates was expected to not even exist before the copy page",
                session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates"));
        crossChannelCopyNewsOverview(MountAction.PUBLISH);
        // make assertions about the templates that now should have become available
        final String[] expectedTemplates = {"webpage", "overview", "header", "title", "leftmenu"};
        for (String expectedTemplate : expectedTemplates) {
            assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:templates/" + expectedTemplate));
            assertFalse(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:templates/" + expectedTemplate));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates/" + expectedTemplate));
            assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:templates/" + expectedTemplate));
        }
    }

    @Test
    public void discard_page_copy_cross_channel_removes_added_templates_as_well() throws Exception {
        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        Thread.sleep(100);
        assertFalse("hst:templates was expected to not exist before the copy page", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:templates"));
        assertFalse("hst:templates was expected to not exist before the copy page", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates"));
        crossChannelCopyNewsOverview(MountAction.DISCARD);
        assertFalse("hst:templates was expected to not exist after the discard", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:templates"));
        assertTrue("hst:templates was expected to even exist after the discard", session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates"));
    }

    protected void movePagesFromUnitTestProjectToCommon() throws RepositoryException {
        session.move("/hst:hst/hst:configurations/unittestproject/hst:pages", "/hst:hst/hst:configurations/unittestcommon/hst:pages");
    }

    @Test
    public void page_copy_cross_channel_different_users_succeeds_if_no_overlapping_nodes() throws Exception {
        // move pages to common first
        movePagesFromUnitTestProjectToCommon();
        session.save();
        Thread.sleep(100);

        {
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "localhost", "/news");
            SiteMapResource siteMapResource = createResource();
            final Mount targetMount = getTargetMountByAlias("subsite");
            final Response copy = siteMapResource.copy(targetMount.getIdentifier(), news.getId(), null, "copy");
            assertEquals(OK.getStatusCode(), copy.getStatus());
        }
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy"));

        // now a different user should be able to copy '/news' to 'copy-again'. This is possible because the previous
        // siteMapResource.copy did not result in copied missing abstractpages, templates, components, etc.
        final Session bob = createSession("bob", "bob");
        try {
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(bob, "localhost", "/news");
            SiteMapResource siteMapResource = createResource();
            final Mount targetMount = getTargetMountByAlias("subsite");
            final Response copy = siteMapResource.copy(targetMount.getIdentifier(), news.getId(), null, "copy-again");
            assertEquals(OK.getStatusCode(), copy.getStatus());
            assertTrue(bob.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy-again"));
            assertEquals("bob",bob.getNode("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy-again").getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

            // before we can use 'mountResource' to publish, we first have to 'switch' current request context to 'subsite'
            getSiteMapItemRepresentation(bob, "localhost", "/subsite");

            mountResource.publish();
            assertTrue(bob.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:pages/copy-again"));
            // changes from 'session' are not yet published
            assertFalse(bob.nodeExists("/hst:hst/hst:configurations/unittestsubproject/hst:workspace/hst:pages/copy"));
        } finally {
            bob.logout();
        }
    }

    @Test
    public void page_copy_cross_channel_already_locked_exception_due_to_other_copy_locking_non_workspace_pages() throws Exception {

        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        // some time for the jcr events to arrive
        Thread.sleep(100);
        crossChannelCopyNewsOverview(MountAction.NOOP);
        final String previewWorkspacePagesPath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages";
        assertTrue(session.nodeExists(previewWorkspacePagesPath+ "/copy"));

        assertFalse(session.getNode(previewWorkspacePagesPath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));

        assertTrue(session.nodeExists(previewWorkspacePagesPath + "/standardoverview"));
        assertEquals("admin", session.getNode(previewWorkspacePagesPath+ "/standardoverview").getProperty(GENERAL_PROPERTY_LOCKED_BY).getString());

        // now a different user should NOT be able to copy '/news' to 'copy-again'. This is NOT possible because the previous
        // siteMapResource.copy did result in copied missing hst:pages (which are still locked)
        final Session bob = createSession("bob", "bob");
        try {
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(bob, "localhost", "/news");
            SiteMapResource siteMapResource = createResource();
            final Mount targetMount = getTargetMountByAlias("subsite");
            final Response copy = siteMapResource.copy(targetMount.getIdentifier(), news.getId(), null, "copy-again");
            assertEquals(BAD_REQUEST.getStatusCode(), copy.getStatus());
            final ResponseRepresentation entity = (ResponseRepresentation)copy.getEntity();
            final String errorCode = entity.getErrorCode();
            assertEquals(String.valueOf(ClientError.ITEM_ALREADY_LOCKED), errorCode);
            // error message is about this node being locked
            assertTrue(entity.getMessage().contains("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages"));
        } finally {
            bob.logout();
        }
    }

    @Test
    public void page_copy_cross_channel_already_locked_exception_due_to_other_copy_locking_templates() throws Exception {
        // move pages to common first otherwise 'pages' already causes the lock
        movePagesFromUnitTestProjectToCommon();
        moveTemplatesFromCommonToUnitTestProject();
        session.save();
        Thread.sleep(100);
        {
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(session, "localhost", "/news");
            SiteMapResource siteMapResource = createResource();
            final Mount targetMount = getTargetMountByAlias("subsite");
            final Response copy = siteMapResource.copy(targetMount.getIdentifier(), news.getId(), null, "copy");
            assertEquals(OK.getStatusCode(), copy.getStatus());
        }
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:pages/copy"));
        final String templatesPath = "/hst:hst/hst:configurations/unittestsubproject-preview/hst:workspace/hst:templates";
        assertFalse("hst:templates node should never get locked, only children",
                session.getNode(templatesPath).hasProperty(GENERAL_PROPERTY_LOCKED_BY));

        // now a different user should NOT be able to copy '/news' to 'copy-again'. This is NOT possible because the previous
        // siteMapResource.copy did result in copied missing templates (which are still locked)
        final Session bob = createSession("bob", "bob");
        try {
            final SiteMapItemRepresentation news = getSiteMapItemRepresentation(bob, "localhost", "/news");
            SiteMapResource siteMapResource = createResource();
            final Mount targetMount = getTargetMountByAlias("subsite");
            final Response copy = siteMapResource.copy(targetMount.getIdentifier(), news.getId(), null, "copy-again");
            assertEquals(BAD_REQUEST.getStatusCode(), copy.getStatus());
            final ResponseRepresentation entity = (ResponseRepresentation)copy.getEntity();
            final String errorCode = entity.getErrorCode();
            assertEquals(String.valueOf(ClientError.ITEM_ALREADY_LOCKED), errorCode);
            // error message is about this node being locked
            assertTrue(entity.getMessage().contains(templatesPath));
        } finally {
            bob.logout();
        }
    }

    @Test
    public void validate_invalid_copy_names() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        for (String invalidChar : new String[]{"?", ";", "#", "\\"}) {
            invalidCopyNameAssertions(home, invalidChar, "Invalid pathInfo");
        }
        for (String invalidChar : new String[]{":", "/"}) {
            invalidCopyNameAssertions(home, invalidChar, "is invalid");
        }
        // %3A = :
        // %2F = /
        // %2f = /
        // %5c = \
        // %5C = \
        // %2e = .
        // %2E = .
        // %3F = ?
        // %3B = ;
        // %23 = #
        for (String checkURLEncodedChar : new String[]{"%3A", "%2F", "%2f", "%5c", "%5C", "%2e", "%2E", "%3F", "%3B", "%23"}) {
            invalidCopyNameAssertions(home, checkURLEncodedChar, "Invalid pathInfo");
        }
    }

    private void invalidCopyNameAssertions(final SiteMapItemRepresentation home, final String invalidChar, final String messagePart) {
        SiteMapResource siteMapResource = createResource();
        final Mount editingMount = mountResource.getPageComposerContextService().getEditingMount();
        final Response copy = siteMapResource.copy(editingMount.getIdentifier(), home.getId(), null, "ho" + invalidChar+"me");
        assertEquals(BAD_REQUEST.getStatusCode(), copy.getStatus());
        assertTrue(((ResponseRepresentation) copy.getEntity()).getMessage().contains(messagePart));
    }

}
