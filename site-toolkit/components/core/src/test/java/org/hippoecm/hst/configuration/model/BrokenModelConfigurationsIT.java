/*
 *  Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.model;


import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.configuration.hosting.MountService;
import org.hippoecm.hst.configuration.hosting.VirtualHostService;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSiteService;
import org.hippoecm.hst.configuration.sitemap.HstNoopSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.request.ResolvedSiteMapItemImpl;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BrokenModelConfigurationsIT extends AbstractTestConfigurations {

    private HstManager hstManager;
    private EventPathsInvalidator invalidator;
    private Session session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
        hstManager = getComponent(HstManager.class.getName());
        invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }

    @Test
    public void testModelMissingHstRootDuringFirstLoad() throws Exception {
        session.getNode("/hst:hst").remove();
        session.save();
        try {
            hstManager.getVirtualHosts();
            fail("Model should not be possible to load");
        } catch (ContainerException e) {
            assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.FAILED);
        }
    }


    @Test
    public void testModelMissingHstRootDuringReload() throws Exception {
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);

        session.getNode("/hst:hst").remove();
        session.save();
        // trigger a reload below a /hst:hst node since /hst:hst or / is ignored in
        // HstEventsCollector
        invalidator.eventPaths("/hst:hst/hst:hosts");
        assertTrue(((HstManagerImpl) hstManager).state == HstManagerImpl.BuilderState.STALE);

        Log4jInterceptor.onWarn().deny(HstManagerImpl.class).run( () -> {
            try {
                final VirtualHosts newModel = hstManager.getVirtualHosts();
                assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.FAILED);
                assertTrue( ((HstManagerImpl)hstManager).consecutiveBuildFailCounter == 1);
                assertSame("hst nodes config should be invalid, hence, old stale model should be reused ",firstModel , newModel);

                hstManager.getVirtualHosts();
                assertTrue( ((HstManagerImpl)hstManager).consecutiveBuildFailCounter == 2);
                hstManager.getVirtualHosts();
                assertTrue(((HstManagerImpl) hstManager).consecutiveBuildFailCounter == 3);

            } catch (ContainerException e) {
                fail(e.toString());
            }
        });

        restoreHstConfigBackup(session);

        // trigger a reload below a /hst:hst node since /hst:hst or / is ignored in
        // HstEventsCollector
        invalidator.eventPaths("/hst:hst/hst:hosts");

        final VirtualHosts finalModel = hstManager.getVirtualHosts();
        assertNotSame(finalModel, firstModel);
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
        assertTrue( ((HstManagerImpl)hstManager).consecutiveBuildFailCounter == 0);
    }

    @Test
    public void testModelMissingSitesDuringFirstLoad() throws Exception {
        session.getNode("/hst:hst/hst:sites").remove();
        session.save();
        try {
            hstManager.getVirtualHosts();
            fail("Model should not be possible to load");
        } catch (ContainerException e) {
            assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.FAILED);
        }
    }

    @Test
    public void testModelMissingSitesDuringReload() throws Exception {
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
        final Map<String,Channel> channels = firstModel.getChannels("dev-localhost");
        assertTrue(channels.size() == 2);
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("hst:root",resolvedMount.getMount().getName());
        assertEquals("/hst:hst/hst:sites/unittestproject",resolvedMount.getMount().getMountPoint());
        assertTrue(resolvedMount.getMount().getChannel() == channels.get("unittestproject"));
        // now remove sites node
        session.getNode("/hst:hst/hst:sites").remove();
        invalidator.eventPaths("/hst:hst/hst:sites");
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.STALE);
        session.save();

        Log4jInterceptor.onWarn().deny(HstManagerImpl.class).run( () -> {
            try {
                final VirtualHosts newModel = hstManager.getVirtualHosts();
                assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.FAILED);
                assertTrue( ((HstManagerImpl)hstManager).consecutiveBuildFailCounter == 1);
                assertSame("hst nodes config should be invalid, hence, old stale model should be reused ",firstModel , newModel);

                hstManager.getVirtualHosts();
                assertTrue( ((HstManagerImpl)hstManager).consecutiveBuildFailCounter == 2);
                hstManager.getVirtualHosts();
                assertTrue(((HstManagerImpl) hstManager).consecutiveBuildFailCounter == 3);

            } catch (ContainerException e) {
                fail(e.toString());
            }
        });

        restoreHstConfigBackup(session);
        invalidator.eventPaths("/hst:hst/hst:sites");

        final VirtualHosts finalModel = hstManager.getVirtualHosts();
        assertNotSame(finalModel, firstModel);
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
        assertTrue( ((HstManagerImpl)hstManager).consecutiveBuildFailCounter == 0);
    }


    @Test
    public void testModelMissingSingleSite() throws Exception {
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
        // now remove /hst:hst/hst:sites/unittestproject node
        session.getNode("/hst:hst/hst:sites/unittestproject").remove();
        invalidator.eventPaths("/hst:hst/hst:sites");
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.STALE);
        session.save();

        Log4jInterceptor.onWarn().deny(MountService.class, VirtualHostService.class, VirtualHostsService.class).run( () -> {
            try {
                final VirtualHosts newModel = hstManager.getVirtualHosts();
                assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
                assertTrue(((HstManagerImpl) hstManager).consecutiveBuildFailCounter == 0);
                assertNotSame(firstModel, newModel);
                final ResolvedMount newResolvedMount = newModel.matchMount("localhost", "/site", "home");
                // although the resolved mount is not a valid one, we can still match it : It can namely
                // still contain valid child mount
                assertNotNull(newResolvedMount);
                assertNull(newResolvedMount.getMount().getContentPath());
                assertNull(newResolvedMount.getMount().getHstSite());

                // assert we can still match the child mount 'subsite' which has
                // hst:mountpoint = /hst:hst/hst:sites/unittestsubproject
                final ResolvedMount subResolvedMount = newModel.matchMount("localhost", "/site", "subsite/home");
                assertNotNull(subResolvedMount);
                assertTrue(subResolvedMount.getMount().getParent() == newResolvedMount.getMount());
                assertNotNull(subResolvedMount.getMount().getHstSite());
                assertEquals("/unittestcontent/documents/unittestsubproject", subResolvedMount.getMount().getContentPath());
            } catch (ContainerException e) {
                fail(e.toString());
            }
        });

        // restore the unittestproject
        JcrUtils.copy(session, "/hst-backup/hst:sites/unittestproject", "/hst:hst/hst:sites/unittestproject");
        invalidator.eventPaths("/hst:hst/hst:sites");
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.STALE);
        session.save();
        final VirtualHosts finalModel = hstManager.getVirtualHosts();
        assertNotSame(finalModel, firstModel);
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
        assertTrue( ((HstManagerImpl)hstManager).consecutiveBuildFailCounter == 0);
        final ResolvedMount finalResolvedMount = finalModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject",finalResolvedMount.getMount().getMountPoint());
    }

    @Test
    public void testModelMissingSiteContentProp() throws Exception {
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
        // now remove /hst:hst/hst:sites/unittestproject node
        String origContentValue = session.getNode("/hst:hst/hst:sites/unittestproject").getProperty("hst:content").getString();
        session.getNode("/hst:hst/hst:sites/unittestproject").getProperty("hst:content").remove();
        invalidator.eventPaths("/hst:hst/hst:sites/unittestproject");
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.STALE);
        session.save();

        Log4jInterceptor.onWarn().deny(MountService.class, VirtualHostService.class, VirtualHostsService.class).run( () -> {
            try {
                final VirtualHosts newModel = hstManager.getVirtualHosts();
                assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
                assertTrue(((HstManagerImpl) hstManager).consecutiveBuildFailCounter == 0);
                assertNotSame(firstModel, newModel);
                final ResolvedMount newResolvedMount = newModel.matchMount("localhost", "/site", "home");
                assertNotNull(newResolvedMount);
                assertNull(newResolvedMount.getMount().getContentPath());
                // there is a site!
                assertNotNull(newResolvedMount.getMount().getHstSite());

                // assert we can still match the child mount 'subsite' which has
                // hst:mountpoint = /hst:hst/hst:sites/unittestsubproject
                final ResolvedMount subResolvedMount = newModel.matchMount("localhost", "/site", "subsite/home");
                assertNotNull(subResolvedMount);
                assertTrue(subResolvedMount.getMount().getParent() == newResolvedMount.getMount());
                assertNotNull(subResolvedMount.getMount().getHstSite());
                assertEquals("/unittestcontent/documents/unittestsubproject", subResolvedMount.getMount().getContentPath());
            } catch (ContainerException e) {
                fail(e.toString());
            }
        });

        // restore the unittestproject
        session.getNode("/hst:hst/hst:sites/unittestproject").setProperty("hst:content", origContentValue);
        invalidator.eventPaths("/hst:hst/hst:sites/unittestproject");
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.STALE);
        session.save();
        final VirtualHosts finalModel = hstManager.getVirtualHosts();
        assertNotSame(finalModel, firstModel);
        assertTrue( ((HstManagerImpl)hstManager).state == HstManagerImpl.BuilderState.UP2DATE);
        assertTrue( ((HstManagerImpl)hstManager).consecutiveBuildFailCounter == 0);
        final ResolvedMount finalResolvedMount = finalModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject",finalResolvedMount.getMount().getMountPoint());

        final ResolvedMount finalModelSubResolvedMount = finalModel.matchMount("localhost", "/site", "subsite/home");
        assertNotNull(finalModelSubResolvedMount);
        assertTrue(finalModelSubResolvedMount.getMount().getParent() == finalResolvedMount.getMount());
        assertNotNull(finalModelSubResolvedMount.getMount().getHstSite());
    }

    @Test
    public void testModelMountIsNotMapped() throws Exception {
        final String absPathToRootMount = "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root";
        final String origContentProp = session.getNode("/hst:hst/hst:sites/unittestproject").getProperty("hst:content").getString();
        {
            session.getNode(absPathToRootMount).setProperty("hst:ismapped", false);
            session.save();
            final VirtualHosts firstModel = hstManager.getVirtualHosts();
            final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
            assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
            assertFalse(resolvedMount.getMount().isMapped());
            assertEquals("/unittestcontent/documents/unittestproject", resolvedMount.getMount().getContentPath());
        }

        {
            // now remove the hst:contentpath from the hst:site node : In that case, we expect a mount that has null
            // contentpath : in other words, a wrong mount, but kept because can have correct child mounts

            session.getNode("/hst:hst/hst:sites/unittestproject").getProperty("hst:content").remove();
            session.save();
            invalidator.eventPaths("/hst:hst/hst:sites/unittestproject");

            Log4jInterceptor.onWarn().deny(MountService.class).run( () -> {
                try {
                    final VirtualHosts changedModel = hstManager.getVirtualHosts();
                    final ResolvedMount newResolvedMount = changedModel.matchMount("localhost", "/site", "home");
                    assertNull(newResolvedMount.getMount().getContentPath());
                } catch (ContainerException e) {
                    fail(e.toString());
                }
            });
        }

        {
            // reset content path
            session.getNode("/hst:hst/hst:sites/unittestproject").setProperty("hst:content", origContentProp);
            // now set the mountpoint directly to a content node (REST mount points like for images/assets have this)
            session.getNode(absPathToRootMount).setProperty("hst:mountpoint", "/unittestcontent/documents/unittestproject");
            invalidator.eventPaths(absPathToRootMount, "/hst:hst/hst:sites/unittestproject");
            session.save();
            final VirtualHosts changedModel = hstManager.getVirtualHosts();

            final ResolvedMount newResolvedMount = changedModel.matchMount("localhost", "/site", "home");
            assertEquals("/unittestcontent/documents/unittestproject", newResolvedMount.getMount().getMountPoint());
            assertFalse(newResolvedMount.getMount().isMapped());
            assertEquals("/unittestcontent/documents/unittestproject", newResolvedMount.getMount().getContentPath());
        }

        // now set the mountpoint directly a wrong hst configuration node: We then expect to have a contentpath which
        // is just the same as the mountpoint
        {
            // now set the mountpoint directly to a content node (REST mount points like for images/assets have this)
            session.getNode(absPathToRootMount).setProperty("hst:mountpoint", "/hst:hst/hst:hosts");
            invalidator.eventPaths(absPathToRootMount);
            session.save();
            final VirtualHosts changedModel = hstManager.getVirtualHosts();

            final ResolvedMount newResolvedMount = changedModel.matchMount("localhost", "/site", "home");
            assertEquals("/hst:hst/hst:hosts", newResolvedMount.getMount().getMountPoint());
            assertFalse(newResolvedMount.getMount().isMapped());
            assertEquals("/hst:hst/hst:hosts", newResolvedMount.getMount().getContentPath());
        }
    }

    @Test
    public void testProjectMissingHstPages() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages").remove();
        session.save();
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());

        for (HstComponentConfiguration compConfig : resolvedMount.getMount().getHstSite().getComponentsConfiguration().getComponentConfigurations().values()) {
            assertFalse(compConfig.getCanonicalStoredLocation().contains("/hst:pages/"));
        }
    }

    @Test
    public void testProjectAndDefaultMissingHstPages() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages").remove();
        session.getNode("/hst:hst/hst:configurations/hst:default/hst:pages").remove();
        session.save();
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());

        for (HstComponentConfiguration compConfig : resolvedMount.getMount().getHstSite().getComponentsConfiguration().getComponentConfigurations().values()) {
            assertFalse(compConfig.getCanonicalStoredLocation().contains("/hst:pages/"));
        }
    }


    @Test
    public void testProjectMissingHstComponents() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components").remove();
        session.save();
        Log4jInterceptor.onWarn().deny(HstComponentConfigurationService.class).run( () -> {
            try {
                final VirtualHosts firstModel = hstManager.getVirtualHosts();
                final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
                assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());

                for (HstComponentConfiguration compConfig : resolvedMount.getMount().getHstSite().getComponentsConfiguration().getComponentConfigurations().values()) {
                    assertFalse(compConfig.getCanonicalStoredLocation().contains("/hst:components/"));
                }
            } catch (ContainerException e) {
                fail(e.toString());
            }
        });
    }

    @Test
    public void testProjectAndDefaultMissingHstComponents() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components").remove();
        session.getNode("/hst:hst/hst:configurations/hst:default/hst:components").remove();
        session.save();
        Log4jInterceptor.onWarn().deny(HstComponentConfigurationService.class).run( () -> {
            try {
                final VirtualHosts firstModel = hstManager.getVirtualHosts();
                final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
                assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());

                for (HstComponentConfiguration compConfig : resolvedMount.getMount().getHstSite().getComponentsConfiguration().getComponentConfigurations().values()) {
                    assertFalse(compConfig.getCanonicalStoredLocation().contains("/hst:components/"));
                }
            } catch (ContainerException e) {
                fail(e.toString());
            }
        });
    }


    @Test
    public void testProjectMissingHstPageAndComponents() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components").remove();
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages").remove();
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages").remove();

        session.save();
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
        assertTrue(resolvedMount.getMount().getHstSite().getComponentsConfiguration().getComponentConfigurations().size() == 0);
    }

    @Test
    public void testProjectAndDefaultMissingHstPageAndComponents() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components").remove();
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages").remove();
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages").remove();
        session.getNode("/hst:hst/hst:configurations/hst:default/hst:components").remove();
        session.getNode("/hst:hst/hst:configurations/hst:default/hst:pages").remove();
        session.save();
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
        assertTrue(resolvedMount.getMount().getHstSite().getComponentsConfiguration().getComponentConfigurations().size() == 0);
    }


    @Test
    public void testProjectMissingSingleReferencedComponent() throws Exception {

        // through hst:referencecomponent = hst:pages/basepage a header should be inherited
        {
            final VirtualHosts model = hstManager.getVirtualHosts();
            final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
            final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem("/home");
            assertNotNull(resolvedSiteMapItem.getHstComponentConfiguration());
            assertNotNull(resolvedSiteMapItem.getHstComponentConfiguration().getChildByName("header"));
        }

        // now break the references
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage").setProperty("hst:referencecomponent", "nonexisting....");
        invalidator.eventPaths("/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage");
        session.save();
        {
            Log4jInterceptor.onWarn().deny(HstComponentConfigurationService.class).run( () -> {
                try {
                    final VirtualHosts model = hstManager.getVirtualHosts();
                    final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
                    final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem("/home");
                    assertNotNull(resolvedSiteMapItem.getHstComponentConfiguration());
                    // now header component is not inherited any more, and should thus be missing
                    assertNull(resolvedSiteMapItem.getHstComponentConfiguration().getChildByName("header"));
                } catch (ContainerException e) {
                    fail(e.toString());
                }
            });
        }
    }

    @Test
    public void testProjectNoSiteMap() throws Exception {

        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();

        final Node defaultSiteMap = session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemap");
        for (Node siteMapItem : new NodeIterable(defaultSiteMap.getNodes())) {
            siteMapItem.remove();
        }
        session.save();
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
        final HstSiteService hstSite = (HstSiteService)resolvedMount.getMount().getHstSite();
        assertTrue(hstSite.getSiteMap().getSiteMapItems().size() == 0);
        // since hst:default contains a hst:sitemap node, we still have a sitemap instance
        assertFalse(hstSite.getSiteMap() instanceof HstNoopSiteMap);
    }

    @Test
    public void testProjectAndDefaultNoSiteMap() throws Exception {
        session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemap").remove();
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();
        session.save();
        Log4jInterceptor.onWarn().deny(HstSiteService.class).run( () -> {
            try {
                final VirtualHosts firstModel = hstManager.getVirtualHosts();
                final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
                assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
                final HstSiteService hstSite = (HstSiteService) resolvedMount.getMount().getHstSite();
                final HstSiteMap siteMap = hstSite.getSiteMap();
                assertTrue(siteMap instanceof HstNoopSiteMap);
            } catch (ContainerException e) {
                log.error("ContainerException", e);
                fail(e.toString());
            }
        });
    }

    @Test
    public void testSiteMapItemReferencesNonExistingComponent() throws Exception {
        {
            VirtualHosts model = hstManager.getVirtualHosts();
            final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
            assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
            final HstSiteService hstSite = (HstSiteService)resolvedMount.getMount().getHstSite();

            final HstSiteMap siteMap = hstSite.getSiteMap();
            assertNotNull(siteMap.getSiteMapItem("home"));
            assertEquals("hst:pages/homepage", siteMap.getSiteMapItem("home").getComponentConfigurationId());
            final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem("/home");
            assertNotNull(resolvedSiteMapItem.getHstComponentConfiguration());
            assertEquals("/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage",
                    resolvedSiteMapItem.getHstComponentConfiguration().getCanonicalStoredLocation());
        }

        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home").setProperty("hst:componentconfigurationid","nonexisting");
        session.save();
        invalidator.eventPaths("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home");
        {
            Log4jInterceptor.onWarn().deny(ResolvedSiteMapItemImpl.class).run( () -> {
                try {
                    final VirtualHosts model = hstManager.getVirtualHosts();
                    final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
                    assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
                    final HstSiteService hstSite = (HstSiteService) resolvedMount.getMount().getHstSite();

                    final HstSiteMap siteMap = hstSite.getSiteMap();
                    assertNotNull(siteMap.getSiteMapItem("home"));
                    assertEquals("nonexisting", siteMap.getSiteMapItem("home").getComponentConfigurationId());
                    final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem("/home");
                    assertNull(resolvedSiteMapItem.getHstComponentConfiguration());
                } catch (ContainerException e) {
                    fail(e.toString());
                }
            });
        }
    }


    @Test
    public void testProjectMissingOwnAndInheritedSiteMenus() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemenus").remove();
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:sitemenus").remove();
        session.save();
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
        assertTrue(resolvedMount.getMount().getHstSite().getSiteMenusConfiguration().getSiteMenuConfigurations().size() == 0);
    }

    @Test
    public void testProjectMissingDefaultOwnAndInheritedSiteMenus() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemenus").remove();
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:sitemenus").remove();
        session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemenus").remove();
        session.save();
        final VirtualHosts firstModel = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = firstModel.matchMount("localhost", "/site", "home");
        assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());
        assertNull(resolvedMount.getMount().getHstSite().getSiteMenusConfiguration());
    }


    @Test
    public void testProjectMissingOwnAndInheritedTemplates() throws Exception {

        {
            final VirtualHosts model = hstManager.getVirtualHosts();
            final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
            assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());

            final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem("/home");
            assertNotNull(resolvedSiteMapItem.getHstComponentConfiguration());

            final String renderPath = resolvedSiteMapItem.getHstComponentConfiguration().getRenderPath();
            final String hstTemplate = ((HstComponentConfigurationService) resolvedSiteMapItem.getHstComponentConfiguration()).getHstTemplate();

            assertEquals("webpage", hstTemplate);
            assertEquals("jsp/webpage.jsp", renderPath);
        }

        // now remove templates
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:templates").remove();
        invalidator.eventPaths("/hst:hst/hst:configurations/unittestcommon");
        session.save();
        {
            Log4jInterceptor.onWarn().deny(HstComponentConfigurationService.class).run( () -> {
                try {
                    final VirtualHosts model = hstManager.getVirtualHosts();
                    final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
                    assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());

                    final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem("/home");
                    assertNotNull(resolvedSiteMapItem.getHstComponentConfiguration());

                    final String renderPath = resolvedSiteMapItem.getHstComponentConfiguration().getRenderPath();
                    final String hstTemplate = ((HstComponentConfigurationService) resolvedSiteMapItem.getHstComponentConfiguration()).getHstTemplate();

                    assertEquals("webpage", hstTemplate);
                    assertNull(renderPath);
                } catch (ContainerException e) {
                    fail(e.toString());
                }
            });
        }

    }

    @Test
    public void testProjectMissingDefaultOwnAndInheritedTemplates() throws Exception {
        // now remove templates
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:templates").remove();
        session.getNode("/hst:hst/hst:configurations/hst:default/hst:templates").remove();
        session.save();

        Log4jInterceptor.onWarn().deny(HstComponentConfigurationService.class).run( () -> {
            try {
                final VirtualHosts model = hstManager.getVirtualHosts();
                final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
                assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());

                final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem("/home");
                assertNotNull(resolvedSiteMapItem.getHstComponentConfiguration());
                final String renderPath = resolvedSiteMapItem.getHstComponentConfiguration().getRenderPath();
                final String hstTemplate = ((HstComponentConfigurationService) resolvedSiteMapItem.getHstComponentConfiguration()).getHstTemplate();

                assertEquals("webpage", hstTemplate);
                assertNull(renderPath);
            } catch (ContainerException e) {
                fail(e.toString());
            }
        });
    }


    @Test
    public void testProjectMissingChannels() throws Exception {

        QueryResult result = session.getWorkspace().getQueryManager().createQuery("/jcr:root/hst:hst//element(*,hst:channel)", "xpath").execute();
        for (Node node : new NodeIterable(result.getNodes())) {
            node.remove();
        }
        session.save();

        try (Log4jInterceptor ignored = Log4jInterceptor.onWarn().deny(VirtualHostsService.class).build()) {
            final VirtualHosts model = hstManager.getVirtualHosts();
            final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
            assertEquals("/hst:hst/hst:sites/unittestproject", resolvedMount.getMount().getMountPoint());

            assertTrue(hstManager.getVirtualHosts().getChannels("dev-localhost").size() == 0);
            assertNull(resolvedMount.getMount().getChannel());
        }
    }

    @Test
    public void testProjectMissingSingleChannel() throws Exception {

        final String channelName;
        {
            final VirtualHosts model = hstManager.getVirtualHosts();
            final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
            assertNotNull(resolvedMount.getMount().getChannel());
            channelName = resolvedMount.getMount().getChannel().getId();
        }

        session.getNode("/hst:hst/hst:configurations/"+channelName + "/hst:channel").remove();
        invalidator.eventPaths("/hst:hst/hst:configurations/"+channelName);
        session.save();
        {
            try (Log4jInterceptor ignored = Log4jInterceptor.onWarn().deny(VirtualHostsService.class).build()) {
                final VirtualHosts model = hstManager.getVirtualHosts();
                final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
                assertNull(resolvedMount.getMount().getChannel());
            }
        }
    }

    @Test
    public void test_preview_channel_instance_is_different_than_live_even_if_there_is_no_preview_configuration() throws Exception {
        String configPath;
        {
            VirtualHosts model = hstManager.getVirtualHosts();
            final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
            final ContextualizableMount mount = (ContextualizableMount) resolvedMount.getMount();
            assertFalse("there is no preview channel but still preview channel should be a different instance than the " +
                            "live channel",
                    mount.getChannel() == mount.getPreviewChannel());
            assertEquals("Since there is no preview channel, the id must be the same",
                    mount.getChannel().getId(), mount.getPreviewChannel().getId());

            configPath = mount.getHstSite().getConfigurationPath();
        }

        JcrUtils.copy(session, configPath, configPath+"-preview");
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        {
            VirtualHosts model = hstManager.getVirtualHosts();
            final ResolvedMount resolvedMount = model.matchMount("localhost", "/site", "home");
            final ContextualizableMount mount = (ContextualizableMount) resolvedMount.getMount();
            assertEquals("Since there is a preview channel, the id must be the same as live id + '-preview'",
                    mount.getChannel().getId() + "-preview", mount.getPreviewChannel().getId());
        }

    }

}
