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
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryRestful;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
