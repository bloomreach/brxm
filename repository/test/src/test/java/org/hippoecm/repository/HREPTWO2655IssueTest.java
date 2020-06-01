/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

/**
 * An example class to show how to write unit tests for the repository.
 */
public class HREPTWO2655IssueTest extends RepositoryTestCase {

    private Session session2;

    String[] content1 = {
      "/test", "nt:unstructured",
      "/test/documents", "nt:unstructured",
      "jcr:mixinTypes", "mix:referenceable"
    };

    String[] content2 = {
      "/test/mirror", "hippo:facetselect",
      "hippo:docbase", "/test/documents",
      "hippo:facets", null,
      "hippo:values", null,
      "hippo:modes", null
    };

    String[] content3 = {
      "/test/documents/document", "hippo:document",
      "jcr:mixinTypes", "mix:versionable"
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content1, session);
        build(content3, session);
        session.save();
    }

    volatile boolean stop = false;

    @Ignore("timeout issue test not supported")
    // @Test(timeout = 60000)
    public void testIssue() throws Exception {
        session2 = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        ObservationManager obsmgr = session2.getWorkspace().getObservationManager();
        obsmgr.addEventListener(new Listener(), Event.NODE_ADDED, "/test", true, null, new String[] {"nt:unstructured"}, true);
        
        Thread thread = new Thread() {
            @Override public void run() {
                try {
                    for (int i = 0; i < 1000 && !stop; i++) {
                        session2.getRootNode().getNode("test/documents").addNode("dummy");
                        session2.save();
                    }
                } catch (RepositoryException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
        };
        thread.start();
        Thread.sleep(100);
        
        build(content2, session);
        session.save();
        //build(session, content3);
        session.save();
        stop = true;
        thread.join();
    }

    @Test
    public void testDummy() {
    }
    
    class Listener implements EventListener, SynchronousEventListener {
        public void onEvent(EventIterator events) {
            try {
                session2.refresh(false);
                while (events.hasNext()) {
                    Event event = events.nextEvent();
                    String path = event.getPath();
                    Node node = session2.getRootNode().getNode(path.substring(1));
                    for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                        node = iter.nextNode();
                        if(node.isNodeType("hippo:facetselect")) {
                            String uuid = node.getProperty("hippo:docbase").getString();
                            node = session2.getNodeByUUID(uuid);
                            node.getPath();
                        }
                    }
                    session2.getRootNode().getNode("test/mirror").getNode("document");
                }
            } catch (RepositoryException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }
}
