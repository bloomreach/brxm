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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * These tests try all kind of variants of browsing through facetted navigation and
 * changing, with and without saving physical content (real nodes and external nodes),
 * changing content from within other sessions, and concurrent runs. The tests should indicate
 * when there is a problem with the HippoLocalISM. Though take into account, that we are not testing
 * the HippoLocalISM directly because this is simply not possible, but only indirect by doing many
 * different tests, involving different JCR calls. This class cannot garantuee that there are
 * no possible errors in the HippoLocalISM. Though, it might be the first indication when something
 * regargding the HippoLocalISM is broken.
 *
 * If one of these tests fails, it might indicate that the <code>HippoLocalItemStateManager</code>
 * has a problem. Also changes to the item state managers in Jackrabbit trunk might result
 * in errors in these tests (the HippoLocalItemStateManager is pretty close coupled to the ISM;'s
 * in Jackrabbit)
 */
public class HippoISMTest extends FacetedNavigationAbstractTest {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
         super.tearDown();
    }

    @Test
    public void testTrivialMultipleTraverseVirtualNavigation() throws RepositoryException{
        try {
            commonStart();
            // external node indicates for the half regular half virtual nodes
            Node externalNode = getSearchNode();
            traverse(externalNode);
            traverse(externalNode);
            traverse(externalNode);
        } catch(NullPointerException ex) {
            fail(ex.getMessage());
        } catch(RepositoryException ex) {
            fail(ex.getMessage());
        } finally {
            commonEnd();
        }
    }

    @Test
    public void testSaveRefetchExternalNode() throws RepositoryException {
        try {
            commonStart();
            Node node, child, searchNode = session.getRootNode().getNode("test/navigation").getNode("xyz");

            long countBefore = searchNode.getNode("x1").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
            session.refresh(false);
            session.save();

            node = session.getRootNode().getNode("test/documents");

            for(int i =1 ; i < 50; i++){
                child = node.addNode("test"+i, "hippo:testdocument");
                child.addMixin("mix:versionable");
                child.setProperty("x", "x1");
                child.setProperty("y", "yy");
                node.save();
                // refetch searchNode
                searchNode = session.getRootNode().getNode("test/navigation").getNode("xyz");
                long countAfter = searchNode.getNode("x1").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
                assertEquals(countBefore + i, countAfter);
            }
            commonEnd();
        } catch(NullPointerException ex) {
            System.out.println(ex);
            fail(ex.getMessage());
        } catch(RepositoryException ex) {
            System.out.println(ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void testSaveNoRefetchExternalNode() throws RepositoryException {
        try {
            commonStart();
            Node node, child, searchNode = session.getRootNode().getNode("test/navigation").getNode("xyz");
            traverse(searchNode);
            traverse(searchNode);
            long countBefore = searchNode.getNode("x1").getProperty(HippoNodeType.HIPPO_COUNT).getLong();

            node = session.getRootNode().getNode("test/documents");
            child = node.addNode("test", HippoNodeType.NT_DOCUMENT);
            child.setProperty("x", "x1");
            child.setProperty("y", "yy");
            node.save();

            /*
             * Without a refetch of the searchNode after a save(), a repository exception is allowed
             * because the node might not exist anymore. Refetch of a virtual/external node is needed after a save().
             */
            searchNode.getNode("x1").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
            fail("An exception should have been thrown");
        } catch(NullPointerException ex) {
            fail(ex.getMessage());
        } catch(InvalidItemStateException ex) {
            assertNotNull("Exception allowed: item needs to be refetched", ex);
        } catch(RepositoryException ex) {
            assertNotNull("Exception allowed: item needs to be refetched", ex);
        } finally {
            commonEnd();
        }
    }

    @Test
    public void testCorrectRemoveExternalNodeSave() throws RepositoryException{
            commonStart();
            //external node indicates for the half regular half virtual nodes
            Node externalNode = session.getRootNode().getNode("test/navigation").getNode("xyz");
            traverse(externalNode);
            session.getRootNode().getNode("test/documents").addNode("test");
            session.save();
            externalNode = session.getRootNode().getNode("test/navigation").getNode("xyz");
            externalNode.remove();
            session.save();
    }

    @Test
    public void testWrongRemoveExternalNodeSave() throws RepositoryException {
        try {
            commonStart();
            //external node indicates for the half regular half virtual nodes
            Node externalNode = session.getRootNode().getNode("test/navigation").getNode("xyz");
            traverse(externalNode);
            session.getRootNode().getNode("test/documents").addNode("test");
            session.save();
            // without refetch gives correctly an exception!
            externalNode.remove();
            session.save();
            fail("An exception should have been thrown");
        } catch(NullPointerException ex) {
            fail(ex.getMessage());
        } catch(InvalidItemStateException ex) {
            assertNotNull("Exception allowed: item needs to be refetched", ex);
        } catch(RepositoryException ex) {
            assertNotNull("Exception allowed: item needs to be refetched", ex);
        } finally {
            commonEnd();
        }
    }

    @Test
    public void testCorrectRemoveVirtualNodeSave() throws RepositoryException {
        try {
            commonStart();
            //external node indicates for the half regular half virtual nodes
            Node virtualNode = session.getRootNode().getNode("test/navigation").getNode("xyz").getNode("x1");
            traverse(virtualNode);
            session.getRootNode().getNode("test/documents").addNode("test");
            session.save();
            virtualNode = session.getRootNode().getNode("test/navigation").getNode("xyz").getNode("x1");
            virtualNode.remove();
            session.save();
        } catch(NullPointerException ex) {
            fail(ex.getMessage());
        } catch(RepositoryException ex) {
            fail(ex.getMessage());
        }  finally {
            commonEnd();
        }

    }

    @Test
    public void testWrongRemoveVirtualNodeSave() throws RepositoryException {
        try {
            commonStart();
            //external node indicates for the half regular half virtual nodes
            Node virtualNode = session.getRootNode().getNode("test/navigation").getNode("xyz").getNode("x1");
            traverse(virtualNode);
            session.getRootNode().getNode("test/documents").addNode("test");
            session.save();
            // without refetch gives correctly an exception!
            virtualNode.remove();
            session.save();
            fail("An exception should have been thrown");
        } catch(NullPointerException ex) {
            fail(ex.getMessage());
        } catch(InvalidItemStateException ex) {
            assertNotNull("Exception allowed: item needs to be refetched", ex);
        } catch(RepositoryException ex) {
            assertNotNull("Exception allowed: item needs to be refetched", ex);
        } finally {
            commonEnd();
        }
    }
}
