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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class FacetedNavigationRemoteTest extends FacetedNavigationAbstractTest {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepositoryServer backgroundServer;
    private HippoRepository server;

    public void setUp() throws Exception {
        backgroundServer = new HippoRepositoryServer();
        backgroundServer.run(true);
        Thread.sleep(3000);
        server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        // first clean possible old entries
        for (NodeIterator iter = session.getRootNode().getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (!child.getPath().equals("/jcr:system")) {
                child.remove();
            }
        }
        session.save();
        session.getRootNode().addNode("navigation");
    }

    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("navigation")) {
            session.getRootNode().getNode("navigation").remove();
        }
        if(session.getRootNode().hasNode("documents")) {
            session.getRootNode().getNode("documents").remove();
        }
        session.save();
        if(session != null) {
            session.logout();
        }
        if (server != null) {
            server.close();
        }
        backgroundServer.close();
        Thread.sleep(3000);
    }

    public void testTraversal() throws RepositoryException, IOException {
        Node node = commonStart();
        traverse(node); // for a full verbose dump use: Utilities.dump(root);
        commonEnd();
    }

    public void testCounts() throws RepositoryException, IOException {
        numDocs = 500;
        Node node = commonStart();
        check("/navigation/xyz/x1", 1, 0, 0);
        check("/navigation/xyz/x2", 2, 0, 0);
        check("/navigation/xyz/x1/y1", 1, 1, 0);
        check("/navigation/xyz/x1/y2", 1, 2, 0);
        check("/navigation/xyz/x2/y1", 2, 1, 0);
        check("/navigation/xyz/x2/y2", 2, 2, 0);
        check("/navigation/xyz/x1/y1/z1", 1, 1, 1);
        check("/navigation/xyz/x1/y1/z2", 1, 1, 2);
        check("/navigation/xyz/x1/y2/z1", 1, 2, 1);
        check("/navigation/xyz/x1/y2/z2", 1, 2, 2);
        check("/navigation/xyz/x2/y1/z1", 2, 1, 1);
        check("/navigation/xyz/x2/y1/z2", 2, 1, 2);
        check("/navigation/xyz/x2/y2/z1", 2, 2, 1);
        check("/navigation/xyz/x2/y2/z2", 2, 2, 2);
        commonEnd();
    }
}
