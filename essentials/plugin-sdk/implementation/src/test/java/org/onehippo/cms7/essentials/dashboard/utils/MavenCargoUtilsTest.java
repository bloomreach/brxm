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

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.DependencyRestful;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenCargoUtilsTest extends BaseResourceTest {
    @Test
    public void addCargoDeployableTest() {
        final PluginContext context = getContext();
        final EssentialsDependency dependency = new DependencyRestful();
        final String webContext = "/addCargoDeployableTest";

        assertFalse(MavenCargoUtils.hasCargoRunnerWebappContext(context, webContext));

        dependency.setTargetPom("project");
        dependency.setArtifactId("hippo-plugins-shared");
        dependency.setVersion("dummy");
        dependency.setGroupId("org.onehippo.cms");
        MavenCargoUtils.addDeployableToCargoRunner(context, dependency, webContext);
        assertTrue(MavenCargoUtils.hasCargoRunnerWebappContext(context, webContext));

        MavenCargoUtils.removeDeployableFromCargoRunner(context, webContext);
        assertFalse(MavenCargoUtils.hasCargoRunnerWebappContext(context, webContext));
    }

    @Test
    public void addCargoDeployableDuplicateTest() {
        final PluginContext context = getContext();
        final String webContext = "/test";

        final EssentialsDependency dependency = new DependencyRestful();
        dependency.setTargetPom("project");
        dependency.setArtifactId("hippo-plugins-shared");
        dependency.setVersion("dummy");
        dependency.setGroupId("org.onehippo.cms");
        boolean result = MavenCargoUtils.addDeployableToCargoRunner(context, dependency, webContext);
        assertTrue(result);
        result = MavenCargoUtils.addDeployableToCargoRunner(context, dependency, webContext);
        assertFalse(result);
        result = MavenCargoUtils.removeDeployableFromCargoRunner(context, webContext);
        assertTrue(result);
    }

    @Test
    public void addSharedClasspathTest() {
        final PluginContext context = getContext();
        boolean result = MavenCargoUtils.addDependencyToCargoSharedClasspath(context, "org.onehippo.cms", "hippo-plugins-shared");
        assertTrue("Expected to add dependency", result);
        result = MavenCargoUtils.addDependencyToCargoSharedClasspath(context, "org.onehippo.cms", "hippo-plugins-shared");
        assertFalse("Expected to fail adding a duplicate dependency", result);
        result = MavenCargoUtils.removeDependencyFromCargoSharedClasspath(context, "org.onehippo.cms", "hippo-plugins-shared");
        assertTrue("Failed to remove dependency", result);
    }

    @Test
    public void addPropertyTest() {
        final PluginContext context = getContext();
        MavenCargoUtils.addPropertyToProfile(context, "first.test.property", "random", true);
        assertTrue(MavenCargoUtils.hasProfileProperty(context, "first.test.property"));
    }

    @Test
    public void mergeModelTest() throws IOException, XmlPullParserException {
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());
        final PluginContext context = getContext();

        InputStream pomStream = MavenCargoUtilsTest.class.getResourceAsStream("/test-pom-overlay.xml");
        Model model = MavenModelUtils.readPom(pomStream);
        MavenCargoUtils.mergeCargoProfile(context, model);

        String version = model.getProperties().get("elasticsearch.version").toString();
        assertEquals("5.6.4", version);
    }
}
