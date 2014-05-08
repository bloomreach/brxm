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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    private void createDocumentsWithUniqueLastModified(final int amount, final Calendar calSeed, final int timeUnit) throws Exception {
        for (int i = 0 ; i < amount; i++) {
            Calendar nextCal =  ((Calendar)calSeed.clone());
            nextCal.add(timeUnit, i);
            Node handle = testPath.addNode("Document"+i, HippoNodeType.NT_HANDLE);
            Node document = handle.addNode("Document"+i, NT_SEARCHDOCUMENT);
            document.setProperty("hippo:lastModified", nextCal);
        }
        session.save();
    }

    @Test
    public void testSupportedResolutions() throws Exception {
        final int nrOfDocuments = 100;
        final Calendar calStart = Calendar.getInstance();

        createDocumentsWithUniqueLastModified(nrOfDocuments, calStart, Calendar.SECOND);

        String dateRangeNoResolutionStart = DateTools.createXPathConstraint(session, calStart);
        String dateRangeNoResolutionEnd = DateTools.createXPathConstraint(session, calStart);

        final long limit = 1L;
        String xpathNoResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@hippo:lastModified >= " + dateRangeNoResolutionStart +
                " and @hippo:lastModified <= "+dateRangeNoResolutionEnd+"] order by @hippo:lastModified ascending";
        final Query withNoResolution = session.getWorkspace().getQueryManager().createQuery(xpathNoResolution, "xpath");
        withNoResolution.setLimit(limit);
        final Node firstResultNoResolution = withNoResolution.execute().getNodes().nextNode();

        for (DateTools.Resolution resolution : DateTools.getSupportedResolutions()){
            String dateResolutionStart = DateTools.createXPathConstraint(session, calStart, resolution);
            String dateResolutionEnd = DateTools.createXPathConstraint(session, calStart, resolution);
            String lastModifiedPropertyForResolution = DateTools.getPropertyForResolution("hippo:lastModified", resolution);

            String xpathResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@"+lastModifiedPropertyForResolution+" >= " + dateResolutionStart +
                    " and @"+lastModifiedPropertyForResolution+" <= "+dateResolutionEnd+"] order by @hippo:lastModified ascending";
            final Query withResolution = session.getWorkspace().getQueryManager().createQuery(xpathResolution, "xpath");
            withResolution.setLimit(limit);
            final Node firstResultWithResolution = withResolution.execute().getNodes().nextNode();
            assertTrue("First result for range query with resolution should be same as without", firstResultNoResolution.isSame(firstResultWithResolution));
        }

    }

    @Test
    public void testDayRangeResolutions() throws Exception {
        final int nrDocsToday = 25;
        final int nrDocsToMorrow = 50;
        final Calendar calToday = Calendar.getInstance();
        // make sure start date can never be around noon to avoid race conditions in our expectations
        calToday.set(Calendar.HOUR, 8);
        createDocumentsWithUniqueLastModified(nrDocsToday, calToday, Calendar.SECOND);

        final Calendar calTomorrow = (Calendar)calToday.clone();
        calTomorrow.add(Calendar.DAY_OF_YEAR,1);
        createDocumentsWithUniqueLastModified(nrDocsToMorrow, calTomorrow, Calendar.SECOND);

        // same calendar for start and end
        String dateRangeDayResolutionStart = DateTools.createXPathConstraint(session, calToday, DateTools.Resolution.DAY);
        String dateRangeDayResolutionEnd = DateTools.createXPathConstraint(session, calToday, DateTools.Resolution.DAY);
        String lastModifiedPropertyForDay = DateTools.getPropertyForResolution("hippo:lastModified", DateTools.Resolution.DAY);

        String xpathDayResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@"+lastModifiedPropertyForDay+" >= " + dateRangeDayResolutionStart +
                " and @"+lastModifiedPropertyForDay+" <= "+dateRangeDayResolutionEnd+"] order by @hippo:lastModified ascending";

        final Query queryWithDayToDayResolution = session.getWorkspace().getQueryManager().createQuery(xpathDayResolution, "xpath");

        // ALL todays docs!!
        assertTrue(queryWithDayToDayResolution.execute().getNodes().getSize() == 25L);

        // now test with original range query without resolution DAY. There is only 1 document with exactly calToday as date
        String dateRangeNoResolutionStart = DateTools.createXPathConstraint(session, calToday);
        String dateRangeNoResolutionEnd = DateTools.createXPathConstraint(session, calToday);
        String xpathNoResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@hippo:lastModified >= " + dateRangeNoResolutionStart +
                " and @hippo:lastModified <= "+dateRangeNoResolutionEnd+"] order by @hippo:lastModified ascending";

        final Query queryWithDayResolution = session.getWorkspace().getQueryManager().createQuery(xpathNoResolution, "xpath");

        assertTrue("For original range query without resolution, there should be only 1 date with the exact 'calToday' as date",
                queryWithDayResolution.execute().getNodes().getSize() == 1L);
    }

    @Test
    public void testEqualsWithResolutions() throws Exception {
        final int nrDocsToday = 25;
        final Calendar calToday = Calendar.getInstance();
        // make sure start date can never be around noon to avoid race conditions in our expectations
        calToday.set(Calendar.HOUR, 8);
        createDocumentsWithUniqueLastModified(nrDocsToday, calToday, Calendar.SECOND);

        int attempts = 0;
        boolean success = false;
        AssertionError error = null;
        // multiple attempts because search results are not always immediately available after save (see REPO-878)
        while (!success && attempts < 10) {
            try {
                for (DateTools.Resolution resolution : DateTools.getSupportedResolutions()) {
                    String dateWithResolution = DateTools.createXPathConstraint(session, calToday, resolution);
                    String lastModifiedPropertyForResolution = DateTools.getPropertyForResolution("hippo:lastModified", resolution);
                    String xpathWithResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@"+lastModifiedPropertyForResolution+" = " + dateWithResolution +"] order by @hippo:lastModified ascending";
                    final Query queryWithResolution = session.getWorkspace().getQueryManager().createQuery(xpathWithResolution, "xpath");

                    // ALL docs with equals!!
                    assertEquals("Resolution" + resolution + " failed", 25L, queryWithResolution.execute().getNodes().getSize());
                    success = true;
                }
            } catch (AssertionError e) {
                error = e;
                attempts++;
                Thread.sleep(100);
            }
        }
        if (!success) {
            throw error;
        }
        if (attempts > 0) {
            log.debug("Needed " + attempts + " attempts, " + attempts*100 + " ms. to pass test");
        }
    }

    @Test
    public void testUnSupportedResolutions() throws Exception {
        final int nrOfDocuments = 100;
        final Calendar calStart = Calendar.getInstance();
        // make sure start date can never be around noon to avoid race conditions in our expectations
        calStart.set(Calendar.HOUR, 8);
        createDocumentsWithUniqueLastModified(nrOfDocuments, calStart, Calendar.SECOND);

        final long limit = 1L;
        DateTools.Resolution[] unSupportedResolutions = {
                DateTools.Resolution.WEEK,
                DateTools.Resolution.MINUTE,
                DateTools.Resolution.SECOND,
                DateTools.Resolution.MILLISECOND
        };

        for (DateTools.Resolution unsupportedResolution : unSupportedResolutions){
            String dateResolutionStart = DateTools.createXPathConstraint(session, calStart, unsupportedResolution);
            String dateResolutionEnd = DateTools.createXPathConstraint(session, calStart, unsupportedResolution);
            String lastModifiedPropertyForResolution = DateTools.getPropertyForResolution("hippo:lastModified", unsupportedResolution);

            String xpathResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@"+lastModifiedPropertyForResolution+" >= " + dateResolutionStart +
                    " and @"+lastModifiedPropertyForResolution+" <= "+dateResolutionEnd+"] order by @hippo:lastModified ascending";
            final Query withResolution = session.getWorkspace().getQueryManager().createQuery(xpathResolution, "xpath");
            withResolution.setLimit(limit);
            final QueryResult result = withResolution.execute();
            assertTrue(result.getNodes().getSize() == 0);
        }
    }


    @Test
    @Ignore
    public void testDateRangePerformanceAndSorting() throws Exception {

        // create some not too small number of docs with unique date to prove performance for DAY resolution is better
        final int nrOfDocuments = 1000;
        final Calendar calStart = Calendar.getInstance();
        // make sure start date can never be around noon to avoid race conditions in our expectations
        calStart.set(Calendar.HOUR, 8);

        createDocumentsWithUniqueLastModified(nrOfDocuments, calStart, Calendar.SECOND);

        String dateRangeDayStart = DateTools.createXPathConstraint(session, calStart, DateTools.Resolution.DAY);
        String dateRangeDayEnd = DateTools.createXPathConstraint(session, calStart, DateTools.Resolution.DAY);
        String lastModifiedPropertyForDay = DateTools.getPropertyForResolution("hippo:lastModified", DateTools.Resolution.DAY);

        String xpathDayResolution = "//element(*,"+NT_SEARCHDOCUMENT+")[@"+lastModifiedPropertyForDay+" >= " + dateRangeDayStart +
                " and @"+lastModifiedPropertyForDay+" <= "+dateRangeDayEnd+"] order by @hippo:lastModified ascending";

        String dateRangeNoResolutionStart = DateTools.createXPathConstraint(session, calStart);
        // for query with no resolution, the end date must be bigger that start date
        Calendar calEnd = (Calendar)calStart.clone();
        calEnd.add(Calendar.HOUR, 1);
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

        long slowQueriesTook = (System.currentTimeMillis() - slowStart);

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