/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.facetnavigation;

import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FacetedNavigationConcurrencyTest extends RepositoryTestCase {

    private final Calendar globalCal = Calendar.getInstance();
    
    private static volatile int errorCount = 0;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test
    public void testConcurrentFacNavigationNoWrites() throws RepositoryException, IOException, InterruptedException {
        int jobCount = 1000;
        int workerCount = 40;
        
        commonStart();
        
        
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeValues(testNode);
        session.save();
          
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < jobCount; i++) {
            jobQueue.add(new FacetedTraverser("test/facetnavigation/hippo:navigation", 2));
        }
        
        Thread [] workers = new Thread[workerCount];

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Worker(jobQueue);
        }

        long start = System.currentTimeMillis();
        
        for (Thread worker : workers) {
            worker.start();
        }
        
        for (Thread worker : workers) {
            worker.join();
        }
        
        assertTrue("The job queue is not empty.", jobQueue.isEmpty());
       
        if(errorCount > 0) {
            fail("Exception happened during concurrent traversal.");
        }
    }
    
    @Test
    public void testConcurrentSearchAndWrites() throws RepositoryException, IOException, InterruptedException {
        int jobCount = 1000;
        int workerCount = 40;
        
        commonStart();
        
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        session.save();
          
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < jobCount; i++) {
            jobQueue.add(new Searcher());
        }
        
        Thread [] workers = new Thread[workerCount];

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Worker(jobQueue);
        }

        for (Thread worker : workers) {
            worker.start();
        }
        
        // now while the jobQueue is not empty, we also write nodes:
        int i = 0;
        int j = 0;
        int prevCount = jobQueue.size();
        while(!jobQueue.isEmpty()) {
           i++;
           if(jobQueue.size() > ( prevCount - 3) ) {
               Thread.sleep(2);
               continue;
           }
           prevCount = jobQueue.size();
           
           modifyOrAddCar(testNode, i);
        }
        
        for (Thread worker : workers) {
            worker.join();
        }
        
        assertTrue("The job queue is not empty.", jobQueue.isEmpty());
       
        if(errorCount > 0) {
            fail("Exception happened during concurrent traversal.");
        }
        
        
    }

    @Test
    public void testConcurrentFacNavigationWithWrites() throws RepositoryException, IOException, InterruptedException {
        int jobCount = 1000;
        int workerCount = 40;
        
        commonStart();
        
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeValues(testNode);
        session.save();
          
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < jobCount; i++) {
            jobQueue.add(new FacetedTraverser("test/facetnavigation/hippo:navigation", 2));
        }
        
        Thread [] workers = new Thread[workerCount];

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Worker(jobQueue);
        }

        for (Thread worker : workers) {
            worker.start();
        }
        
        // now while the jobQueue is not empty, we also write nodes:
        int i = 0;
        int prevCount = jobQueue.size();
        while(!jobQueue.isEmpty()) {
           i++;
           if(jobQueue.size() > ( prevCount - 3) ) {
               Thread.sleep(10);
               continue;
           }
           prevCount = jobQueue.size();
           modifyOrAddCar(testNode, i);
        }
        
        for (Thread worker : workers) {
            worker.join();
        }
        
        assertTrue("The job queue is not empty.", jobQueue.isEmpty());
       
        if(errorCount > 0) {
            fail(errorCount + " exceptions happened during concurrent traversal. ");
        }
        
    }

    @Test
    public void testConcurrentSearchAndFacNavigationWithWrites() throws RepositoryException, IOException, InterruptedException {
        int jobCount = 1000;
        int workerCount = 40;
        
        commonStart();
        
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeValues(testNode);
        session.save();
          
        LinkedList<Runnable> jobQueue = new LinkedList<Runnable>();
        
        for (int i = 0; i < jobCount; i++) {
            if(i % 2 == 0) {
                jobQueue.add(new FacetedTraverser("test/facetnavigation/hippo:navigation", 2));
            } else {
                jobQueue.add(new Searcher());
            }
        }
        
        Thread [] workers = new Thread[workerCount];

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Worker(jobQueue);
        }

        for (Thread worker : workers) {
            worker.start();
        }
        
        // now while the jobQueue is not empty, we also write nodes:
        int i = 0;
        int prevCount = jobQueue.size();
        while(!jobQueue.isEmpty()) {
           i++;
           if(jobQueue.size() > ( prevCount - 3) ) {
               Thread.sleep(10);
               continue;
           }
           prevCount = jobQueue.size();
           modifyOrAddCar(testNode, i);
        }
        
        for (Thread worker : workers) {
            worker.join();
        }
        
        assertTrue("The job queue is not empty.", jobQueue.isEmpty());
       
        if(errorCount > 0) {
            fail(errorCount + " exceptions happened during concurrent traversal. ");
        }
        
    }

    private void modifyOrAddCar(Node testNode, int i) throws PathNotFoundException, RepositoryException,
            ItemExistsException, NoSuchNodeTypeException, LockException, VersionException,
            ConstraintViolationException, ValueFormatException, AccessDeniedException, ReferentialIntegrityException,
            InvalidItemStateException {
        Node cars = testNode.getNode("documents").getNode("cars");
           if(i % 5 == 0) {
               // add a car
               String carName = "car"+System.currentTimeMillis();
               Node car = cars.addNode(carName, "hippo:handle");
               car.addMixin("hippo:hardhandle");
               car = car.addNode(carName, "hippo:testcardocument");
               car.addMixin("mix:versionable");
               car.setProperty("hippo:brand", "mercedes");
               car.setProperty("hippo:color", "grey");
               car.setProperty("hippo:product", "car");
               car.setProperty("hippo:date", globalCal);
               car.setProperty("hippo:price", Double.valueOf(i));
               car.setProperty("hippo:travelled", Long.valueOf(i));
           } else {
               // modify an existing car
               NodeIterator nodes = cars.getNodes();
               // skip to different one when size grows
               nodes.skip(nodes.getSize() / 2);
               Node carHandle = nodes.nextNode();
               Node car = carHandle.getNode(carHandle.getName());
               car.setProperty("hippo:brand", "peugeot");
           }
           session.save();
    }
    
    
    private void commonStart() throws RepositoryException {
        session.getRootNode().addNode("test");
        session.save();
    }

    private void createSimpleStructure1(Node test) throws RepositoryException {
        Node documents = test.addNode("documents", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node cars = documents.addNode("cars", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        /*
         * car 0
         * car that has no facets, so should not be visible at all in facet
         */
        Node car = cars.addNode("car0", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car0", "hippo:testcardocument");
        car.addMixin("mix:versionable");

        // car 1
        car = cars.addNode("car1", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car1", "hippo:testcardocument");
        car.addMixin("mix:versionable");
        car.setProperty("hippo:brand", "mercedes");
        car.setProperty("hippo:color", "grey");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 12000.0D);
        car.setProperty("hippo:travelled", 122000L);

        // car 2
        car = cars.addNode("car2", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car2", "hippo:testcardocument");
        car.addMixin("mix:versionable");
        car.setProperty("hippo:brand", "volkswagen");
        car.setProperty("hippo:color", "grey");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 13000.0D);
        car.setProperty("hippo:travelled", 129000L);

        // car 3
        car = cars.addNode("car3", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car3", "hippo:testcardocument");
        car.addMixin("mix:versionable");
        car.setProperty("hippo:brand", "peugeot");
        car.setProperty("hippo:color", "blue");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 11900.0D);
        car.setProperty("hippo:travelled", 99000L);

        test.save();

        // car 4
        car = cars.addNode("car4", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car4", "hippo:testcardocument");
        car.addMixin("mix:versionable");

        // add a facetselect to car 1
        String docbase = cars.getNode("car1").getIdentifier();
        Node facetselect = car.addNode("car1", HippoNodeType.NT_FACETSELECT);
        facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, docbase);
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});

        car.setProperty("hippo:brand", "peugeot");
        car.setProperty("hippo:color", "grey");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 14000.0D);
        car.setProperty("hippo:travelled", 72340L);

    }
   
    private void createFacetNodeValues(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node.addMixin("mix:referenceable");
        node = node.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        node.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:brand", "hippo:color",
                "hippo:product" });

    }
    
    private void traverse(Node node, String indent, int depth) throws RepositoryException {
        if(depth < 0) {
            return;
        }
          
        NodeIterator nodes = node.getNodes();
        --depth;
        while(nodes.hasNext()) {
            Node n = nodes.nextNode();
            traverse(n, "\t"+indent, depth);
        }
       
    }
    
    private class Searcher implements Runnable {

        public Searcher() {
        }

        public void run() {
            Session searchSession = null;
            try {
                searchSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
                Query q = searchSession.getWorkspace().getQueryManager().createQuery("//element(*,hippo:document) order by @jcr:score descending", "xpath");
                q.setLimit(100);
                
                QueryResult result = q.execute();
                
                NodeIterator it = result.getNodes();
                while(it.hasNext()) {
                    // just traverse the result
                    Node n = it.nextNode();
                }
                
            } catch (RepositoryException e) {
                errorCount++;
            } finally {
               if(searchSession != null) {
                   searchSession.logout();
               }
            }
        
        }
    }
    
    private class FacetedTraverser implements Runnable {

        private String facetedNodePath; 
        private int traverseDepth;
        
        public FacetedTraverser(String facetedNodePath, int traverseDepth) {
            this.facetedNodePath = facetedNodePath;
            this.traverseDepth = traverseDepth;
        }

        public void run() {
            Session traversalSession = null;
            try {
                traversalSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
                Node facetedNode = traversalSession.getRootNode().getNode(facetedNodePath);
                traverse(facetedNode, "", traverseDepth);
            } catch (RepositoryException e) {
                errorCount++;
            } finally {
               if(traversalSession != null) {
                   traversalSession.logout();
               }
            }
        
        }
         
        
    }
    
    private class Worker extends Thread {
        
        private LinkedList<Runnable> jobQueue;
        
        public Worker(LinkedList<Runnable> jobQueue) {
            this.jobQueue = jobQueue;
        }
        
        public void run() {
           while (true) {
                Runnable job = null;
                
                synchronized (this.jobQueue) {
                    try {
                        job = this.jobQueue.removeFirst();
                    } catch (NoSuchElementException e) {
                        // job queue is empty, so stop here.
                        break;
                    }
                }
                job.run();
            }
        }        
    }
}
