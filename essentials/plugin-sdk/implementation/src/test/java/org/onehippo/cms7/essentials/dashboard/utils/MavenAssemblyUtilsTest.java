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

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class MavenAssemblyUtilsTest {

    @Test
    public void testAddIncludeToDependencySet() throws Exception {
        final String include = "my:dependency:jar";
        final String includeXpath = "//*[text()='" + include + "']";

        InputStream stream = getClass().getClassLoader().getResourceAsStream("project/src/main/assembly/shared-lib-component.xml");
        Document doc = new SAXReader().read(stream);

        Element dependencySet = (Element)doc.selectSingleNode("//component/*/*");
        assertEquals("include should not be in initial dependencySet", 0, dependencySet.selectNodes(includeXpath).size());
        dependencySet = MavenAssemblyUtils.addIncludeToDependencySet(dependencySet,include);
        assertEquals("include should have been added in dependencySet", 1, dependencySet.selectNodes(includeXpath).size());
        dependencySet = MavenAssemblyUtils.addIncludeToDependencySet(dependencySet,include);
        assertEquals("include must be in dependencySet only once", 1, dependencySet.selectNodes(includeXpath).size());
    }

    @Test
    public void testAddDependencySet() throws Exception {
        final String include = "my:dependency:jar";
        final String includeXpath = "//*[text()='" + include + "']";

        InputStream stream = getClass().getClassLoader().getResourceAsStream("project/src/main/assembly/webapps-component.xml");
        Document doc = new SAXReader().read(stream);

        MavenAssemblyUtils.addDependencySet(doc, "outputDirectory", "öutputFileNameMapping",
                false, "provided", include);
        assertEquals("include should have been added in dependencySet", 1, doc.selectNodes(includeXpath).size());
    }

    @Test
    public void testAddIncludeToFirstDependencySet() throws Exception {
        final String include = "my:dependency:jar";
        final String includeXpath = "//*[text()='" + include + "']";

        System.setProperty("project.basedir", getClass().getResource("/project").getPath());
        File assemblyDescriptor = ProjectUtils.getAssemblyFile("shared-lib-component.xml");

        File target = File.createTempFile("assembly", ".xml");
        FileUtils.copyFile(assemblyDescriptor, target);

        MavenAssemblyUtils.addIncludeToFirstDependencySet(target, include);

        assertEquals(1, new SAXReader().read(target).selectNodes(includeXpath).size());
    }

    @Test
    public void testAddDependencySetFile() throws Exception {
        final String include = "my:dependency:jar";
        final String includeXpath = "//*[text()='" + include + "']";
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());

        File assemblyFile = ProjectUtils.getAssemblyFile("webapps-component.xml");
        File target = File.createTempFile("assembly", ".xml");
        FileUtils.copyFile(assemblyFile, target);

        MavenAssemblyUtils.addDependencySet(target, "webapps", "bmp.war", false,
                "provided", include);
        assertEquals(1, new SAXReader().read(target).selectNodes(includeXpath).size());
    }
}
