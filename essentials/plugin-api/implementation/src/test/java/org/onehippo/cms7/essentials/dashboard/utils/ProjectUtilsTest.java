/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import org.apache.maven.model.Dependency;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.model.DependencyType;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class ProjectUtilsTest extends BaseResourceTest {


    @Test
    public void testSitePackages() throws Exception {
        final List<String> sitePackages = ProjectUtils.getSitePackages(getContext());
        assertTrue(sitePackages.size() > 8);
        assertTrue(sitePackages.contains("org"));
        assertTrue(sitePackages.contains("org.dummy"));

    }

    @Test
    public void testDependencies() throws Exception {
        final URL resource = getClass().getResource("/project");
        Dependency dependency = new Dependency();
        dependency.setGroupId("org.onehippo.cms7.essentials");
        dependency.setArtifactId("hippo-components-plugin-components");
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, resource.getPath());
        boolean installed = ProjectUtils.isInstalled(DependencyType.SITE, dependency);
        assertTrue(!installed);
        dependency.setGroupId("org.onehippo.cms7.hst.dependencies");
        dependency.setArtifactId("hst-server-dependencies");
        installed = ProjectUtils.isInstalled(DependencyType.SITE, dependency);
        assertTrue(installed);


    }


}
