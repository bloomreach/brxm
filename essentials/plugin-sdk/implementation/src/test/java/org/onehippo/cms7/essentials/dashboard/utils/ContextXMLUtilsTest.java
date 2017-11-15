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
import org.dom4j.io.SAXReader;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContextXMLUtilsTest {
    private static String CONTEXT_RESOURCE = "<Resource\n" +
            "            name=\"jdbc/wpmDS\" auth=\"Container\" type=\"javax.sql.DataSource\"\n" +
            "            maxTotal=\"100\" maxIdle=\"10\" initialSize=\"10\" maxWaitMillis=\"10000\"\n" +
            "            testWhileIdle=\"true\" testOnBorrow=\"false\" validationQuery=\"SELECT 1\"\n" +
            "            timeBetweenEvictionRunsMillis=\"10000\"\n" +
            "            minEvictableIdleTimeMillis=\"60000\"\n" +
            "            username=\"sa\" password=\"\"\n" +
            "            driverClassName=\"org.h2.Driver\"\n" +
            "            url=\"jdbc:h2:./wpm/wpm;AUTO_SERVER=TRUE\"/>";
    @Test
    public void testContextHasNoResource() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("project/conf/context.xml");
        Document doc = new SAXReader().read(stream);
        assertFalse(ContextXMLUtils.hasResources(doc));
        assertFalse(ContextXMLUtils.hasResource(doc,"jdbc/wpmDS"));
    }

    @Test
    public void testAddResource() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("project/conf/context.xml");
        Document doc = new SAXReader().read(stream);
        doc = ContextXMLUtils.addResource(doc, "jdbc/wpmDS", CONTEXT_RESOURCE);

        File result = File.createTempFile("context", ".xml");
        ContextXMLUtils.writeResource(doc, result);
        assertTrue(ContextXMLUtils.hasResource(result, "jdbc/wpmDS"));
    }

    @Test
    public void testAddResourceToFile() throws Exception {
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());
        File source = ProjectUtils.getContextXml();
        File target = File.createTempFile("context", ".xml");
        FileUtils.copyFile(source, target);

        ContextXMLUtils.addResource(target, "jdbc/wpmDS", CONTEXT_RESOURCE);

        assertTrue(ContextXMLUtils.hasResource(target, "jdbc/wpmDS"));
    }

    @Test
    public void testGetContextXmlFile() {
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());
        File contextXml = ProjectUtils.getContextXml();
        assertTrue((contextXml.getName().equals("context.xml")));
    }
}
