/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.test;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoWorkspace;

public class PersistanceInheritanceTest extends TestCase
{
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepository server;
    private Session session;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();

        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node node, root = session.getRootNode();

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("documents/$name", Query.XPATH);
        node = query.storeAsNode("/hippo:configuration/hippo:documents/test");
        String statement = node.getProperty("jcr:statement").getString();
        String language = node.getProperty("jcr:language").getString();
        node.remove();
        node = root.getNode("hippo:configuration/hippo:documents");
        node = node.addNode("test", "hippo:ocmquery");
        node.setProperty("jcr:statement",statement);
        node.setProperty("jcr:language",language);
        node.setProperty("hippo:classname","org.hippoecm.repository.test.SubClass");
        Node types = node.getNode("hippo:types");
        node = types.addNode("org.hippoecm.repository.test.SubClass","hippo:type");
        node.setProperty("hippo:nodetype","nt:unstructured");
        node.setProperty("hippo:display","Sub");
        node.setProperty("hippo:classname","org.hippoecm.repository.test.SubClass");
        node = types.addNode("org.hippoecm.repository.test.SuperClass","hippo:type");
        node.setProperty("hippo:nodetype","nt:unstructured");
        node.setProperty("hippo:display","Super");
        node.setProperty("hippo:classname","org.hippoecm.repository.test.SuperClass");

        session.save();

        if(root.hasNode("documents"))
            root.getNode("documents").remove();
        node = root.addNode("documents");
        node = node.addNode("mydocument", "hippo:testdocument");
        node.addMixin("hippo:harddocument");
        node.setProperty("a", "1");
        node.setProperty("b", "2");

        session.save();
    }

    public void tearDown() throws Exception {
        session.getRootNode().getNode("hippo:configuration").remove();
        session.getRootNode().getNode("documents").remove();
        session.save();
        server.close();
    }

    public void testInheritance() throws RepositoryException {
        HippoWorkspace wsp = (HippoWorkspace)(session.getWorkspace());
        DocumentManager dmngr = wsp.getDocumentManager();
        SubClass document = (SubClass) dmngr.getDocument("test", "mydocument");
        assertNotNull(document);
        assertEquals("1", document.superfield);
        assertEquals("2", document.subfield);
    }
}
