/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cm.engine.impl.DigestBundleResolver;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DigestBundleResolver.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*"})
public class ConfigurationConfigServiceBundleTest {

    private ConfigurationBaselineService baselineService;

    private Session session;

    private ConfigurationConfigService configurationConfigService = new ConfigurationConfigService();

    private final String bundleName = "site";
    private final JcrPath bundlePath = JcrPaths.getPath(WebFilesService.JCR_ROOT_PATH, bundleName);

    private final String fsBundleDigest = "A";
    private final String baselineBundleDigest = "B";
    private final String runtimeDigest = "C";

    private final String reloadMode = WebFilesService.RELOAD_NEVER;



    @Before
    public void setUp() throws Exception {
        session = createNiceMock(Session.class);
        baselineService = createNiceMock(ConfigurationBaselineService.class);
        PowerMock.mockStatic(DigestBundleResolver.class);
    }
    @Test
    public void baseline_bundle_digest_not_exists() throws Exception {

        expect(baselineService.getBaselineBundleDigest(bundleName, session)).andReturn(Optional.empty()).atLeastOnce();
        replay(baselineService);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            boolean reloadBundle = configurationConfigService.shouldReloadBundle(fsBundleDigest, bundleName, reloadMode, baselineService, session);
            assertEquals(true, reloadBundle);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "baseline bundle does not exist, first bootstrap, (re)load")));
        }
        verify(baselineService);
    }

    @Test
    public void same_bundles() throws Exception {

        final String baselineBundleDigest = fsBundleDigest;

        expect(baselineService.getBaselineBundleDigest(bundleName, session)).andReturn(Optional.of(baselineBundleDigest)).atLeastOnce();
        replay(baselineService);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            boolean reloadBundle = configurationConfigService.shouldReloadBundle(fsBundleDigest, bundleName, WebFilesService.RELOAD_NEVER, baselineService, session);
            assertEquals(false, reloadBundle);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "classpath & baseline bundle's digests are same, skip reload")));
        }
    }

    @Test
    public void different_bundles_no_runtime() throws Exception {

        expect(baselineService.getBaselineBundleDigest(bundleName, session)).andReturn(Optional.of(baselineBundleDigest)).atLeastOnce();
        replay(baselineService);

        expect(session.nodeExists("/webfiles/" + bundleName)).andReturn(false);
        replay(session);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            boolean reloadBundle = configurationConfigService.shouldReloadBundle(fsBundleDigest, bundleName, reloadMode, baselineService, session);
            assertEquals(false, reloadBundle);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "classpath & baseline bundles digests are different")));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    String.format("Bundle name '%s' does not exist in repository, skip processing bundle", bundlePath))));
        }
    }
    @Test
    public void different_bundles_never_mode() throws Exception {

        expect(baselineService.getBaselineBundleDigest(bundleName, session)).andReturn(Optional.of(baselineBundleDigest)).atLeastOnce();
        replay(baselineService);

        expect(session.nodeExists(bundlePath.toString())).andReturn(true);
        replay(session);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            boolean reloadBundle = configurationConfigService.shouldReloadBundle(fsBundleDigest, bundleName, WebFilesService.RELOAD_NEVER, baselineService, session);
            assertEquals(false, reloadBundle);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "classpath & baseline bundles digests are different")));
        }
    }

    @Test
    public void different_bundles_runtime_unchanged() throws Exception {

        final String reloadMode = WebFilesService.RELOAD_IF_RUNTIME_UNCHANGED;

        Node mockNode = createNiceMock(Node.class);
        expect(session.getNode(bundlePath.toString())).andReturn(mockNode).times(2);
        expect(session.nodeExists(bundlePath.toString())).andReturn(true).times(2);
        replay(session);

        expect(baselineService.getBaselineBundleDigest(bundleName, session)).andReturn(Optional.of(baselineBundleDigest));
        PowerMock.expectLastCall().times(2);
        replay(baselineService);
        expect(DigestBundleResolver.calculateRuntimeBundleDigest(mockNode)).andReturn(runtimeDigest).once();
        expect(DigestBundleResolver.calculateRuntimeBundleDigest(mockNode)).andReturn("B").once();
        replay(DigestBundleResolver.class);

        //runtimeDigest != baselineDigest
        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            boolean reloadBundle = configurationConfigService.shouldReloadBundle(fsBundleDigest, bundleName, reloadMode, baselineService, session);
            assertEquals(false, reloadBundle);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "classpath & baseline bundles digests are different")));
        }

        //runtimeDigest == baselineDigest
        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            boolean reloadBundle = configurationConfigService.shouldReloadBundle(fsBundleDigest, bundleName, reloadMode, baselineService, session);
            assertEquals(true, reloadBundle);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "classpath & baseline bundles digests are different")));
        }
    }

    @Test
    public void different_bundles_runtime_discard_changes() throws Exception {

        final String reloadMode = WebFilesService.RELOAD_DISCARD_RUNTIME_CHANGES;
        Node mockNode = createNiceMock(Node.class);
        expect(session.nodeExists(bundlePath.toString())).andReturn(true);
        expect(session.getNode(bundlePath.toString())).andReturn(mockNode);
        replay(session);

        expect(baselineService.getBaselineBundleDigest(bundleName, session)).andReturn(Optional.of(baselineBundleDigest));
        replay(baselineService);
        expect(DigestBundleResolver.calculateRuntimeBundleDigest(mockNode)).andReturn(runtimeDigest);
        replay(DigestBundleResolver.class);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            boolean reloadBundle = configurationConfigService.shouldReloadBundle(fsBundleDigest, bundleName, reloadMode, baselineService, session);
            assertEquals(true, reloadBundle);
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "classpath & baseline bundles digests are different")));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    String.format("Web bundle '%s' will be force reloaded and runtime changes will be lost", bundleName))));
        }

    }

}