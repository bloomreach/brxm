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

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FreeTextSearchTest extends RepositoryTestCase {
    private Calendar start = Calendar.getInstance();
    private Calendar onehourearlier = Calendar.getInstance();
    private Calendar onedayearlier = Calendar.getInstance();
    private Calendar threedayearlier = Calendar.getInstance();
    private Calendar monthearlier = Calendar.getInstance();
    private Calendar monthandadayearlier = Calendar.getInstance();
    private Calendar twomonthsearlier = Calendar.getInstance();
    private Calendar yearearlier = Calendar.getInstance();
    private Calendar twoyearearlier = Calendar.getInstance();
    //private Node facetNavigation;
    private int currentYear;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        start.set(2009, 11, 23, 10, 46);
        onehourearlier.set(2009, 11, 23, 9, 46);
        onedayearlier.set(2009, 11, 22, 10, 46);
        threedayearlier.set(2009, 11, 20, 10, 46);
        monthearlier.set(2009, 10, 23, 10, 46);
        monthandadayearlier.set(2009, 10, 22, 10, 46);
        twomonthsearlier.set(2009, 9, 23, 10, 46);
        yearearlier.set(2008, 11, 23, 10, 46);
        twoyearearlier.set(2007, 11, 23, 10, 46);
        currentYear = start.get(Calendar.YEAR);

        if (!session.getRootNode().hasNode("test")) {
            session.getRootNode().addNode("test");
        }
        Node testNode = session.getRootNode().getNode("test");
        Node documents = testNode.addNode("documents", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node carDocs = documents.addNode("cardocs", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        addCarDoc(carDocs, "cardoc1", start, "the quick brown fox jumps over the lazy dog", "peugeot", "red");
        addCarDoc(carDocs, "cardoc2", onehourearlier, "brown fox jumps over the lazy dog", "peugeot", "green");
        addCarDoc(carDocs, "cardoc3", onedayearlier, "jumps over the lazy dog", "peugeot", "yellow");
        addCarDoc(carDocs, "cardoc4", threedayearlier, "lazy dog", "peugeot", "red");
        addCarDoc(carDocs, "cardoc5", monthearlier, "just some really other text about the laziest dog ever", "peugeot", "red");
        addCarDoc(carDocs, "cardoc6", monthandadayearlier, null, "bmw", "green");
        addCarDoc(carDocs, "cardoc7", twomonthsearlier, null, "mercedes", "red");
        addCarDoc(carDocs, "cardoc8", yearearlier, null, "mercedes", "green");
        addCarDoc(carDocs, "cardoc9", twoyearearlier, null, "bmw", "red");
        session.save();

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] {"hippo:date$year", "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year", "month"});
        session.save();

        int currentYear = start.get(Calendar.YEAR);
        //facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        session.refresh(false);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void addCarDoc(Node carDocs, String name, Calendar cal, String contents, String brand, String color) throws RepositoryException {
        Node carDoc = carDocs.addNode(name, "hippo:handle");
        carDoc.addMixin("hippo:hardhandle");
        carDoc = carDoc.addNode(name, "hippo:testcardocument");
        carDoc.addMixin("mix:versionable");
        carDoc.setProperty("hippo:date", cal);
        carDoc.setProperty("hippo:brand", brand);
        carDoc.setProperty("hippo:color", color);
        if (contents != null) {
            Node contentNode = carDoc.addNode("contents", "hippo:ntunstructured");
            contentNode.setProperty("content", contents);
        }
    }

    @Test
    public void testSanityCheck() throws RepositoryException {
        Node facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)));
        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT).getLong() == 7L);
        session.refresh(false);
    }

    @Test
    public void testDirectPath() throws RepositoryException {
        verify(session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{jumps}]"));
    }

    @Test
    public void testIteratedPath() throws RepositoryException {
        Node n = session.getRootNode();
        n = n.getNode("test/facetnavigation");
        n = n.getNode("hippo:navigation[{jumps}]");
        verify(n);
    }

    @Test
    public void testDeepDirectPath() throws RepositoryException {
        verify(session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{jumps}]/year").getParent());
    }

    @Test
    public void testDirectAccess() throws RepositoryException {
        verify((Node)session.getItem("/test/facetnavigation/hippo:navigation[{jumps}]"));
    }

    @Test
    public void testDeepDirectAccess() throws RepositoryException {
        verify(session.getItem("/test/facetnavigation/hippo:navigation[{jumps}]/year").getParent());
    }

    private void verify(Node node) throws RepositoryException {
        assertNotNull(node.getNode("year"));
        assertNotNull(node.getNode("year").getNode(String.valueOf(currentYear)));
        assertEquals(3L, node.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT).getLong());
    }
}
