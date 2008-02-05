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
package org.hippoecm.repository;

import java.io.IOException;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.Utilities;

import junit.framework.TestCase;

public class HREPTWO451Test extends TestCase {

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
        Node node, root = session.getRootNode().addNode("test");
        node = root.addNode("documents");
        node = node.addNode("document",HippoNodeType.NT_DOCUMENT);
        node.setProperty("hippo:testfacet", "aap");
        session.save();
        node = root.addNode("navigation");
        node = node.addNode("search",HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "search");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/test/documents");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:testfacet" });
        session.save();
        assertTrue(root.getNode("navigation").getNode("search").hasNode("aap"));
        assertTrue(root.getNode("navigation").getNode("search").getNode("aap").getNode("hippo:resultset").hasNode("document"));
        assertTrue(root.getNode("navigation").getNode("search").getNode("hippo:resultset").hasNode("document"));
    }
}
