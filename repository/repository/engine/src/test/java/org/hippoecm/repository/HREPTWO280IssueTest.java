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

public class HREPTWO280IssueTest extends FacetedNavigationAbstractTest {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    public void testIssue() throws RepositoryException {
        commonStart();

        Node node, child, searchNode = session.getRootNode().getNode("navigation").getNode("xyz");
        traverse(session.getRootNode().getNode("navigation"));

        node = session.getRootNode().getNode("documents").addNode("aap");
        node.setProperty("x", "x1");
        session.save();

        searchNode = session.getRootNode().getNode("navigation").getNode("xyz");
        traverse(searchNode);

        try {
            session.getRootNode().getNode("navigation").remove();
            System.gc();  // reproducability of correct result
            session.save();
            session.refresh(false);
            session.getRootNode().getNode("documents").remove();
            session.save();
            session.refresh(false);
            System.err.println("ISSUE HREPTWO-280 resolved (part 2, 3)");
        } catch(NullPointerException ex) {
            System.err.println("ISSUE HREPTWO-280 still present (part 2)");
            System.err.println(ex.getMessage());
            // ex.printStackTrace(System.err);
        } catch(RepositoryException ex) {
            System.err.println("ISSUE HREPTWO-280 still present (part 3)");
            System.err.println(ex.getMessage());
            // ex.printStackTrace(System.err);
        } finally {
            session.logout();
            session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        }

        commonEnd();
    }


    @Override
    public void testPerformance() throws RepositoryException, IOException {
    }

}
