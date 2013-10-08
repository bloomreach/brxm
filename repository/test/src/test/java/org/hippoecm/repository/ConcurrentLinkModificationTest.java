/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

/**
 * Concurrent modification tests to test for regressions of data corruption due
 * to http://issues.apache.org/jira/browse/JCR-2129.
 */
public class ConcurrentLinkModificationTest extends RepositoryTestCase {
    private final class LinkModifierThread extends Thread {
        private final Session threadSession;
        private final String uuid;
        private final String path;
        Exception failure = null;
        int count = 0;
        volatile boolean stop = false;

        private LinkModifierThread(Session threadSession, String uuid, String path) {
            this.threadSession = threadSession;
            this.uuid = uuid;
            this.path = path;
        }

        @Override
        public void run() {
            Node root;
            try {
                root = threadSession.getRootNode().getNode("test/" + path);
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
            while (!stop) {
                count++;
                try {
                    if (!root.hasNode("link")) {
                        root.setProperty("x", "x");
                        Node link = root.addNode("link", "hippo:facetselect");
                        link.setProperty("hippo:docbase", uuid);
                        link.setProperty("hippo:facets", new String[0]);
                        link.setProperty("hippo:values", new String[0]);
                        link.setProperty("hippo:modes", new String[0]);
                        threadSession.save();
                    }
                    Node link = root.getNode("link");
                    link.getNode(path);
                    link.remove();
                    root.setProperty("x", "y");
                    threadSession.save();
                } catch (RepositoryException ex) {
                    if (!stop) {
                        failure = ex;
                        stop = true;
                    }
                } catch (RuntimeException ex) {
                    failure = ex;
                    stop = true;
                }
            }
        }
    }


    private Session userSession;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test");
        session.save();
        userSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (userSession != null && userSession.isLive()) {
            userSession.logout();
            userSession = null;
        }
        super.tearDown();
    }

    @Test
    public void dummy() {
    }

    @Ignore
    public void testConcurrency() throws Exception {
        // maak virtuele nodes aan / verwijder, multi-session met observers
        session.getWorkspace().getObservationManager().addEventListener(
                new EventListener() {

                    public void onEvent(EventIterator events) {
                        while (events.hasNext()) {
                            Event event = events.nextEvent();
                            try {
                                event.getPath();
                            } catch (RepositoryException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }

                },
                Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED
                        | Event.PROPERTY_REMOVED, "/", true, null, null, true);
        userSession.getWorkspace().getObservationManager().addEventListener(
                new EventListener() {

                    public void onEvent(EventIterator events) {
                        while (events.hasNext()) {
                            Event event = events.nextEvent();
                            try {
                                event.getPath();
                            } catch (RepositoryException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }

                },
                Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED
                        | Event.PROPERTY_REMOVED, "/", true, null, null, true);
        Node testNode = session.getRootNode().getNode("test");
        testNode.addMixin("mix:referenceable");
        session.save();
        final String uuid = testNode.getIdentifier();
        LinkModifierThread[] threads = new LinkModifierThread[2];
        for (int i = 0; i < 2; i++) {
            final Session threadSession;
            final String path;
            switch (i) {
            case 0:
                threadSession = session;
                path = "one";
                testNode.addNode(path, "nt:unstructured");
                break;
            default:
                threadSession = userSession;
                path = "two";
                testNode.addNode(path, "nt:unstructured");
            }
            threads[i] = new LinkModifierThread(threadSession, uuid, path);
        }
        session.save();
        for (int i = 0; i < 2; i++) {
            threads[i].start();
        }
        try {
            for (int j = 0; j < 60; j++) {
                Thread.sleep(1000);
                if (threads[0].stop || threads[1].stop) {
                    break;
                }
            }
        } finally {
            for (int i = 0; i < 2; i++) {
                threads[i].stop = true;
                threads[i].interrupt();
            }
            for (int i = 0; i < 2; i++) {
                threads[i].join();
            }
        }
        for (int i = 0; i < 2; i++) {
            if (threads[i].failure != null) {
                throw threads[i].failure;
            }
            else {
                System.out.println("Thread " + i + ", count " + threads[i].count);
            }
        }
    } 

}
