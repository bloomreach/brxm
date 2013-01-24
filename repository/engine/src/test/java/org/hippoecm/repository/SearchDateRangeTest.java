/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.DateTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;

public class SearchDateRangeTest extends RepositoryTestCase {
    public static final String NT_SEARCHDOCUMENT = "hippo:testsearchdocument";

    private static final String TEST_PATH = "test";
    private Node testPath;


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode(TEST_PATH)) {
            session.getRootNode().getNode(TEST_PATH).remove();
        }
        testPath = session.getRootNode().addNode(TEST_PATH);
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void createDocumentsWithUniqueLastModified(final int amount, final Calendar calSeed, final int timeUnit) throws Exception {
        for (int i = 0 ; i < amount; i++) {
            Calendar nextCal =  ((Calendar)calSeed.clone());
            nextCal.add(timeUnit, i);
            Node handle = testPath.addNode("Document"+i, HippoNodeType.NT_HANDLE);
            Node document = handle.addNode("Document"+i, NT_SEARCHDOCUMENT);
            document.setProperty("hippo:lastModified", nextCal);
        }
        testPath.getSession().save();
    }

    @Test
    public void testSupportedResolutions() throws Exception {
        final int nrOfDocuments = 100;
        final Calendar calStart = Calendar.getInstance();
        // make sure start date can never be around noon to avoid race conditions in our expectations
        calStart.set(Calendar.HOUR, 8);
        final Calendar calEnd = (Calendar)calStart.clone();
        // end date 1 year later because we want range on resolution YEAR as well
        calEnd.add(Calendar.YEAR, 1);
        createDocumentsWithUniqueLastModified(nrOfDocuments, calStart, Calendar.SECOND);

        String dateRangeNoResolutionStart = DateTools.createXPathConstraint(session, calStart);
        String dateRangeNoResolutionEnd = DateTools.createXPathConstraint(session, calEnd);

        final long limit = 1L;
        String xpathNoResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@hippo:lastModified >= " + dateRangeNoResolutionStart +
                " and @hippo:lastModified <= "+dateRangeNoResolutionEnd+"] order by @hippo:lastModified ascending";
        final Query withNoResolution = session.getWorkspace().getQueryManager().createQuery(xpathNoResolution, "xpath");
        withNoResolution.setLimit(limit);
        final Node firstResultNoResulution = withNoResolution.execute().getNodes().nextNode();

        DateTools.Resolution[] supportedResolutions = {DateTools.Resolution.YEAR,
                DateTools.Resolution.MONTH,
                DateTools.Resolution.WEEK,
                DateTools.Resolution.DAY,
                DateTools.Resolution.HOUR};

        for (DateTools.Resolution resolution : supportedResolutions){
            String dateResolutationStart = DateTools.createXPathConstraint(session, calStart, resolution);
            String dateResolutationEnd = DateTools.createXPathConstraint(session, calEnd, resolution);
            String lastModifiedPropertyForResolution = DateTools.getPropertyForResolution("hippo:lastModified", resolution);

            String xpathResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@"+lastModifiedPropertyForResolution+" >= " + dateResolutationStart +
                    " and @"+lastModifiedPropertyForResolution+" <= "+dateResolutationEnd+"] order by @hippo:lastModified ascending";
            final Query withResolution = session.getWorkspace().getQueryManager().createQuery(xpathResolution, "xpath");
            withResolution.setLimit(limit);
            final Node firstResultWithResulution = withResolution.execute().getNodes().nextNode();
            assertTrue("First result for range query with resolution should be same as without", firstResultNoResulution.isSame(firstResultWithResulution));
        }

    }

    @Test
    public void testUnSupportedResolutions() throws Exception {
        final int nrOfDocuments = 100;
        final Calendar calStart = Calendar.getInstance();
        // make sure start date can never be around noon to avoid race conditions in our expectations
        calStart.set(Calendar.HOUR, 8);
        final Calendar calEnd = (Calendar)calStart.clone();
        // end date 1 year later because we want range on resolution YEAR as well
        calEnd.add(Calendar.YEAR, 1);
        createDocumentsWithUniqueLastModified(nrOfDocuments, calStart, Calendar.SECOND);

        final long limit = 1L;
        DateTools.Resolution[] unSupportedResolutions = {DateTools.Resolution.MINUTE,
                DateTools.Resolution.SECOND,
                DateTools.Resolution.MILLISECOND};

        for (DateTools.Resolution unsupportedResolution : unSupportedResolutions){
            String dateResolutationStart = DateTools.createXPathConstraint(session, calStart, unsupportedResolution);
            String dateResolutationEnd = DateTools.createXPathConstraint(session, calEnd, unsupportedResolution);
            String lastModifiedPropertyForResolution = DateTools.getPropertyForResolution("hippo:lastModified", unsupportedResolution);

            String xpathResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@"+lastModifiedPropertyForResolution+" >= " + dateResolutationStart +
                    " and @"+lastModifiedPropertyForResolution+" <= "+dateResolutationEnd+"] order by @hippo:lastModified ascending";
            final Query withResolution = session.getWorkspace().getQueryManager().createQuery(xpathResolution, "xpath");
            withResolution.setLimit(limit);
            final QueryResult result = withResolution.execute();
            assertTrue(result.getNodes().getSize() == 0);
        }
    }


    @Test
    public void testDateRangePerformanceAndSorting() throws Exception {

        // create some not too small number of docs with unique date to proof performance for DAY resolution is better
        final int nrOfDocuments = 1000;
        final Calendar calStart = Calendar.getInstance();
        // make sure start date can never be around noon to avoid race conditions in our expectations
        calStart.set(Calendar.HOUR, 8);
        final Calendar calEnd = (Calendar)calStart.clone();
        // end date 1 day later because we want range on resolution DAY
        calEnd.add(Calendar.DAY_OF_YEAR, 1);
        long start = System.currentTimeMillis();
        createDocumentsWithUniqueLastModified(nrOfDocuments, calStart, Calendar.SECOND);

        String dateRangeDayStart = DateTools.createXPathConstraint(session, calStart, DateTools.Resolution.DAY);
        String dateRangeDayEnd = DateTools.createXPathConstraint(session, calEnd, DateTools.Resolution.DAY);
        String lastModifiedPropertyForDay = DateTools.getPropertyForResolution("hippo:lastModified", DateTools.Resolution.DAY);

        String xpathDayResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@"+lastModifiedPropertyForDay+" >= " + dateRangeDayStart +
                " and @"+lastModifiedPropertyForDay+" <= "+dateRangeDayEnd+"] order by @hippo:lastModified ascending";

        String dateRangeNoResolutionStart = DateTools.createXPathConstraint(session, calStart);
        String dateRangeNoResolutionEnd = DateTools.createXPathConstraint(session, calEnd);

        String xpathNoResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@hippo:lastModified >= " + dateRangeNoResolutionStart +
                " and @hippo:lastModified <= "+dateRangeNoResolutionEnd+"] order by @hippo:lastModified ascending";

        final long limit = 10L;

        final Query queryWithDayResolution = session.getWorkspace().getQueryManager().createQuery(xpathDayResolution, "xpath");
        queryWithDayResolution.setLimit(limit);
        final Query withNoResolution = session.getWorkspace().getQueryManager().createQuery(xpathNoResolution, "xpath");
        withNoResolution.setLimit(limit);
        for (int i = 0; i < 100; i++) {
            // WARM UP ROUNDS
            queryWithDayResolution.execute();
            withNoResolution.execute();
        }

        // Make sure the JVM is all ready for first test round
        System.gc();
        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);

        QueryResult fastQueryResultBecauseDayResolution = null;
        long fastStart = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            fastQueryResultBecauseDayResolution = queryWithDayResolution.execute();
        }
        long fastQueriesTook = (System.currentTimeMillis() - fastStart);


        // Make sure the JVM is all ready for second test round
        System.gc();
        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);
        QueryResult slowQueryResultBecauseNoResolution = null;
        long slowStart = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            slowQueryResultBecauseNoResolution = withNoResolution.execute();
        }

        long slowQueriesTook = (System.currentTimeMillis() - fastStart);

        System.out.println(slowQueriesTook);
        System.out.println(fastQueriesTook);


        assertTrue("date range query on the original date property should be slower than date range on Resolution DAY. ",
                slowQueriesTook > fastQueriesTook);

        NodeIterator slowQueryNodes = slowQueryResultBecauseNoResolution.getNodes();
        NodeIterator fastQueryNodes = fastQueryResultBecauseDayResolution.getNodes();

        assertTrue(slowQueryNodes.getSize() == limit);
        assertTrue(fastQueryNodes.getSize() == limit);
        // because sorting is for both done on @hippo:lastModified ascending result should be the same
        while (fastQueryNodes.hasNext()) {
            assertTrue(fastQueryNodes.nextNode().isSame(slowQueryNodes.nextNode()));
        }

        assertTrue(((HippoNodeIterator)fastQueryNodes).getTotalSize() == nrOfDocuments);
        assertTrue(((HippoNodeIterator)slowQueryNodes).getTotalSize() == nrOfDocuments);
    }


}