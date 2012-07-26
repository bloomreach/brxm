/*
 *  Copyright 2010 Hippo.
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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.query.lucene.HippoDateTools;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractDateFacetNavigationTest extends TestCase {
   

    static final Calendar start = Calendar.getInstance();
    static {
        start.set(2009, 11, 23, 10, 46);
    }
    static final Calendar onehourearlier = Calendar.getInstance();
    static {
        onehourearlier.set(2009, 11, 23, 9, 46);
    }
    static final Calendar onedayearlier = Calendar.getInstance();
    static {
        onedayearlier.set(2009, 11, 22, 10, 46);
    }
    static final Calendar threedayearlier = Calendar.getInstance();
    static {
        threedayearlier.set(2009, 11, 20, 10, 46);
    }
    static final Calendar monthearlier = Calendar.getInstance();
    static {
        monthearlier.set(2009, 10, 23, 10, 46);
    }
    static final Calendar monthandadayearlier = Calendar.getInstance();
    static {
        monthandadayearlier.set(2009, 10, 22, 10, 46);
    }
    static final Calendar twomonthsearlier = Calendar.getInstance();
    static {
        twomonthsearlier.set(2009, 9, 23, 10, 46);
    }
    static final Calendar yearearlier = Calendar.getInstance();
    static {
        yearearlier.set(2008, 11, 23, 10, 46);
    }

    static final Calendar twoyearearlier = Calendar.getInstance();
    static {
        twoyearearlier.set(2007, 11, 23, 10, 46);
    }

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
    

    void commonStart() throws RepositoryException {
        session.getRootNode().addNode("test");
        session.save();
    }
    
    void createDateStructure(Node test, boolean populateCars) throws RepositoryException {
        Node documents = test.addNode("documents", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node carDocs = documents.addNode("cardocs", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        if(populateCars) {
            addCarDoc(carDocs, "cardoc1", start, "the quick brown fox jumps over the lazy dog", "peugeot", "red");
            addCarDoc(carDocs, "cardoc2", onehourearlier, "brown fox jumps over the lazy dog", "peugeot", "green");
            addCarDoc(carDocs, "cardoc3", onedayearlier, "jumps over the lazy dog", "peugeot", "yellow");
            addCarDoc(carDocs, "cardoc4", threedayearlier, "lazy dog", "peugeot", "red");
            addCarDoc(carDocs, "cardoc5", monthearlier, "just some really other text about the laziest dog ever", "peugeot", "red");
            addCarDoc(carDocs, "cardoc6", monthandadayearlier, null, "bmw", "green");
            addCarDoc(carDocs, "cardoc7", twomonthsearlier, null, "mercedes", "red");
            addCarDoc(carDocs, "cardoc8", yearearlier, null, "mercedes", "green");
            addCarDoc(carDocs, "cardoc9", twoyearearlier, null, "bmw", "red");
        }
        test.save();
    }
    void createDateStructure(Node test) throws RepositoryException {
       this.createDateStructure(test,true);
    }

    void addCarDoc(Node carDocs, String name, Calendar cal, String contents, String brand, String color) throws ItemExistsException,
            PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException,
            ConstraintViolationException, RepositoryException {
        Node carDoc = carDocs.addNode(name, "hippo:handle");
        carDoc.addMixin("hippo:hardhandle");
        carDoc = carDoc.addNode(name, "hippo:testcardocument");
        carDoc.addMixin("hippo:harddocument");
        carDoc.setProperty("hippo:date", cal);
        carDoc.setProperty("hippo:brand", brand);
        carDoc.setProperty("hippo:color", color);
        if(contents != null) {
            Node contentNode = carDoc.addNode("contents", "hippo:ntunstructured");
            contentNode.setProperty("content", contents);
        }

    }

}
