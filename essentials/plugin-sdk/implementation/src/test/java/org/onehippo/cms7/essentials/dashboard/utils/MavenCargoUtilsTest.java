/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.IOException;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MavenCargoUtilsTest extends ResourceModifyingTest {
    @Test
    public void addCargoDeployableTest() throws Exception {
        final PluginContext context = getContext();
        final String webappContext = "/addCargoDeployableTest";

        createModifiableFile("/project/pom.xml", "pom.xml");

        assertFalse(MavenCargoUtils.hasCargoRunnerWebappContext(context, webappContext));

        final Dependency dependency = new Dependency();
        dependency.setArtifactId("hippo-plugins-shared");
        dependency.setGroupId("org.onehippo.cms");

        assertTrue(MavenCargoUtils.addDeployableToCargoRunner(context, dependency, webappContext));

        assertTrue(MavenCargoUtils.hasCargoRunnerWebappContext(context, webappContext));
    }

    @Test
    public void addCargoDeployableDuplicateTest() throws Exception {
        final PluginContext context = getContext();
        final String webappContext = "/test";

        createModifiableFile("/project/pom.xml", "pom.xml");

        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.onehippo.cms");
        dependency.setArtifactId("hippo-plugins-shared");

        assertTrue(MavenCargoUtils.addDeployableToCargoRunner(context, dependency, webappContext));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(MavenCargoUtils.class).build()) {
            assertFalse(MavenCargoUtils.addDeployableToCargoRunner(context, dependency, webappContext));
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("Cargo deployable with web context /test already exists")));
        }
    }

    @Test
    public void noCargoPlugin() throws Exception {
        final PluginContext context = getContext();

        createModifiableFile("/utils/cargo/no-cargo-plugin.xml", "pom.xml");

        assertFalse(MavenCargoUtils.addDeployableToCargoRunner(context, null, null));
    }

    @Test
    public void noCargoProfile() throws Exception {
        final PluginContext context = getContext();

        createModifiableFile("/utils/cargo/no-cargo-profile.xml", "pom.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(MavenCargoUtils.class).build()) {
            assertFalse(MavenCargoUtils.addDeployableToCargoRunner(context, null, null));
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("cargo.run profile not found")));
        }
    }

    @Test
    public void addSharedClasspathTest() throws Exception {
        final String groupId = "org.onehippo.cms";
        final String artifactId ="hippo-plugins-shared";
        final PluginContext context = getContext();

        createModifiableFile("/project/pom.xml", "pom.xml");

        assertTrue(MavenCargoUtils.addDependencyToCargoSharedClasspath(context, groupId, artifactId));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(MavenCargoUtils.class).build()) {
            assertFalse(MavenCargoUtils.addDependencyToCargoSharedClasspath(context, groupId, artifactId));
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("Dependency org.onehippo.cms:hippo-plugins-shared already on the shared classpath")));
        }
    }

    @Test
    public void addPropertyTest() throws Exception {
        final PluginContext context = getContext();

        createModifiableFile("/project/pom.xml", "pom.xml");

        assertFalse(MavenCargoUtils.hasProfileProperty(context, "first.test.property"));
        assertTrue(MavenCargoUtils.addPropertyToProfile(context, "first.test.property", "random"));
        assertTrue(MavenCargoUtils.hasProfileProperty(context, "first.test.property"));
    }

    @Test
    public void mergeModelTest() throws IOException, XmlPullParserException {
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());
        final PluginContext context = getContext();

        final File pom = createModifiableFile("/project/pom.xml", "pom.xml");

        Model incomingModel = MavenModelUtils.readPom(getClass().getResourceAsStream("/test-pom-overlay.xml"));
        MavenCargoUtils.mergeCargoProfile(context, incomingModel);

        Model model = MavenModelUtils.readPom(pom);
        assertNotNull(model);
        Profile cargoProfile = null;
        for (Profile p : model.getProfiles()) {
            if ("cargo.run".equals(p.getId())) {
                cargoProfile = p;
                break;
            }
        }
        assertNotNull(cargoProfile);
        assertTrue(cargoProfile.getProperties().containsKey("es.tcpPort"));
    }
}
