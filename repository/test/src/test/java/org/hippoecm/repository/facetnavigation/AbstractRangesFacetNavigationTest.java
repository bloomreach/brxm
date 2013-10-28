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

import org.hippoecm.repository.util.DateTools;
import org.onehippo.repository.testutils.RepositoryTestCase;

public abstract class AbstractRangesFacetNavigationTest extends RepositoryTestCase {
   
    /*
     * as we are having expectations about dates, our expectations might be wrong during the 'switch' of a day. Hence, this method returns
     * true when we should skip the date tests
     */
    protected boolean testShouldSkip() {
        
       /*
        * This test should not run just around the swapping of day (so, at 23:59 hours), as in this case, the expectations below with 
        * runtime calendar based ranges might return different values then expected in the unit test: this is not wrong, but we cannot anticipate 
        * this in the tests. Hence, when there are less then 5 minutes left in the current day, we skip this test 
        */
       Calendar noon = Calendar.getInstance();
       // get next day
       noon.add(Calendar.DAY_OF_YEAR, 1);
       //
       long timeMillisSecAtNoon = DateTools.round(noon.getTimeInMillis(), DateTools.Resolution.DAY);
       noon.setTimeInMillis(timeMillisSecAtNoon);

       // 300.000 millesec = 5 min
       if ((timeMillisSecAtNoon - start.getTimeInMillis()) < 300 * 1000) {
           // we do not run this unit test, as around the swapping of a day, there might be unexpected results, as during the test, the 
           // calendar may change to the next day, leading to wrong expectations
           return true;
       }
        return false;
    }
    
    protected static final Calendar start = Calendar.getInstance();
    protected static final Calendar onehourbefore = Calendar.getInstance();
    static {
        onehourbefore.setTimeInMillis(start.getTimeInMillis());
        onehourbefore.add(Calendar.HOUR, -1);
    }
    protected static final Calendar onedaybefore = Calendar.getInstance();
    static {
        onedaybefore.setTimeInMillis(start.getTimeInMillis());
        onedaybefore.add(Calendar.DAY_OF_YEAR, -1);
    }
    protected static final Calendar threedaybefore = Calendar.getInstance();
    static {
        threedaybefore.setTimeInMillis(start.getTimeInMillis());
        threedaybefore.add(Calendar.DAY_OF_YEAR, -3);
    }
    protected static final Calendar monthbefore = Calendar.getInstance();
    static {
        monthbefore.setTimeInMillis(start.getTimeInMillis());
        monthbefore.add(Calendar.MONTH, -1);
    }
    protected static final Calendar monthandadaybefore = Calendar.getInstance();
    static {
        monthandadaybefore.setTimeInMillis(start.getTimeInMillis());
        monthandadaybefore.add(Calendar.MONTH, -1);
        monthandadaybefore.add(Calendar.DAY_OF_YEAR, -1);
    }
    protected static final Calendar twomonthsbefore = Calendar.getInstance();
    static {
        twomonthsbefore.setTimeInMillis(start.getTimeInMillis());
        twomonthsbefore.add(Calendar.MONTH, -2);
    }
    protected static final Calendar yearbefore = Calendar.getInstance();
    static {
        yearbefore.setTimeInMillis(start.getTimeInMillis());
        yearbefore.add(Calendar.YEAR, -1);
    }

    protected static final Calendar twoyearbefore = Calendar.getInstance();
    static {
        twoyearbefore.setTimeInMillis(start.getTimeInMillis());
        twoyearbefore.add(Calendar.YEAR, -2);
    }

    protected void commonStart() throws RepositoryException {
        session.getRootNode().addNode("test");
        session.save();
    }

    protected void createDateStructure(Node test, boolean populateCars) throws RepositoryException {
        Node documents = test.addNode("documents", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node dateDocs = documents.addNode("datedocs", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        if(populateCars) {
            addCar(dateDocs, "start", start, 125000L, 18000.0D, "peugeot");
            addCar(dateDocs, "onehourbefore", onehourbefore, 112000L, 12125.0D, "peugeot");
            addCar(dateDocs, "onedaybefore", onedaybefore, 92000L, 9156.0D, "audi");
            addCar(dateDocs, "threedaybefore", threedaybefore, 63000L, 22345.0D, "mercedes");
            addCar(dateDocs, "monthbefore", monthbefore, 119000L, 13456.0D, "toyota");
            addCar(dateDocs, "monthandadaybefore", monthandadaybefore, 134000L, 6787.0D, "audi");
            addCar(dateDocs, "twomonthsbefore", twomonthsbefore, 232000L, 4125.0D, "alfa romeo");
            addCar(dateDocs, "yearbefore", yearbefore, 12200L, 52125.0D, "bmw");
            addCar(dateDocs, "twoyearbefore", twoyearbefore, 152000L, 1225.0D, "bentley");
        }
        test.save();
    }
    
    protected void createDateStructure(Node test) throws RepositoryException {
        this.createDateStructure(test, true);
    }

    protected void addCar(Node dateDocs, String name, Calendar cal, long travelled, double price, String brand)
            throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException,
            VersionException, ConstraintViolationException, RepositoryException {
        Node carDoc = dateDocs.addNode(name, "hippo:handle");
        carDoc.addMixin("hippo:hardhandle");
        carDoc = carDoc.addNode(name, "hippo:testcardocument");
        carDoc.addMixin("mix:versionable");
        carDoc.setProperty("hippo:date", cal);
        carDoc.setProperty("hippo:travelled", travelled);
        carDoc.setProperty("hippo:price", price);
        carDoc.setProperty("hippo:brand", brand);
    }
}
