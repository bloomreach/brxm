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

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProjectUtilsTest extends BaseResourceTest {

    @Test
    public void testSitePackages() throws Exception {
        final PluginContext context = getContext();

        final List<String> sitePackages = ProjectUtils.getSitePackages(context);
        assertTrue(sitePackages.size() > 8);
        assertTrue(sitePackages.contains("org"));
        assertTrue(sitePackages.contains("org.dummy"));

        // validate defaults
        assertEquals("repository-data", ProjectUtils.getRepositoryDataFolder(context).getName());
        assertEquals("application", ProjectUtils.getRepositoryDataApplicationFolder(context).getName());
        assertEquals("development", ProjectUtils.getRepositoryDataDevelopmentFolder(context).getName());
        assertEquals("webfiles", ProjectUtils.getRepositoryDataWebfilesFolder(context).getName());
    }

    @Test
    public void testGetLog4jFiles() throws Exception {
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());
        final List<File> log4jFiles = ProjectUtils.getLog4j2Files();
        assertEquals(2, log4jFiles.size());
    }

    @Test
    public void testGetContextXml() throws Exception {
        assertTrue(ProjectUtils.getContextXml().exists());
    }

    @Test
    public void testGetAssemblyFile() throws Exception {
        assertTrue(ProjectUtils.getAssemblyFile("common-lib-component.xml").exists());
    }

    @Test
    public void testGetAssemblyFiles() throws Exception {
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());
        final List<File> assemblyFiles = ProjectUtils.getAssemblyFiles();
        assertEquals(7, assemblyFiles.size());
        File webappsComponent = ProjectUtils.getAssemblyFile("webapps-component.xml");
        assertTrue(webappsComponent.exists());
        File sharedLibComponent = ProjectUtils.getAssemblyFile("shared-lib-component.xml");
        assertTrue(sharedLibComponent.exists());
    }
}
