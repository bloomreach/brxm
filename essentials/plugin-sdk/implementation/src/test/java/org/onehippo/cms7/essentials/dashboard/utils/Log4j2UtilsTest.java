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
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Log4j2UtilsTest {

    @Test
    public void testHasLogger() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("log4j2.xml");

        Document doc = new SAXReader().read(stream);
        assertTrue(Log4j2Utils.hasLogger(doc, "org.apache.jackrabbit.core"));
        assertFalse(Log4j2Utils.hasLogger(doc, "non.existing.logger"));

        final URL resource = getClass().getResource("/log4j2.xml");
        final File source = new File(GlobalUtils.decodeUrl(resource.getPath()));

        assertTrue(Log4j2Utils.hasLogger(source, "org.apache.jackrabbit.core"));
        assertFalse(Log4j2Utils.hasLogger(source, "non.existing.logger"));
    }

    @Test
    public void testAddLoggerStream() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("log4j2.xml");
        Document doc = new SAXReader().read(stream);

        Log4j2Utils.addLogger(doc, "new.essentials.logger", "warn");
        Log4j2Utils.addLogger(doc, "new.essentials.logger.next", "warn");

        File result = File.createTempFile("log4j2", ".xml");
        Log4j2Utils.writeLog4j2(doc, result);
        assertTrue(Log4j2Utils.hasLogger(result, "new.essentials.logger.next"));
    }

    @Test
    public void testAddLoggerToFile() throws Exception {
        final URL resource = getClass().getResource("/log4j2.xml");
        final File source = new File(GlobalUtils.decodeUrl(resource.getPath()));
        File target = File.createTempFile("log4j2", ".xml");
        FileUtils.copyFile(source, target);

        Log4j2Utils.addLogger(target, "new.essentials.logger", "warn");
        Log4j2Utils.addLogger(target, "new.essentials.logger.next", "warn");
        assertTrue(Log4j2Utils.hasLogger(target, "new.essentials.logger.next"));
    }

    @Test
    public void testGetLog4j2Files() {
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());
        List<File> log4j2Files = ProjectUtils.getLog4j2Files();
        assertEquals(2, log4j2Files.size());
        assertTrue((log4j2Files.get(0).getName().startsWith("log4j2")));
        assertTrue((log4j2Files.get(0).getName().endsWith(".xml")));
        assertTrue((log4j2Files.get(1).getName().startsWith("log4j2")));
        assertTrue((log4j2Files.get(1).getName().endsWith(".xml")));
    }

    @Test
    public void testAddLoggerToLog4j2Files() {
        System.setProperty("project.basedir", getClass().getResource("/project").getPath());

        Log4j2Utils.addLoggerToLog4j2Files("org.onehippo.cms7.unit.test", "warn");

    }
}
