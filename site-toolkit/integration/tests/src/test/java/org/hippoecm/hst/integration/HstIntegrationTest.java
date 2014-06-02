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
import java.util.Random;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HstIntegrationTest extends AbstractHstIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(HstIntegrationTest.class);

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
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("www.unit.test", "/site", "/");
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
            ResolvedMount resMount = virtualHosts.matchMount("www.unit.test", "/site", "/");
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
            ResolvedMount resMountAfter = hstManager.getVirtualHosts().matchMount("www.unit.test", "/site", "/");
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
    public void test_hstManager_create_preview_and_reload() throws Exception {
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
                    ContextualizableMount mount = (ContextualizableMount)hostsBefore.matchMount("localhost", "/site", "/").getMount();
                    assertEquals("/hst:hst/hst:channels/testchannel", mount.getChannelPath());

                    validateMount(mount, false);

                    validatePreviewConfiguration(mount, false);

                    // copy live config to preview and also for channel node
                    JcrUtils.copy(remoteSession, mount.getHstSite().getConfigurationPath(), mount.getHstSite().getConfigurationPath() + "-preview");
                    JcrUtils.copy(remoteSession, mount.getChannelPath(), mount.getChannelPath() + "-preview");
                    remoteSession.save();
                }


                localSession.refresh(false);
                // After refresh events can take a short while to arrive: Hence below fetch hst hosts until instance is changed
                tryUntilModelReloaded(hostsBefore);

                expectedInvalidations++;
                assertEquals(expectedInvalidations, hstManager.getMarkStaleCounter());

                final VirtualHosts hostsAfter = hstManager.getVirtualHosts();

                assertFalse(hostsAfter == hostsBefore);

                {
                    ContextualizableMount mount = (ContextualizableMount) hostsAfter.matchMount("localhost", "/site", "/").getMount();
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
        JcrUtils.copy(remoteSession, "/hst:hst/hst:configurations/unittestcommon/hst:abstractpages", "/hst:hst/hst:configurations/unittestproject/hst:abstractpages");
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
     * validation on preview mount : it is a just created copy of the live.
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
                assertTrue(previewComp.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/"));
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
                assertTrue(previewSiteMapItem.getQualifiedId().startsWith("/hst:hst/hst:configurations/unittestproject/"));
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


    @Test
    public void test_hstManager_create_preview_and_reload_while_writing_remote() throws Exception {
        // backup hst config
        final Session remoteSession = createRemoteSession(new SimpleCredentials("admin", "admin".toCharArray()));
        final Session localSession = createLocalSession(new SimpleCredentials("admin", "admin".toCharArray()));
        Thread configWriter = null;
        try {
            backupHstConfig(remoteSession);

            // since from unit test setup, all components are shared through '/hst:hst/hst:configurations/unittestcommon'
            // we first copy all of them to make sure we have a bit bigger jcr trees for live and preview : namely, as of
            // the writing of this unit test, inherited 'common' configuration does not have the 'preview' | 'live'

            copyPagesAndComponentsFromCommonConfig(remoteSession, localSession);

            assertEquals(1, hstManager.getMarkStaleCounter());

            String rootPreviewConfigurationPath;
            // first load model
            VirtualHosts currentHosts = hstManager.getVirtualHosts();
            {
                ContextualizableMount mount = (ContextualizableMount)currentHosts.matchMount("localhost", "/site", "/").getMount();
                assertEquals("/hst:hst/hst:channels/testchannel", mount.getChannelPath());

                validateMount(mount, false);

                validatePreviewConfiguration(mount, false);

                rootPreviewConfigurationPath = mount.getHstSite().getConfigurationPath() + "-preview";
                // copy live config to preview and also for channel node
                JcrUtils.copy(remoteSession, mount.getHstSite().getConfigurationPath(), rootPreviewConfigurationPath);
                JcrUtils.copy(remoteSession, mount.getChannelPath(), mount.getChannelPath() + "-preview");
                remoteSession.save();
            }

            // pull in all changes above locally
            localSession.refresh(false);
            while (hstManager.getMarkStaleCounter() != 2) {
                Thread.sleep(100);
            }

            // now in another thread, we'll start moving configuration nodes such that all expectations regaring
            // PREVIEW vs LIVE configuration can be kept the same. In the separate thread we'll pull
            // in changes through localSession.refresh(false);
            int totalNumberOfWritesToDo = 10000;
            int sleepTimeConfigurationWriter = 10;
            int maxSleepTimeReading = sleepTimeConfigurationWriter * 5;

            configWriter = new Thread(new ConfigurationWriter(
                    remoteSession,
                    localSession,
                    rootPreviewConfigurationPath,
                    totalNumberOfWritesToDo,
                    sleepTimeConfigurationWriter
                    ));

            configWriter.start();

            Thread.sleep(100);

            Random rand = new Random();
            int modelReloadCounter = 0;
            while(configWriter.isAlive()) {
                final VirtualHosts hostsAfter = hstManager.getVirtualHosts();
                if (hstManager.isBuilderStateFailed()) {
                    fail("BuildState of HstManager has state FAILED. Check the logs as most likely a ModelLoadingException " +
                            "must have cause this.");
                }

                if (hostsAfter != currentHosts) {
                    modelReloadCounter++;
                }

                currentHosts = hostsAfter;
                // sleep is sometimes shorter than the 'writer thread sleep' and sometimes longer: This
                // ensures sometimes single change set, and sometimes multiple change sets during one reload
                Thread.sleep(rand.nextInt(maxSleepTimeReading));
            }

            localSession.refresh(false);

            // one more final reload after 1 second to make sure all asynchronous events must have been processed
            Thread.sleep(1000);
            hstManager.getVirtualHosts();

            // now we got here, it means the model has been reloaded correctly all the time.
            // We can have some expectations about some numbers now. Since
            // maxSleepTimeReading = 5 * sleepTimeConfigurationWriter , we sometimes will have
            // multiple changes processed in a single RELOAD. Hence, the
            // modelReloadCounter is 99.99999999% sure expected to be lower than IntegrationHstManagerImpl#getMarkStaleCounter

            assertTrue("modelReloadCounter should had been lower than hstManager.getMarkStaleCounter(). ", modelReloadCounter < hstManager.getMarkStaleCounter());
            // SINCE THE remote session made totalNumberOfWritesToDo changes, the IntegrationHstManagerImpl#getMarkStaleCounter
            // should be totalNumberOfWritesToDo + 2 (the first one was of the copyPagesAndComponentsFromCommonConfig and
            // second for creating the preview)
            assertEquals("Since there should have been in total 'totalNumberOfWritesToDo + 1' saves with the remove session," +
                    " we also expect 'totalNumberOfWritesToDo + 1' for the stale counter.",
                    totalNumberOfWritesToDo + 2, hstManager.getMarkStaleCounter());


        } finally {
            if (configWriter != null && configWriter.isAlive()) {
                // stop the thread as otherwise it will continue with remote and local session
                // which get logged out below
                configWriter.interrupt();
                configWriter.join();
            }
            restoreHstConfig(remoteSession);
            remoteSession.logout();
            localSession.refresh(false);
            Thread.sleep(1000);
            localSession.logout();
        }
    }

    class ConfigurationWriter implements Runnable {

        final Session remoteSession;
        final Session localSession;
        final String rootConfigurationPath;
        final int numberOfWrites;
        final int sleepTimeConfigurationWriter;
        int totalWriteAttempts;
        Random rand = new Random();

        ConfigurationWriter(final Session remoteSession,
                            final Session localSession,
                            final String rootConfigurationPath,
                            final int numberOfWrites,
                            final int sleepTimeConfigurationWriter) {
            this.remoteSession  = remoteSession;
            this.localSession = localSession;
            this.rootConfigurationPath = rootConfigurationPath;
            this.numberOfWrites = numberOfWrites;
            this.sleepTimeConfigurationWriter = sleepTimeConfigurationWriter;
        }

        @Override
        public void run() {
            String path1 = rootConfigurationPath + "/hst:pages/homepage";
            String path2 = rootConfigurationPath + "/hst:pages/homepage-renamed";
            while (totalWriteAttempts < numberOfWrites) {
                try {
                    Thread.sleep(sleepTimeConfigurationWriter);
                    totalWriteAttempts++;
                    if (remoteSession.nodeExists(path1)) {
                        remoteSession.move(path1,
                                path2);
                    } else {
                        remoteSession.move(path2,
                                path1);
                    }
                    remoteSession.save();

                    // to include extra randomness, not always refresh the local session
                    if (rand.nextBoolean()) {
                      localSession.refresh(false);
                    }

                } catch (InterruptedException e) {
                    log.info("Thread interrupted. Finish thread");
                    break;
                }  catch (RepositoryException e) {
                    log.error("RepositoryException", e);
                }
            }
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
