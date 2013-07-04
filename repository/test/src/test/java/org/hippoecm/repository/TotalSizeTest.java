/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TotalSizeTest extends RepositoryTestCase {

    @Test
    public void testTotalSize() throws RepositoryException {
        Session authorSession = server.login("author", "author".toCharArray());
        assertEquals("author", authorSession.getUserID());

        String queryStatement = "SELECT * FROM hipposys:domain ORDER BY jcr:score";
        String queryLanguage = Query.SQL;

        Query adminQuery = session.getWorkspace().getQueryManager().createQuery(queryStatement, queryLanguage);
        adminQuery.setLimit(1);
        QueryResult adminResult = adminQuery.execute();
        NodeIterator adminIterator = adminResult.getNodes();
        assertTrue(adminIterator.getSize() > 0L);
        assertTrue(adminIterator instanceof HippoNodeIterator);
        long adminTotal = ((HippoNodeIterator)adminIterator).getTotalSize();
        int adminCount = 0;
        while(adminIterator.hasNext()) {
            Node node = adminIterator.nextNode();
            if (node != null) {
                ++adminCount;
            }
        }
        long adminSize = adminIterator.getSize();
        assertTrue(adminTotal > adminSize);
        assertEquals(adminCount, adminSize);

        // now do the same search but set a limit of 5. We then expect a getSize of 5, but a getTotalSize of *all* the hits
        // not taking the setLimit into account

        adminQuery.setLimit(5);
        adminResult = adminQuery.execute();
        assertEquals(5L , adminResult.getNodes().getSize());

        // the getTotalSize should still equal adminTotal !!
        assertEquals(adminTotal , ((HippoNodeIterator)adminResult.getNodes()).getTotalSize());

        Query authorQuery = authorSession.getWorkspace().getQueryManager().createQuery(queryStatement, queryLanguage);
        QueryResult authorResult = authorQuery.execute();
        NodeIterator authorIterator = authorResult.getNodes();
        assertTrue(authorIterator instanceof HippoNodeIterator);
        long authorTotal = ((HippoNodeIterator)authorIterator).getTotalSize();
        int authorCount = 0;
        while(authorIterator.hasNext()) {
            Node node = authorIterator.nextNode();
            if (node != null) {
                ++authorCount;
            }
        }
        assertEquals(0L, authorIterator.getSize());
        assertEquals(0L, authorCount);
        assertEquals(0L, authorTotal);
        authorSession.logout();
    }

    @Test
    public void testSizesNoOrderBy() throws RepositoryException {
        Session authorSession = server.login("author", "author".toCharArray());
        assertEquals("author", authorSession.getUserID());

        String queryStatement = "//element(*,hippo:document)";
        String queryLanguage = Query.XPATH;

        Query adminQuery = session.getWorkspace().getQueryManager().createQuery(queryStatement, queryLanguage);
        QueryResult adminResult = adminQuery.execute();
        NodeIterator adminIterator = adminResult.getNodes();
        // getTotalSize return -1 for searches without order by
        assertEquals(-1L, ((HippoNodeIterator) adminIterator).getTotalSize());

        Query authorQuery = authorSession.getWorkspace().getQueryManager().createQuery(queryStatement, queryLanguage);
        QueryResult authorResult = authorQuery.execute();
        NodeIterator authorIterator = authorResult.getNodes();
        // getTotalSize return -1 for searches without order by
        assertEquals(-1L, ((HippoNodeIterator) authorIterator).getTotalSize());
        authorSession.logout();
    }

    @Test
    public void testAuthorSearchesAndChanges() throws RepositoryException {

        Session authorSession = server.login("author", "author".toCharArray());
        assertEquals("author", authorSession.getUserID());

        String queryStatement = "/jcr:root/test//element(*,hippo:translation) order by @jcr:score";
        String queryLanguage = Query.XPATH;
        Query authorQuery = authorSession.getWorkspace().getQueryManager().createQuery(queryStatement, queryLanguage);
        authorQuery.setLimit(1);
        // we set a limit of 1 so getSize at most 1 and getTotalSize might be bigger

        QueryResult authorResult = authorQuery.execute();
        NodeIterator authorIterator = authorResult.getNodes();

        assertEquals(0L, authorIterator.getSize());
        assertEquals(0L, ((HippoNodeIterator) authorIterator).getTotalSize());
        
        // create with admin session a 'hippo:translation' node as this one is readable for authors (readable for everybody)
        Node test =  session.getRootNode().addNode("test", "nt:unstructured");
        Node translation = test.addNode("dummytranslation", "hippo:translation");
        translation.setProperty("hippo:language", "en");
        translation.setProperty("hippo:message", "readable by author");
        session.save();

        // now we should get a hit for author query when do the query again
        authorResult = authorQuery.execute();
        authorIterator = authorResult.getNodes();
        assertEquals(1L, authorIterator.getSize());
        assertEquals(1L, ((HippoNodeIterator) authorIterator).getTotalSize());

        // add another translation
        translation = test.addNode("dummytranslation2", "hippo:translation");
        translation.setProperty("hippo:language", "en");
        translation.setProperty("hippo:message", "readable by author");
        session.save();

        // now we should get a hit for author query when do the query again. Because setLimit = 1 still, we have getSize() =1
        authorResult = authorQuery.execute();
        authorIterator = authorResult.getNodes();
        assertEquals(1L, authorIterator.getSize());
        assertEquals(2L, ((HippoNodeIterator) authorIterator).getTotalSize());
        authorSession.logout();
    }

    /**
     * This unit tests start writing with the admin user to the repository.
     * Most of the workers write nodes that cannot be read by the AUTHOR session, but 
     * some or one of them writes 'hippo:translation's which are readable. 
     * This test is to assure caching of the AuthorizationQuery as bitset always
     * works correct and invalidates when needed
     * An author is in this test set not allowed to READ *hippo:handle* or *hippo:document*
     * @throws RepositoryException
     */

    private volatile long expectedTotalSizeTranslationNodes;

    @Test
    public void testAuthorSearchesAndConcurrentChanges() throws RepositoryException, InterruptedException {

        int nodesToWriteCount = 500;
        int workerCount = 20;
       // int nodesToWriteCount = 1000;
       // int workerCount = 40;

        Session authorSession = server.login("author", "author".toCharArray());
        // get initial number of translation nodes that the author is allowed to read
        String queryStatement = "//element(*,hippo:translation) order by @jcr:score";
        String queryLanguage = Query.XPATH;
        Query authorQuery = authorSession.getWorkspace().getQueryManager().createQuery(queryStatement, queryLanguage);
        authorQuery.setLimit(1);
        // we set a limit of 1 so getSize at most 1 and getTotalSize might be bigger
        QueryResult authorResult = authorQuery.execute();
        long initialNumberOfSearchHits = ((HippoNodeIterator) authorResult.getNodes()).getTotalSize();
        expectedTotalSizeTranslationNodes = initialNumberOfSearchHits;

        // below we start writing with multiple sessions, and sometimes add a node that is readable
        // for the authorSession
        
        LinkedList<NodeWriter> jobQueue = new LinkedList<NodeWriter>();
        for (int i = 0; i < nodesToWriteCount; i++) {
            jobQueue.add(new NodeWriterImpl());
        }

        Thread [] workers = new Thread[workerCount];

        Node test =  session.getRootNode().addNode("test", "nt:unstructured");
        // every worker will write below its own nodes only:
        for (int i = 0; i < workerCount; i++) {
            Node workerNode;
            if (i == 0) {
                // this is the worker that ADDs nodes that are READABLE for author session
                workerNode = test.addNode("translations", "nt:unstructured");
            } else {
                workerNode = test.addNode("handle"+i, "hippo:handle");
            }
            workers[i] = new Worker(jobQueue, workerNode.getPath());
        }
        session.save();
        
        for (Thread worker : workers) {
            worker.start();
        }

        // now while the jobQueue is not empty, we search for nodes:
        int i = 0;
        while(!jobQueue.isEmpty()) {
            Thread.sleep(3);
            // SEARCH 
            // every time AuthorReadableNodeWriter adds a node below 
            // /jcr:root/test of type hippo:translation, we should get an extra hit.
            // because at the same time as we search  a new node can be added, the hit size might
            // be a little off.
            // This is also to ensure that we do not get repository exception in the caching of the
            // authorization query
            authorResult = authorQuery.execute();
            long totalSizeFound = ((HippoNodeIterator) authorResult.getNodes()).getTotalSize();
            // the totalSizeFound must be equal or less then expectedTotalSizeTranslationNodes
            assertTrue(totalSizeFound <= expectedTotalSizeTranslationNodes);
        }

        for (Thread worker : workers) {
            worker.join();
        }

        // now to one more search : All workers are done, we should now find expectedTotalSizeTranslationNodes
        authorResult = authorQuery.execute();
        long totalSizeFound = ((HippoNodeIterator) authorResult.getNodes()).getTotalSize();
        assertEquals(expectedTotalSizeTranslationNodes, totalSizeFound);
        authorSession.logout();
    }

    private interface NodeWriter extends Runnable {
        void setAbsWorkerPath(String absPath);
    }

    private class NodeWriterImpl implements NodeWriter {

        private String absWorkerPath;
        
        public void run() {
            
            Session adminSession = null;
            try {
                adminSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
                Node workerNode = adminSession.getNode(absWorkerPath);
                if (absWorkerPath.equals("/test/translations")) {
                    // add unique named node for translation
                    // NOTE that AUTHORS can read translations
                    Node translation = workerNode.addNode("mytranslation" + System.nanoTime(), "hippo:translation");
                    translation.setProperty("hippo:language", "en");
                    translation.setProperty("hippo:message", "readable by author");
                    expectedTotalSizeTranslationNodes++;
                } else {
                    // NOTE that AUTHORS cannot read hippo:document in this test setup
                    workerNode.addNode("mydoc", "hippo:document");
                }
                adminSession.save();

            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            finally {
                if(adminSession != null) {
                    adminSession.logout();
                }
            }

        }

        @Override
        public void setAbsWorkerPath(final String absPath) {
            this.absWorkerPath = absPath;
        }
    }

    
    private class Worker extends Thread {

        private LinkedList<NodeWriter> jobQueue;
        private String absWorkerPath;

        public Worker(LinkedList<NodeWriter> jobQueue, final String absWorkerPath) {
            this.jobQueue = jobQueue;
            this.absWorkerPath = absWorkerPath;
        }

        public void run() {
            while (true) {
                NodeWriter job = null;

                synchronized (this.jobQueue) {
                    try {
                        job = this.jobQueue.removeFirst();
                    } catch (NoSuchElementException e) {
                        // job queue is empty, so stop here.
                        break;
                    }
                }
                job.setAbsWorkerPath(absWorkerPath);
                job.run();
            }
        }
    }

}
