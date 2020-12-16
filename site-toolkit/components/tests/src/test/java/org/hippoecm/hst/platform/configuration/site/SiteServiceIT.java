/*
 * Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.site;

import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.container.site.CompositeHstSite;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.hosting.MountService;
import org.hippoecm.hst.platform.container.site.DelegatingHstSiteProvider;
import org.hippoecm.hst.platform.model.HstModelImpl;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.request.ResolvedMountImpl;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.springframework.mock.web.MockHttpServletRequest;

import com.google.common.base.Optional;

import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_OF;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_BRANCH;
import static org.hippoecm.hst.configuration.HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SiteServiceIT extends AbstractTestConfigurations {

    private HstManager hstManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        hstManager = getComponent(HstManager.class.getName());


        // first trigger the virtual hosts to be invalidated otherwise the loaded model from a preview test method
        // might be loaded already
        ((HstModelImpl)hstModelSite1).invalidate();
        HstConfigurationLoadingCache hstConfigurationLoadingCache = on(hstModelSite1).field("hstConfigurationLoadingCache").get();
        hstConfigurationLoadingCache.clear();

    }

    @Test
    public void componentsConfigurationLoadedLazilyAndInstancesShared() throws Exception {
        // both hosts below have a mount that results in the same configuration path
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        final HstSiteService hstSite1 = (HstSiteService) mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService) mount2.getMount().getHstSite();

        assertNull(hstSite1.componentsConfiguration);
        assertNull(hstSite2.componentsConfiguration);

        hstSite1.getComponentsConfiguration();
        assertNotNull(hstSite1.componentsConfiguration);
        assertNull(hstSite2.componentsConfiguration);
        assertSame(hstSite2.getComponentsConfiguration(), hstSite1.componentsConfiguration.get());
    }

    @Test
    public void previewSiteComponentsConfigurationLoadedLazilyAndInstancesShared() throws Exception {
        final ResolvedMount resMount = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ContextualizableMount mount = (ContextualizableMount) resMount.getMount();

        final HstSiteService hstSite = (HstSiteService) mount.getHstSite();
        final HstSiteService previewHstSite = (HstSiteService) mount.getPreviewHstSite();

        assertNotSame(hstSite, previewHstSite);
        assertNull(hstSite.componentsConfiguration);
        assertNull(previewHstSite.componentsConfiguration);
        hstSite.getComponentsConfiguration();
        previewHstSite.getComponentsConfiguration();
        assertNotNull(hstSite.componentsConfiguration);
        assertNotNull(previewHstSite.componentsConfiguration);
        assertSame(hstSite.componentsConfiguration.get(), previewHstSite.componentsConfiguration.get());
    }

    @Test
    public void previewSiteComponentsConfigurationNotLoadedForCurrentHostGroupIfNoPreviewChannel() throws Exception {

        final ResolvedMount resMount = hstManager.getVirtualHosts().matchMount("localhost",  "/");
        final ContextualizableMount mount = (ContextualizableMount) resMount.getMount();

        final HstSiteService hstSite = (HstSiteService) mount.getHstSite();
        final HstSiteService previewHstSite = (HstSiteService) mount.getPreviewHstSite();

        assertNotSame(hstSite, previewHstSite);
        assertNull(hstSite.componentsConfiguration);
        assertNull(previewHstSite.componentsConfiguration);

        final VirtualHosts virtualHosts = resMount.getMount().getVirtualHost().getVirtualHosts();
        assertTrue(virtualHosts.getChannelById("dev-localhost", "unittestproject").getChangedBySet() instanceof HashSet);

        virtualHosts.getChannelById("dev-localhost", "unittestproject").getChangedBySet().size();
        // only when there is a PREVIEW channel, a ChannelLazyLoadingChangedBySet is created. When there is not preview channel
        // the componentsConfiguration is still not loaded
        assertNull(previewHstSite.componentsConfiguration);

    }


    @Test
    public void previewSiteComponentsConfigurationLoadedLazilyForCurrentHostGroup() throws Exception {
        // first make sure there is a preview channel
        Session session = createSession();
        try {
            createHstConfigBackup(session);
            {
                final ResolvedMount resMount = hstManager.getVirtualHosts().matchMount("localhost",  "/");
                final ContextualizableMount mount = (ContextualizableMount) resMount.getMount();
                // since we do not have a preview yet, previewChannel and live channel should have the same #getId, though
                // there instances are different
                assertNotSame(mount.getPreviewChannel(), mount.getChannel());
                assertEquals("Since there is not preview, the preview channel id should be same as the live channel" +
                        " instance", mount.getPreviewChannel().getId(), mount.getChannel().getId());

                final HstSiteService hstSite = (HstSiteService) mount.getHstSite();
                // create preview configuration and preview channel
                String configPath = hstSite.getConfigurationPath();
                JcrUtils.copy(session, configPath, configPath + "-preview");
                // add a change by setting 'lockedby' on preview channel node
                session.getNode(configPath + "-preview/hst:channel").setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "someonelikeyou");
            }
            String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
            saveSession(session);
            invalidator.eventPaths(pathsToBeChanged);

            {
                final ResolvedMount resMount = hstManager.getVirtualHosts().matchMount("localhost",  "/");
                final ContextualizableMount mount = (ContextualizableMount) resMount.getMount();
                assertNotSame("Since there is *a* preview, the preview channel instance should NOT be same as the live channel" +
                        " instance", mount.getPreviewChannel(), mount.getChannel());
                final HstSiteService hstSite = (HstSiteService) mount.getHstSite();
                // componentsConfiguration is loaded lazily
                assertNull(hstSite.componentsConfiguration);
                final VirtualHosts virtualHosts = resMount.getMount().getVirtualHost().getVirtualHosts();

                assertEquals(0, virtualHosts.getChannelById("dev-localhost", "unittestproject").getChangedBySet().size());

                final HstSiteService previewHstSite = (HstSiteService) mount.getPreviewHstSite();
                // only when changes from preview channel are loaded the lazy component configuration gets populated
                assertNull(previewHstSite.componentsConfiguration);

                assertEquals(1, virtualHosts.getChannelById("dev-localhost", "unittestproject-preview").getChangedBySet().size());
                assertTrue(virtualHosts.getChannelById("dev-localhost", "unittestproject-preview").getChangedBySet().contains("someonelikeyou"));
                // since changedBySet is request on preview channel, we expect previewHstSite.componentsConfiguration not to be
                // null any more.
                assertNull(hstSite.componentsConfiguration);
                assertNotNull(previewHstSite.componentsConfiguration);
            }

        } finally {
            restoreHstConfigBackup(session);
            session.logout();
        }

    }

    @Test
    public void componentsConfigurationLoadedLazilyUnlessPresentInCache() throws Exception {
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        final HstSiteService hstSite1 = (HstSiteService) mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService) mount2.getMount().getHstSite();
        assertNull(hstSite1.componentsConfiguration);
        assertNull(hstSite2.componentsConfiguration);

        hstSite1.getComponentsConfiguration();

        assertNotNull(hstSite1.componentsConfiguration);
        // the componentsConfiguration for hstSite2 is never loaded before!
        assertNull(hstSite2.componentsConfiguration);

        hstSite2.getComponentsConfiguration();

        assertNotNull(hstSite2.componentsConfiguration);

        assertNotSame(hstSite1.componentsConfiguration, hstSite2.componentsConfiguration);
        assertSame(hstSite1.componentsConfiguration.get(), hstSite2.componentsConfiguration.get());

        // we now invalidate the hst:hosts node by an explicit event
        invalidator.eventPaths(new String[]{"/hst:hst/hst:hosts"});

        final ResolvedMount mountAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mountAfter2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        assertNotSame(mountAfter1, mount1);
        assertNotSame(mountAfter2, mount2);

        final HstSiteService hstSiteAfter1 = (HstSiteService) mountAfter1.getMount().getHstSite();
        final HstSiteService hstSiteAfter2 = (HstSiteService) mountAfter2.getMount().getHstSite();

        assertNotSame(hstSiteAfter1, hstSite1);
        assertNotSame(hstSiteAfter2, hstSite2);

        // since the previously loaded hstSite1.componentsConfiguration is not affected by event "/hst:hst/hst:hosts"
        // the 'componentsConfiguration' should be repopulated from cache during HstComponentsConfiguration constructor

        assertNotNull(hstSiteAfter1.componentsConfiguration);
        assertNotNull(hstSiteAfter2.componentsConfiguration);

        assertSame(hstSiteAfter1.componentsConfiguration.get(), hstSiteAfter2.componentsConfiguration.get());

        assertNotSame(hstSiteAfter1.componentsConfiguration, hstSite1.componentsConfiguration);

        assertSame(hstSiteAfter1.componentsConfiguration.get(), hstSite1.componentsConfiguration.get());
        assertSame(hstSiteAfter2.componentsConfiguration.get(), hstSite2.componentsConfiguration.get());
    }


    @Test
    public void sitemapLoadedLazily() throws Exception {

        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        final HstSiteService hstSite1 = (HstSiteService) mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService) mount2.getMount().getHstSite();

        assertNull(hstSite1.siteMap);
        hstSite1.getSiteMap();

        assertNotNull(hstSite1.siteMap);
        // loading sitemap should not trigger location map : vice versa it will
        assertNull(hstSite1.locationMapTree);

        assertNull(hstSite2.siteMap);
        hstSite2.getSiteMap();
        assertNotNull(hstSite2.siteMap);

        // siteMap instances have a reference to their HstSite so can't be shared, even if they share the exact same
        // configuration
        assertNotSame(hstSite1.getSiteMap(), hstSite2.getSiteMap());

    }

    @Test
    public void locationMapLoadedLazily() throws Exception {
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        final HstSiteService hstSite1 = (HstSiteService) mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService) mount2.getMount().getHstSite();

        assertNull(hstSite1.locationMapTree);
        hstSite1.getLocationMapTree();
        assertNotNull(hstSite1.locationMapTree);
        // locationmap loading also triggers sitemap
        assertNotNull(hstSite1.siteMap);


        assertNull(hstSite2.locationMapTree);
        hstSite2.getLocationMapTree();
        assertNotNull(hstSite2.locationMapTree);

        // location map instances have a reference to their HstSite so can't be shared, even if they share the exact same
        // configuration
        assertNotSame(hstSite1.getLocationMapTree(), hstSite2.getLocationMapTree());

    }

    @Test
    public void siteMapItemHandlersLoadedLazilyAndInstancesShared() throws Exception {
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        final HstSiteService hstSite1 = (HstSiteService) mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService) mount2.getMount().getHstSite();

        assertNull(hstSite1.siteMapItemHandlersConfigurationService);
        hstSite1.getSiteMapItemHandlersConfiguration();
        assertNotNull(hstSite1.siteMapItemHandlersConfigurationService);

        assertNull(hstSite2.siteMapItemHandlersConfigurationService);
        hstSite2.getSiteMapItemHandlersConfiguration();
        assertNotNull(hstSite2.siteMapItemHandlersConfigurationService);

        // HstSiteMapItemHandlersConfiguration instances are shared for same configurations
        assertSame(hstSite1.getSiteMapItemHandlersConfiguration(), hstSite2.getSiteMapItemHandlersConfiguration());

    }

    @Test
    public void siteMapItemHandlersLoadedLazilyUnlessPresentInCache() throws Exception {
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        final HstSiteService hstSite1 = (HstSiteService) mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService) mount2.getMount().getHstSite();
        assertNull(hstSite1.siteMapItemHandlersConfigurationService);
        assertNull(hstSite2.siteMapItemHandlersConfigurationService);

        hstSite1.getSiteMapItemHandlersConfiguration();

        assertNotNull(hstSite1.siteMapItemHandlersConfigurationService);
        // the HstSiteMapItemHandlersConfiguration for hstSite2 is never loaded before!
        assertNull(hstSite2.siteMapItemHandlersConfigurationService);

        hstSite2.getSiteMapItemHandlersConfiguration();

        assertNotNull(hstSite2.siteMapItemHandlersConfigurationService);

        assertNotSame(hstSite1.siteMapItemHandlersConfigurationService, hstSite2.siteMapItemHandlersConfigurationService);
        assertSame(hstSite1.siteMapItemHandlersConfigurationService.get(), hstSite2.siteMapItemHandlersConfigurationService.get());

        // we now invalidate the hst:hosts node by an explicit event
        invalidator.eventPaths(new String[]{"/hst:hst/hst:hosts"});

        final ResolvedMount mountAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mountAfter2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        assertNotSame(mountAfter1, mount1);
        assertNotSame(mountAfter2, mount2);

        final HstSiteService hstSiteAfter1 = (HstSiteService) mountAfter1.getMount().getHstSite();
        final HstSiteService hstSiteAfter2 = (HstSiteService) mountAfter2.getMount().getHstSite();

        assertNotSame(hstSiteAfter1, hstSite1);
        assertNotSame(hstSiteAfter2, hstSite2);

        // since the previously loaded hstSite1.HstSiteMapItemHandlersConfiguration is not affected by event "/hst:hst/hst:hosts"
        // the 'HstSiteMapItemHandlersConfiguration' should be repopulated from cache during HstComponentsConfiguration constructor

        assertNotNull(hstSiteAfter1.siteMapItemHandlersConfigurationService);
        assertNotNull(hstSiteAfter2.siteMapItemHandlersConfigurationService);

        assertSame(hstSiteAfter1.siteMapItemHandlersConfigurationService.get(), hstSiteAfter2.siteMapItemHandlersConfigurationService.get());

        assertNotSame(hstSiteAfter1.siteMapItemHandlersConfigurationService, hstSite1.siteMapItemHandlersConfigurationService);

        assertSame(hstSiteAfter1.siteMapItemHandlersConfigurationService.get(), hstSite1.siteMapItemHandlersConfigurationService.get());
        assertSame(hstSiteAfter2.siteMapItemHandlersConfigurationService.get(), hstSite2.siteMapItemHandlersConfigurationService.get());
    }

    @Test
    public void sitemenusLoadedLazily() throws Exception {

        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");

        final HstSiteService hstSite1 = (HstSiteService) mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService) mount2.getMount().getHstSite();

        assertNull(hstSite1.siteMenusConfigurations);
        hstSite1.getSiteMenusConfiguration();

        assertNotNull(hstSite1.siteMenusConfigurations);

        assertNull(hstSite2.siteMenusConfigurations);
        hstSite2.getSiteMenusConfiguration();
        assertNotNull(hstSite2.siteMenusConfigurations);

        // getSiteMenusConfiguration instances have a reference to their HstSite so can't be shared, even if they share the exact same
        // configuration
        assertNotSame(hstSite1.getSiteMenusConfiguration(), hstSite2.getSiteMenusConfiguration());

    }


    public static Optional<HstSiteMap> getSiteMapInstanceVariable(HstSiteService siteService) {
        return siteService.siteMap;
    }

    @Test
    public void channels_for_branches_get_loaded_as_well() throws Exception {
        Session session = createSession();
        try {
            createHstConfigBackup(session);
            createBranch(session, "unittestproject-branchid-000", "branchid-000");
            Channel branchChannel = hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject-branchid-000");
            assertNotNull(branchChannel);
            assertEquals("branchid-000", branchChannel.getBranchId());
            assertEquals("unittestproject", branchChannel.getBranchOf());

            Channel masterChannel = hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject");
            assertEquals(masterChannel.getMountId(), branchChannel.getMountId());
            assertEquals("Because the branch does not have its own channel node, the channel node is inherited",
                    masterChannel.getChannelPath(), branchChannel.getChannelPath());

            Mount mount = hstManager.getVirtualHosts().getMountByIdentifier(branchChannel.getMountId());
            assertTrue(mount.getHstSite() instanceof CompositeHstSite);

            Channel subChannel = hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestsubproject");
            Mount subMount = hstManager.getVirtualHosts().getMountByIdentifier(subChannel.getMountId());
            assertFalse(subMount.getHstSite() instanceof CompositeHstSite);
        } finally {
            restoreHstConfigBackup(session);
            session.logout();
        }
    }

    private void createBranch(final Session session, final String name, final String branchId) throws RepositoryException {
        Node branchNode = session.getNode("/hst:hst/hst:configurations").addNode(name);
        branchNode.addNode(NODENAME_HST_WORKSPACE, NODETYPE_HST_WORKSPACE);
        branchNode.addMixin(MIXINTYPE_HST_BRANCH);
        branchNode.setProperty(BRANCH_PROPERTY_BRANCH_OF, "unittestproject");
        branchNode.setProperty(BRANCH_PROPERTY_BRANCH_ID, branchId);
        branchNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM, new String[]{"../unittestproject"});
        saveSession(session);
    }

    @Test
    public void branch_channel_that_does_not_point_to_a_existing_master_is_not_loaded() throws Exception {
        Session session = createSession();
        try {
            createHstConfigBackup(session);
            createBranch(session, "unittestproject-branchid-000", "branchid-000");
            Node branchNode = session.getNode("/hst:hst/hst:configurations/unittestproject-branchid-000");
            branchNode.setProperty(BRANCH_PROPERTY_BRANCH_OF, "nonexisting");
            saveSession(session);
            assertNull(hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject-branchid-000"));
        } finally {
            restoreHstConfigBackup(session);
            session.logout();
        }
    }

    @Test
    public void branch_can_start_with_other_name_than_master() throws Exception {
        assertions("othername-branchid-000", "branchid-000");
    }

    private void assertions(final String name, final String branchId) throws RepositoryException, ContainerException {
        Session session = createSession();
        try {
            createHstConfigBackup(session);
            // since the branch is a branchof 'unittestproject', it should start with the name 'unittestproject-' and is
            // thus incorrect like this and hence won't be loaded
            createBranch(session, name, branchId);
            saveSession(session);
            Channel branch = hstManager.getVirtualHosts().getChannels("dev-localhost").get(name);
            assertEquals("unittestproject", branch.getBranchOf());
            assertEquals(branchId, branch.getBranchId());
            assertNotNull(branch);
        } finally {
            restoreHstConfigBackup(session);
            session.logout();
        }
    }

    @Test
    public void no_dash_after_master_name_is_not_a_problem() throws Exception {
        assertions("unittestprojectbranchid-000", "branchid-000");
    }

    @Test
    public void hst_site_not_allowed_to_point_to_branch() throws Exception {
        Session session = createSession();
        try {
            createHstConfigBackup(session);
            createBranch(session, "unittestproject-branchid-000", "branchid-000");
            // add a hst:site node
            session.getNode("/hst:hst/hst:sites").addNode("unittestproject-branchid-000");
            JcrUtils.copy(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite",
                    "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/unittestproject-branchid-000");
            Node mount = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/unittestproject-branchid-000");
            mount.setProperty(MOUNT_PROPERTY_MOUNTPOINT, "/hst:hst/hst:sites/unittestproject-branchid-000");
            saveSession(session);
            // now assert that the resolved mount for unittestproject-branchid-000 has a HstSite that is null because
            // a hst:site is not allowed to point to a branch. Note that the mount 'unittestproject-branchid-000' is added
            // nonetheless because the mount can have valid child mounts
            try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(MountService.class).build()) {
                final ResolvedMount resolvedMount = hstManager.getVirtualHosts().matchMount("localhost",  "/unittestproject-branchid-000");
                assertNull(resolvedMount.getMount().getHstSite());
                // assert we had a warning about incorrectly configured channel
                assertTrue(interceptor.messages().anyMatch(m -> m.contains("Configured Mount '/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/unittestproject-branchid-000' is incorrect")));
            }
        } finally {
            restoreHstConfigBackup(session);
            session.logout();
        }
    }

    @Test
    public void existing_preview_of_master_does_not_influence_branch_channels() throws Exception {
        Session session = createSession();
        try {
            createHstConfigBackup(session);
            createBranch(session, "unittestproject-branchid-000", "branchid-000");
            JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject",
                    "/hst:hst/hst:configurations/unittestproject-preview");
            saveSession(session);
            assertNotNull(hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject-preview"));
            assertNull(hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject-branchid-000-preview"));
        } finally {
            restoreHstConfigBackup(session);
            session.logout();
        }
    }

    @Test
    public void preview_of_branch_is_loaded_in_channels() throws Exception {
        Session session = createSession();
        try {
            createHstConfigBackup(session);
            createBranch(session, "unittestproject-branchid-000", "branchid-000");
            JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject-branchid-000",
                    "/hst:hst/hst:configurations/unittestproject-branchid-000-preview");
            // preview branch extends from the live branch
            session.getNode("/hst:hst/hst:configurations/unittestproject-branchid-000-preview").setProperty(GENERAL_PROPERTY_INHERITS_FROM,
                    new String[]{"../unittestproject-branchid-000"});
            saveSession(session);
            assertNotNull(hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject-branchid-000-preview"));
        } finally {
            restoreHstConfigBackup(session);
            session.logout();
        }
    }

    @Test
    public void render_different_hst_site_for_custom_channel_manager_site_provider() throws Exception {
        Session session = createSession();
        try {
            HstServices.getComponentManager()
                    .getComponent(DelegatingHstSiteProvider.class, "org.hippoecm.hst.platform")
                    .setChannelManagerHstSiteProvider((master, branches, requestContext) -> branches.get("branchid-000"));

            createHstConfigBackup(session);
            createBranch(session, "unittestproject-branchid-000", "branchid-000");
            saveSession(session);

            Node mountNode = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            Channel branch = hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject-branchid-000");
            MockHstRequestContext ctx = new MockHstRequestContext();
            Mount mount = hstManager.getVirtualHosts().getMountByIdentifier(mountNode.getIdentifier());
            ctx.setResolvedMount(new ResolvedMountImpl(mount, null, null, null, 0));
            ctx.setChannelManagerPreviewRequest(true);
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            ctx.setServletRequest(servletRequest);

            ModifiableRequestContextProvider.set(ctx);

            final ResolvedMount resolvedMount = hstManager.getVirtualHosts().matchMount("localhost",  "");
            Channel channel = resolvedMount.getMount().getChannel();
            assertSame(channel, resolvedMount.getMount().getHstSite().getChannel());
            assertSame("Expected that the branch channel would be matched returned due to custom channel mngr hst site provider",
                    channel, branch);
        } finally {
            HstServices.getComponentManager().getComponent(DelegatingHstSiteProvider.class, "org.hippoecm.hst.platform")
                    .setChannelManagerHstSiteProvider((master, branches, requestContext) -> master);
            restoreHstConfigBackup(session);
            session.logout();
            ModifiableRequestContextProvider.clear();
        }
    }

    @Test
    public void render_different_hst_site_for_website() throws Exception {
        Session session = createSession();
        try {
            HstServices.getComponentManager()
                    .getComponent(DelegatingHstSiteProvider.class, "org.hippoecm.hst.platform")
                    .setWebsiteHstSiteProvider((master, branches, requestContext) -> branches.get("branchid-000"));

            createHstConfigBackup(session);
            createBranch(session, "unittestproject-branchid-000", "branchid-000");
            saveSession(session);

            Node mountNode = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
            Channel branch = hstManager.getVirtualHosts().getChannels("dev-localhost").get("unittestproject-branchid-000");

            MockHstRequestContext ctx = new MockHstRequestContext();
            Mount mount = hstManager.getVirtualHosts().getMountByIdentifier(mountNode.getIdentifier());
            ctx.setResolvedMount(new ResolvedMountImpl(mount, null, null, null, 0));
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            ctx.setServletRequest(servletRequest);
            ModifiableRequestContextProvider.set(ctx);

            // now assert that the resolved mount for unittestproject-branchid-000 has a HstSite that is null because
            // a hst:site is not allowed to point to a branch. Note that the mount 'unittestproject-branchid-000' is added
            // nonetheless because the mount can have valid child mounts
            final ResolvedMount resolvedMount = hstManager.getVirtualHosts().matchMount("localhost",  "");
            Channel channel = resolvedMount.getMount().getChannel();
            assertSame(channel, resolvedMount.getMount().getHstSite().getChannel());

            assertSame("Expected that the branch channel would be matched", channel, branch);
        } finally {
            HstServices.getComponentManager().getComponent(DelegatingHstSiteProvider.class, "org.hippoecm.hst.platform")
                    .setWebsiteHstSiteProvider((master, branches, requestContext) -> master);
            restoreHstConfigBackup(session);
            session.logout();
            ModifiableRequestContextProvider.clear();
        }
    }

    private void saveSession(final Session session) throws RepositoryException {
        session.save();
        //TODO SS: Clarify what could be the cause of failures without delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {}
    }
}
