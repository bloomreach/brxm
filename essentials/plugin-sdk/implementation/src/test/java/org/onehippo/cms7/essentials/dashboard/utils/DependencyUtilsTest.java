/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.net.URL;
import org.apache.maven.model.Model;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.DependencyRestful;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryRestful;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DependencyUtilsTest extends BaseResourceTest {

    private static final String NEW_REPO = "http://maven.onehippo.com/maven3/";

    private Logger log = LoggerFactory.getLogger(DependencyUtilsTest.class);

    @Test
    public void testRepositoryAdd() throws Exception {
        final Repository repository = new RepositoryRestful();
        repository.setTargetPom(TargetPom.PROJECT.getName());
        repository.setUrl("https://maven.onehippo.com/maven2/");
        repository.setId("hippo");
        assertEquals(TargetPom.PROJECT, repository.getDependencyTargetPom());
        boolean hasRepo = DependencyUtils.hasRepository(getContext(), repository);
        assertTrue("Expected hippo maven repository", hasRepo);
        // add new one:
        repository.setUrl(NEW_REPO);
        repository.setId("some-id");
        hasRepo = DependencyUtils.hasRepository(getContext(), repository);
        assertFalse("Expected no maven repository", hasRepo);
        DependencyUtils.addRepository(getContext(), repository);
        hasRepo = DependencyUtils.hasRepository(getContext(), repository);
        assertTrue("Expected new maven repository: " + NEW_REPO, hasRepo);
    }

    @Test
    public void testHasDependency() throws Exception {

        final EssentialsDependency dependency = new DependencyRestful();
        dependency.setTargetPom("cms");
        dependency.setArtifactId("hippo-plugins-shared");
        dependency.setVersion("1.01.00-SNAPSHOT");
        dependency.setGroupId("org.onehippo.cms7.essentials");
        assertEquals(TargetPom.CMS, dependency.getDependencyTargetPom());
        final boolean hasDep = DependencyUtils.hasDependency(getContext(), dependency);
        assertTrue("Expected hippo-plugins-shared version", hasDep);

    }

    @Test
    public void testAddRemoveDependency() throws Exception {
        final EssentialsDependency dependency = new DependencyRestful();
        dependency.setTargetPom("cms");
        dependency.setArtifactId("hippo-plugins-non-existing");
        dependency.setVersion("1.01.00-SNAPSHOT");
        dependency.setGroupId("org.onehippo.cms7.essentials");
        assertEquals(TargetPom.CMS, dependency.getDependencyTargetPom());
        final PluginContext context = getContext();
        boolean hasDep = DependencyUtils.hasDependency(context, dependency);
        assertFalse("Expected no dependency", hasDep);
        // add
        DependencyUtils.addDependency(context, dependency);
        hasDep = DependencyUtils.hasDependency(context, dependency);
        assertTrue("Expected hippo-plugins-non-existing", hasDep);
        // remove
        DependencyUtils.removeDependency(context, dependency);
        hasDep = DependencyUtils.hasDependency(context, dependency);
        assertFalse("Expected hippo-plugins-non-existing to be removed", hasDep);
    }

    @Test
    public void testIsEnterpriseProject() throws Exception {
        final URL resource = getClass().getResource("/project");
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, resource.getPath());

        final PluginContext context = getContext();
        boolean enterpriseProject = DependencyUtils.isEnterpriseProject(context);
        assertFalse(enterpriseProject);
    }

    @Test
    public void testUpgradeToEnterpriseProject() throws Exception {
        final URL resource = getClass().getResource("/project");
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, resource.getPath());

        final PluginContext context = getContext();
        boolean enterpriseProject = DependencyUtils.isEnterpriseProject(context);
        if (enterpriseProject) {
            log.info("Probably read Enterprise project from already upgraded target");
            return;
        }

        enterpriseProject = DependencyUtils.upgradeToEnterpriseProject(context);
        assertTrue(enterpriseProject);

        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);

        for (final org.apache.maven.model.Repository repository : pomModel.getRepositories()) {
            if (repository.getId().equals(ProjectUtils.ENT_REPO_ID)) {
                assertTrue(repository.getName().equals(ProjectUtils.ENT_REPO_NAME));
                assertTrue(repository.getReleases().getUpdatePolicy().equals("never"));
                assertTrue(repository.getReleases().getChecksumPolicy().equals("fail"));
                log.debug("Found Enterprise Repository as expected");
                return;
            }
        }

        assertTrue("No Enterprise Repositories found in pom: " + pomModel.getRepositories(), false);
    }

    @Test
    public void addCargoDeployableTest() {
        final PluginContext context = getContext();
        final EssentialsDependency dependency = new DependencyRestful();
        final String webContext = "/addCargoDeployableTest";

        assertFalse(DependencyUtils.hasCargoRunnerWebappContext(context, webContext));

        dependency.setTargetPom("project");
        dependency.setArtifactId("hippo-plugins-shared");
        dependency.setVersion("dummy");
        dependency.setGroupId("org.onehippo.cms");
        DependencyUtils.addDeployableToCargoRunner(context, dependency, webContext);
        assertTrue(DependencyUtils.hasCargoRunnerWebappContext(context, webContext));

        DependencyUtils.removeDeployableFromCargoRunner(context, webContext);
        assertFalse(DependencyUtils.hasCargoRunnerWebappContext(context, webContext));
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
        boolean result = DependencyUtils.addDeployableToCargoRunner(context, dependency, webContext);
        assertTrue(result);
        result = DependencyUtils.addDeployableToCargoRunner(context, dependency, webContext);
        assertFalse(result);
        result = DependencyUtils.removeDeployableFromCargoRunner(context, webContext);
        assertTrue(result);
    }

    @Test
    public void addSharedClasspathTest() {
        final PluginContext context = getContext();
        boolean result = DependencyUtils.addDependencyToCargoSharedClasspath(context, "org.onehippo.cms", "hippo-plugins-shared");
        assertTrue("Expected to add dependency", result);
        result = DependencyUtils.addDependencyToCargoSharedClasspath(context, "org.onehippo.cms", "hippo-plugins-shared");
        assertFalse("Expected to fail adding a duplicate dependency", result);
        result = DependencyUtils.removeDependencyFromCargoSharedClasspath(context, "org.onehippo.cms", "hippo-plugins-shared");
        assertTrue("Failed to remove dependency", result);
    }
}
