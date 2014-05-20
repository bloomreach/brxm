/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;

import java.util.HashSet;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.Assert;

import com.google.common.base.Optional;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TestSiteService extends AbstractTestConfigurations {

    private HstManager hstManager;
    private EventPathsInvalidator invalidator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        hstManager = getComponent(HstManager.class.getName());
    }

    @Test
    public void componentsConfigurationLoadedLazilyAndInstancesShared() throws Exception {
        // both hosts below have a mount that results in the same configuration path
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        final HstSiteService hstSite1 = (HstSiteService)mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService)mount2.getMount().getHstSite();

        assertNull(hstSite1.componentsConfiguration);
        assertNull(hstSite2.componentsConfiguration);

        hstSite1.getComponentsConfiguration();
        assertNotNull(hstSite1.componentsConfiguration);
        assertNull(hstSite2.componentsConfiguration);
        assertSame(hstSite2.getComponentsConfiguration(), hstSite1.componentsConfiguration.get());
    }

    @Test
    public void previewSiteComponentsConfigurationLoadedLazilyAndInstancesShared() throws Exception {
        final ResolvedMount resMount = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ContextualizableMount mount = (ContextualizableMount)resMount.getMount();

        final HstSiteService hstSite = (HstSiteService)mount.getHstSite();
        final HstSiteService previewHstSite = (HstSiteService)mount.getPreviewHstSite();

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

        final ResolvedMount resMount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final ContextualizableMount mount = (ContextualizableMount)resMount.getMount();

        final HstSiteService hstSite = (HstSiteService)mount.getHstSite();
        final HstSiteService previewHstSite = (HstSiteService)mount.getPreviewHstSite();

        assertNotSame(hstSite, previewHstSite);
        assertNull(hstSite.componentsConfiguration);
        assertNull(previewHstSite.componentsConfiguration);

        final VirtualHosts virtualHosts = resMount.getMount().getVirtualHost().getVirtualHosts();
        assertTrue(virtualHosts.getChannelById("dev-localhost","testchannel").getChangedBySet() instanceof HashSet);

        virtualHosts.getChannelById("dev-localhost","testchannel").getChangedBySet().size();
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
                final ResolvedMount resMount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
                final ContextualizableMount mount = (ContextualizableMount)resMount.getMount();
                // since we do not have a preview yet, previewChannel and live channel should be same instance
                assertSame("Since there is not preview, the preview channel instance should be same as the live channel" +
                        " instance", mount.getPreviewChannel(), mount.getChannel());

                final HstSiteService hstSite = (HstSiteService)mount.getHstSite();
                // create preview configuration and preview channel
                String configPath = hstSite.getConfigurationPath();
                JcrUtils.copy(session, configPath, configPath+"-preview");
                String channelPath = mount.getChannelPath();
                JcrUtils.copy(session, channelPath, channelPath+"-preview");
                // add a change by setting 'lockedby' on preview channel node
                session.getNode(channelPath+"-preview").setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "someonelikeyou");
            }
            String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
            session.save();
            invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
            invalidator.eventPaths(pathsToBeChanged);

            {
                final ResolvedMount resMount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
                final ContextualizableMount mount = (ContextualizableMount)resMount.getMount();
                assertNotSame("Since there is *a* preview, the preview channel instance should NOT be same as the live channel" +
                        " instance", mount.getPreviewChannel(), mount.getChannel());
                final HstSiteService hstSite = (HstSiteService)mount.getHstSite();
                // componentsConfiguration is laoded lazily
                assertNull(hstSite.componentsConfiguration);
                final VirtualHosts virtualHosts = resMount.getMount().getVirtualHost().getVirtualHosts();

                assertEquals(0, virtualHosts.getChannelById("dev-localhost","testchannel").getChangedBySet().size());

                final HstSiteService previewHstSite = (HstSiteService)mount.getPreviewHstSite();
                // only when changes from preview channel are loaded the lazy component configuration gets populated
                assertNull(previewHstSite.componentsConfiguration);

                assertEquals(1, virtualHosts.getChannelById("dev-localhost","testchannel-preview").getChangedBySet().size());
                assertTrue(virtualHosts.getChannelById("dev-localhost","testchannel-preview").getChangedBySet().contains("someonelikeyou"));
                // since changedBySet is request on preview channel, we expect previewHstSite.componentsConfiguration not to be
                // null any more.
                assertNull(hstSite.componentsConfiguration);
                assertNotNull(previewHstSite.componentsConfiguration);
            }

        } finally {
            restoreHstConfigBackup(session);
        }

    }

    @Test
    public void componentsConfigurationLoadedLazilyUnlessPresentInCache() throws Exception {
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        final HstSiteService hstSite1 = (HstSiteService)mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService)mount2.getMount().getHstSite();
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

        final ResolvedMount mountAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mountAfter2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        assertNotSame(mountAfter1, mount1);
        assertNotSame(mountAfter2, mount2);

        final HstSiteService hstSiteAfter1 = (HstSiteService)mountAfter1.getMount().getHstSite();
        final HstSiteService hstSiteAfter2 = (HstSiteService)mountAfter2.getMount().getHstSite();

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

        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        final HstSiteService hstSite1 = (HstSiteService)mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService)mount2.getMount().getHstSite();

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
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        final HstSiteService hstSite1 = (HstSiteService)mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService)mount2.getMount().getHstSite();

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
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        final HstSiteService hstSite1 = (HstSiteService)mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService)mount2.getMount().getHstSite();

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
        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        final HstSiteService hstSite1 = (HstSiteService)mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService)mount2.getMount().getHstSite();
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

        final ResolvedMount mountAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mountAfter2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        assertNotSame(mountAfter1, mount1);
        assertNotSame(mountAfter2, mount2);

        final HstSiteService hstSiteAfter1 = (HstSiteService)mountAfter1.getMount().getHstSite();
        final HstSiteService hstSiteAfter2 = (HstSiteService)mountAfter2.getMount().getHstSite();

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

        final ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        final ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");

        final HstSiteService hstSite1 = (HstSiteService)mount1.getMount().getHstSite();
        final HstSiteService hstSite2 = (HstSiteService)mount2.getMount().getHstSite();

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

    protected Session createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    protected void createHstConfigBackup(Session session) throws RepositoryException {
        if (!session.nodeExists("/hst-backup")) {
            JcrUtils.copy(session, "/hst:hst", "/hst-backup");
            session.save();
        }
    }

    protected void restoreHstConfigBackup(Session session) throws RepositoryException {
        if (session.nodeExists("/hst-backup")) {
            if (session.nodeExists("/hst:hst")) {
                session.removeItem("/hst:hst");
            }
            JcrUtils.copy(session, "/hst-backup", "/hst:hst");
            session.removeItem("/hst-backup");
            session.save();
        }
    }
}
