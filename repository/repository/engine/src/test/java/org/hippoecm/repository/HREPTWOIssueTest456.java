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

public class HREPTWOIssueTest456 extends TestCase {

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
        Node node = session.getRootNode().addNode("test");
        node = node.addNode("n", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/test");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { });
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { });
        session.save();
        session.refresh(false);
        
        /*
         * session.getRootNode().getNode("test").getNode("n").getNodes().getSize() should equal 1
         * session.getRootNode().getNode("test").getNode("n").getNode("n").getNodes().getSize() should equal 1
         * session.getRootNode().getNode("test").getNode("n").getNode("n").getNode("n").getNodes().getSize() should equal 1
         * etc...
         * Currently, there is an undesired behavior (programmatically correct), where each facet select in facet select adds the childs of the previous  the child nodes, see asserts:
         */
        assertTrue( session.getRootNode().getNode("test").getNode("n").getNodes().getSize() == 1 );
        
        assertFalse(session.getRootNode().getNode("test").getNode("n").getNode("n").getNodes().getSize() == 1 );
        assertTrue( session.getRootNode().getNode("test").getNode("n").getNode("n").getNodes().getSize() == 2 );

        assertFalse(session.getRootNode().getNode("test").getNode("n").getNode("n").getNode("n").getNodes().getSize() == 1);
        assertTrue( session.getRootNode().getNode("test").getNode("n").getNode("n").getNode("n").getNodes().getSize() == 3);
        
    }
}
