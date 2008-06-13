/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.admin.upload;

import java.util.Calendar;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.decorating.PluginClassLoader;

public class ClassLoaderTest extends TestCase {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();
    private static final String JAR_FILE_ENTRY = "mock/aDir/anotherFile.txt";

    private HippoRepository server;
    private Session session;
    private PluginClassLoader loader;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        loadRepository();

        loader = new PluginClassLoader(session);

    }

    public void tearDown() throws Exception {
        if (loader != null) {
            loader.destroy();
        }
        session.getRootNode().getNode("hippo:plugins").remove();
        session.save();
        session.logout();
        server.close();
    }

    public void testClassloader() throws Exception {
        URL resource = loader.getResource(JAR_FILE_ENTRY);
        assertNotNull("Resource " + JAR_FILE_ENTRY, resource);
    }

    protected void loadRepository() throws Exception {
        Node root = session.getRootNode();
        Node node = root.addNode("hippo:plugins", "hippo:pluginfolder");
        node = node.addNode("test-plugin", "hippo:plugin");
        node = node.addNode("mock.jar", "nt:resource");

        URL url = getClass().getResource("/mock.jar");
        assertNotNull(url);

        node.setProperty("jcr:mimeType", "application/octet-stream");
        node.setProperty("jcr:data", url.openConnection().getInputStream());
        node.setProperty("jcr:lastModified", Calendar.getInstance());

        JarExpander expander = new JarExpander(node);
        expander.extract();
    }
}
