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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import junit.framework.TestCase;

import org.hippoecm.repository.api.HippoNodeType;

public class FacetSearchDemoTest extends TestCase {
    
    private static final String SYSTEMUSER_ID = "systemuser";
    private static final char[] SYSTEMUSER_PASSWORD = "systempass".toCharArray();

    private boolean verbose = false;
    private Session session = null;
    private HippoRepository server = null;

    public FacetSearchDemoTest() {
    }

    protected void traverse(Node node) throws RepositoryException {
        if(verbose) {
            if(node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                System.out.println(node.getPath() + "\t" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
            }
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (!child.getPath().equals("/jcr:system")) {
                traverse(child);
            }
        }
    }

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
    }

    public void tearDown() throws Exception {
        session.save();
        if(session != null) {
            session.logout();
        }
        if (server != null) {
            server.close();
        }
    }

    public void testTraversal() throws RepositoryException, IOException {
        Utilities.dump(session.getRootNode().getNode("facetsearch-demo"));
	String[] tests = {
            "facetsearch-demo/demo/By Brand/hippo:resultset/DeWalt Drill-Saw Kit 19.2v 9884CS",
            "facetsearch-demo/demo/By Brand/Bosch/hippo:resultset/Bosch 18-Volt Brute Tough Drill-Driver 33618"
        };
	for(int i=0; i<tests.length; i++) {
            String[] pathElements = tests[i].split("/");
            Node node = session.getRootNode();
            for(int j=0; j<pathElements.length; j++) {
                boolean found = false;
                for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                    Node child = iter.nextNode();
                    if(child != null && child.getName().equals(pathElements[j]))
                        found = true;
                }
                assertTrue(found);
                assertTrue(node.hasNode(pathElements[j]));
                try {
                    node = node.getNode(pathElements[j]);
                } catch(PathNotFoundException ex) {
                    fail();
                }
                assertNotNull(node);
            }
	}
    }

}
