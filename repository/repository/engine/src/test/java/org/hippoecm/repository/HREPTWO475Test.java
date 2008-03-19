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
 * See the License for the specific lang governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippoecm.repository.api.HippoNodeType;

public class HREPTWO475Test extends TestCase {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected HippoRepository server;
    protected Session session;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
    }

    public void tearDown() throws Exception {
        session.refresh(false);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        if(session != null) {
            session.logout();
        }
        if (server != null) {
            server.close();
        }
    }

    public void testIssue() throws RepositoryException {
        Node node, child, root = session.getRootNode().addNode("test");

        node = root.addNode("docs");
        node = node.addNode("doc1",HippoNodeType.NT_HANDLE);
        child = node.addNode("doc1",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","en");
        child = node.addNode("doc1",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","nl");

        node = root.getNode("docs");
        node = node.addNode("doc2",HippoNodeType.NT_HANDLE);
        child = node.addNode("doc2",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","nl");
        child = node.addNode("doc2",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","en");

        node = root.getNode("docs");
        node = node.addNode("doc3","nt:unstructured");
        child = node.addNode("doc3",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","nl");
        child = node.addNode("doc3",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","en");

        node = root.getNode("docs").addNode("sub");
        node = node.addNode("doc4",HippoNodeType.NT_HANDLE);
        child = node.addNode("doc4",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","en");
        child = node.addNode("doc4",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","nl");

        node = root.getNode("docs").getNode("sub");
        node = node.addNode("doc5",HippoNodeType.NT_HANDLE);
        child = node.addNode("doc5",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","nl");
        child = node.addNode("doc5",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","en");

        node = root.getNode("docs").getNode("sub");
        node = node.addNode("doc6","nt:unstructured");
        child = node.addNode("doc6",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","nl");
        child = node.addNode("doc6",HippoNodeType.NT_DOCUMENT);
        child.setProperty("lang","en");

        node = root.addNode("nav","hippo:facetselect");
        node.setProperty("hippo:docbase","/test/docs");
        node.setProperty("hippo:facets",new String[] { "lang" });
        node.setProperty("hippo:values",new String[] { "nl" });
        node.setProperty("hippo:modes",new String[] { "select" });
        session.save();

        node = root.getNode("nav");

        assertTrue(node.getNode("doc3").hasNode("doc3"));
        assertEquals("nl",node.getNode("doc3").getNode("doc3").getProperty("lang").getString());
        assertTrue(node.getNode("doc3").hasNode("doc3[2]"));
        assertEquals("en",node.getNode("doc3").getNode("doc3[2]").getProperty("lang").getString());

        assertTrue(node.getNode("doc1").hasNode("doc1"));
        assertEquals("nl",node.getNode("doc1").getNode("doc1").getProperty("lang").getString());
        assertFalse(node.getNode("doc1").hasNode("doc1[2]"));
        assertTrue(node.getNode("doc2").hasNode("doc2"));
        assertEquals("nl",node.getNode("doc2").getNode("doc2").getProperty("lang").getString());
        assertFalse(node.getNode("doc2").hasNode("doc2[2]"));

        assertTrue(node.getNode("sub").getNode("doc6").hasNode("doc6"));
        assertEquals("nl",node.getNode("sub").getNode("doc6").getNode("doc6").getProperty("lang").getString());
        assertTrue(node.getNode("sub").getNode("doc6").hasNode("doc6[2]"));
        assertEquals("en",node.getNode("sub").getNode("doc6").getNode("doc6[2]").getProperty("lang").getString());

        assertTrue(node.getNode("sub").getNode("doc4").hasNode("doc4"));
        assertEquals("nl",node.getNode("sub").getNode("doc4").getNode("doc4").getProperty("lang").getString());
        assertFalse(node.getNode("sub").getNode("doc4").hasNode("doc4[2]"));
        assertTrue(node.getNode("sub").getNode("doc5").hasNode("doc5"));
        assertEquals("nl",node.getNode("sub").getNode("doc5").getNode("doc5").getProperty("lang").getString());
        assertFalse(node.getNode("sub").getNode("doc5").hasNode("doc5[2]"));

        session.getRootNode().getNode("test").remove();
        session.save();
    }
}
