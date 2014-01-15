/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.integration;

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HstIntegrationTest extends AbstractHstIntegrationTest {

    @Test
    public void test_remote_and_local_session() throws Exception {
        final Session remoteSession = createRemoteSession(new SimpleCredentials("admin", "admin".toCharArray()));
        final Session localSession = createLocalSession(new SimpleCredentials("admin", "admin".toCharArray()));
        assertTrue(localSession.nodeExists("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root"));
        assertTrue(localSession.nodeExists("/hst:hst/hst:configurations/unittestcommon"));
        assertTrue(remoteSession.nodeExists("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root"));
        assertTrue(remoteSession.nodeExists("/hst:hst/hst:configurations/unittestcommon"));
        remoteSession.logout();
        localSession.logout();
    }

    @Test
    public void test_hstManager_basic() throws Exception {
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        assertNotNull(mount);
        final HstSite hstSite = mount.getMount().getHstSite();
        assertNotNull(hstSite);
        HstComponentsConfiguration service = hstSite.getComponentsConfiguration();
        assertNotNull(service);
    }

    @Test
    public void test_hstManager_basic_change_and_reload() throws Exception {
        // backup hst config
        final Session remoteSession = createRemoteSession(new SimpleCredentials("admin", "admin".toCharArray()));
        final Session localSession = createLocalSession(new SimpleCredentials("admin", "admin".toCharArray()));
        try {
            backupHstConfig(remoteSession);
            // first load model
            final VirtualHosts virtualHosts = hstManager.getVirtualHosts();
            ResolvedMount resMount = virtualHosts.matchMount("www.unit.test", "", "/");
            assertEquals("www.unit.test", resMount.getMount().getVirtualHost().getHostName());

            assertEquals(0, hstManager.getMarkStaleCounter());
            remoteSession.getNode("/hst:hst/hst:hosts/testgroup").remove();
            remoteSession.save();
            long start = System.currentTimeMillis();

            while (virtualHosts == hstManager.getVirtualHosts()) {
                if ((System.currentTimeMillis() - start) > 15000) {
                    fail("There should had arrived a jcr event within 15 sec marking the HstManager to be stale and triggering a " +
                            "model reload");
                }
                Thread.sleep(100);
            }
            assertFalse(localSession.nodeExists("/hst:hst/hst:hosts/testgroup"));
            // since we removed the hostname "www.unit.test" which is part of the 'testgroup' we now expect
            // the default (localhost) hostname to be matched
            ResolvedMount resMountAfter = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
            assertEquals(1, hstManager.getMarkStaleCounter());
            assertEquals("localhost", resMountAfter.getMount().getVirtualHost().getHostName());

        } finally {
            restoreHstConfig(remoteSession);
            remoteSession.logout();
            // make sure the local session gets the changes: Otherwise, later unit tests can get the changes from this remote
            // change
            localSession.refresh(false);
            Thread.sleep(1000);
            localSession.logout();
        }
    }

    @Test
    public void test_hstManager_basic_create_preview_and_reload() throws Exception {
        // backup hst config
        final Session remoteSession = createRemoteSession(new SimpleCredentials("admin", "admin".toCharArray()));
        final Session localSession = createLocalSession(new SimpleCredentials("admin", "admin".toCharArray()));
        try {
            backupHstConfig(remoteSession);

            // since from unit test setup, all components are shared through '/hst:hst/hst:configurations/unittestcommon'
            // we first copy all of them to make sure we have a bit bigger jcr trees for live and preview : namely, as of
            // the writing of this unit test, inherited 'common' configuration does not have the 'preview' | 'live'

            copyPagesAndComponentsFromCommonConfig(remoteSession, localSession);

            int expectedInvalidations = 1;
            assertEquals(1, hstManager.getMarkStaleCounter());

            for (int i = 0; i < 10; i++) {
                // first load model
                final VirtualHosts hostsBefore = hstManager.getVirtualHosts();
                {
                    ContextualizableMount mount = (ContextualizableMount)hostsBefore.matchMount("localhost", "", "/").getMount();
                    assertEquals("/hst:hst/hst:channels/testchannel", mount.getChannelPath());

                    validateMount(mount, false);

                    validatePreviewConfiguration(mount, false);

                    // copy live config to preview and also for channel node
                    JcrUtils.copy(remoteSession, mount.getHstSite().getConfigurationPath(), mount.getHstSite().getConfigurationPath() + "-preview");
                    JcrUtils.copy(remoteSession, mount.getChannelPath(), mount.getChannelPath() + "-preview");
                }

                remoteSession.save();

                localSession.refresh(false);
                // After refresh events can take a short while to arrive: Hence below fetch hst hosts until instance is changed
                tryUntilModelReloaded(hostsBefore);

                expectedInvalidations++;
                assertEquals(expectedInvalidations, hstManager.getMarkStaleCounter());
                // since we removed the hostname "www.unit.test" which is part of the 'testgroup' we now expect
                // the default (localhost) hostname to be matched
                final VirtualHosts hostsAfter = hstManager.getVirtualHosts();

                assertFalse(hostsAfter == hostsBefore);

                {
                    ContextualizableMount mount = (ContextualizableMount) hostsAfter.matchMount("localhost", "", "/").getMount();
                    validateMount(mount, true);
                    validatePreviewConfiguration(mount, true);
                    // clean up preview again
                    remoteSession.getNode(mount.getPreviewHstSite().getConfigurationPath()).remove();
                    remoteSession.getNode(mount.getChannelPath() + "-preview").remove();
                    remoteSession.save();

                    localSession.refresh(false);
                    // After refresh events can take a short while to arrive: Hence below fetch hst hosts until instance is changed
                    tryUntilModelReloaded(hostsAfter);
                    expectedInvalidations++;
                    assertEquals(expectedInvalidations, hstManager.getMarkStaleCounter());
                }
            }

        } finally {
            restoreHstConfig(remoteSession);
            remoteSession.logout();
            localSession.refresh(false);
            Thread.sleep(1000);
            localSession.logout();
        }
    }

    private void copyPagesAndComponentsFromCommonConfig(final Session remoteSession, final Session localSession) throws RepositoryException, InterruptedException {
        JcrUtils.copy(remoteSession, "/hst:hst/hst:configurations/unittestcommon/hst:components", "/hst:hst/hst:configurations/unittestproject/hst:components");
        JcrUtils.copy(remoteSession, "/hst:hst/hst:configurations/unittestcommon/hst:pages", "/hst:hst/hst:configurations/unittestproject/hst:pages");
        remoteSession.save();

        localSession.refresh(false);

        long start = System.currentTimeMillis();
        while (hstManager.getMarkStaleCounter() == 0) {
            if ((System.currentTimeMillis() - start) > 1000) {
                fail("There should had arrived a jcr event within 1 sec marking the HstManager to be stale and triggering a " +
                        "model reload");
            }
            Thread.sleep(10);
        }
    }

    /**
     * validation on preview mount : it is a just created copy of the live, hence we 'know' which  con
     */
    private void validatePreviewConfiguration(final ContextualizableMount mount, boolean hasPreview) {
        final Map<String,HstComponentConfiguration> previewComponentConfigurations = mount.getPreviewHstSite().getComponentsConfiguration().getComponentConfigurations();
        final Map<String, HstComponentConfiguration> liveComponentConfigurations = mount.getHstSite().getComponentsConfiguration().getComponentConfigurations();
        for (HstComponentConfiguration previewComp : previewComponentConfigurations.values()) {
            // regardless preview or live, they both should have same containers with same ids
            assertTrue(liveComponentConfigurations.containsKey(previewComp.getId()));

            HstComponentConfiguration liveComp = liveComponentConfigurations.get(previewComp.getId());
            if (hasPreview) {
                assertTrue(previewComp.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject-preview/"));
                assertTrue(liveComp.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/"));
            } else {
                assertTrue(previewComp.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/"));;
                assertTrue(liveComp.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/"));
            }
        }

        final List<HstSiteMapItem> previewSiteMapItems = mount.getPreviewHstSite().getSiteMap().getSiteMapItems();
        final HstSiteMap liveSiteMap = mount.getHstSite().getSiteMap();
        for (HstSiteMapItem previewSiteMapItem : previewSiteMapItems) {
            // regardless preview or live, they both should have same sitemap item ids
            final HstSiteMapItem liveSiteMapItem = liveSiteMap.getSiteMapItem(previewSiteMapItem.getId());
            assertNotNull(liveSiteMapItem);

            if (hasPreview) {
                assertTrue(previewSiteMapItem.getQualifiedId().startsWith("/hst:hst/hst:configurations/unittestproject-preview/"));
                assertTrue(liveSiteMapItem.getQualifiedId().startsWith("/hst:hst/hst:configurations/unittestproject/"));
            } else {
                assertTrue(previewSiteMapItem.getQualifiedId().startsWith("/hst:hst/hst:configurations/unittestproject/"));;
                assertTrue(liveSiteMapItem.getQualifiedId().startsWith("/hst:hst/hst:configurations/unittestproject/"));
            }
        }

    }

    private void tryUntilModelReloaded(final VirtualHosts prevModel) throws ContainerException, InterruptedException {
        long start = System.currentTimeMillis();
        while (prevModel == hstManager.getVirtualHosts()) {
            if ((System.currentTimeMillis() - start) > 1000) {
                fail("There should had arrived a jcr event within 1 sec (since localSession refresh invoked) " +
                        "marking the HstManager to be stale and triggering a model reload");
            }
            Thread.sleep(10);
        }
    }

    private void validateMount(final ContextualizableMount mount, final boolean hasPreview) {
        if (hasPreview) {
            assertTrue(mount.getPreviewHstSite().hasPreviewConfiguration());
            assertFalse(mount.getHstSite().getConfigurationPath().equals(mount.getPreviewHstSite().getConfigurationPath()));
            assertFalse(mount.getChannel() == mount.getPreviewChannel());

        } else {
            assertFalse(mount.getPreviewHstSite().hasPreviewConfiguration());
            assertTrue(mount.getHstSite().getConfigurationPath().equals(mount.getPreviewHstSite().getConfigurationPath()));
            assertTrue(mount.getChannel() == mount.getPreviewChannel());
        }

    }


    private void backupHstConfig(final Session remoteSession) throws RepositoryException {
        remoteSession.getWorkspace().copy("/hst:hst", "/bak");
    }

    private void restoreHstConfig(final Session remoteSession) throws RepositoryException {
        remoteSession.getNode("/hst:hst").remove();
        remoteSession.move("/bak", "/hst:hst");
        remoteSession.save();

    }
}
