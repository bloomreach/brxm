/*
 *  Copyright 2008,2009 Hippo.
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
package org.hippoecm.frontend.plugins.standards.upload;

import java.net.URL;
import java.util.Calendar;

import javax.jcr.Node;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.impl.PluginClassLoader;
import org.hippoecm.repository.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

public class ClassLoaderTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String JAR_FILE_ENTRY = "mock/aDir/anotherFile.txt";

    private PluginClassLoader loader;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode(HippoNodeType.PLUGIN_PATH, "nt:unstructured");
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if (loader != null) {
            loader.destroy();
        }
        session.getRootNode().getNode(HippoNodeType.PLUGIN_PATH).remove();
        session.save();
        super.tearDown();
    }

    @Test
    public void testClassloader() throws Exception {
        Node node = session.getRootNode().getNode(HippoNodeType.PLUGIN_PATH);
        node = node.addNode("test-plugin", "nt:unstructured");
        node = node.addNode("mock.jar", "nt:resource");

        URL url = getClass().getResource("/mock.jar");
        assertNotNull(url);

        node.setProperty("jcr:mimeType", "application/octet-stream");
        node.setProperty("jcr:data", url.openConnection().getInputStream());
        node.setProperty("jcr:lastModified", Calendar.getInstance());

        JarExpander expander = new JarExpander(node);
        expander.extract();
        loader = new PluginClassLoader(session);

        URL resource = loader.getResource(JAR_FILE_ENTRY);
        assertNotNull("Resource " + JAR_FILE_ENTRY, resource);
    }
}
