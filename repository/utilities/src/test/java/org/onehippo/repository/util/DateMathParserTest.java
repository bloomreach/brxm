/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.util;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class DateMathParserTest {

    private static final Logger log = LoggerFactory.getLogger(DateMathParserTest.class);


    private static Calendar getNow() {
        return Calendar.getInstance();
    }

    @Test
    public void testParseMathAddDays() throws ParseException {
        assertExpectedDuration(Calendar.getInstance(), 7, "+7D");
    }

    /**
     * Test the DateMathParser around daylight saving time in CET timezone
     *
     * @throws ParseException
     */
    @Test
    public void testParseMathDLS() throws ParseException {
        // March 25th 2017 Day Light saving time has started
        // October 29th  2017 Day Light saving time has started
        final GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("CET"));
        cal.set(2017, Calendar.MARCH, 30);
        assertExpectedDuration(cal, -7, "-7D");
        cal.set(2017, Calendar.MARCH, 30);
        assertExpectedDuration(cal, +7, "+7D");
        cal.set(2017, Calendar.MARCH, 30);
        assertExpectedDuration(cal, +7, "+7D");
        cal.set(2017, Calendar.MARCH, 30);
        assertExpectedDuration(cal, -7, "-7D");
        cal.set(2017, Calendar.MARCH, 30);
    }

    @Test
    public void testParseMathSubtractDays() throws ParseException {
        assertExpectedDuration(Calendar.getInstance(), -7, "-7D");
    }

    private void assertExpectedDuration(final Calendar startCalendar, int expectedDuration, String math) throws ParseException {
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy hh:mm");
        final Date startDate = startCalendar.getTime();
        log.debug("startDate:{}", startDate);
        final Calendar endCalendar = (Calendar) startCalendar.clone();
        endCalendar.add(Calendar.DAY_OF_MONTH, expectedDuration);
        final int startTimeOffset = getOffset(startCalendar);
        log.debug("startTimeOffset[h]:{}", startTimeOffset / (60 * 60 * 1000));
        final int endTimeOffset = getOffset(endCalendar);
        log.debug("endTimeOffset[h]:{}", startTimeOffset / (60 * 60 * 1000));
        log.debug("expected endDate:{}", sdf.format(endCalendar.getTime()));
        final Date endDate = DateMathParser.parseMath(startCalendar, math).getTime();
        log.debug("actual endDate:{}", sdf.format(endDate));
        assertNotNull(endDate);
        final long startTimeInMillis = startDate.getTime();
        final long endTimeInMillis = endDate.getTime();
        final long durationInMillis = endTimeInMillis - startTimeInMillis;
        final long utcStartTimeInMillis = startTimeInMillis - startTimeOffset;
        log.debug("utc startDate:{}", new Date(utcStartTimeInMillis));
        final long utcEndTimeInMillis = startTimeInMillis + (expectedDuration * 24 * 60 * 60 * 1000) - endTimeOffset;
        log.debug("utc endDate:{}", new Date(utcEndTimeInMillis));
        final long utcDurationInMillis = utcEndTimeInMillis - utcStartTimeInMillis;


        if (startTimeOffset != endTimeOffset) {
            log.debug("{} and {} have a diffent timezones offset", sdf.format(startDate), sdf.format(endDate));
        }

        final String expected = DurationFormatUtils.formatDurationISO(utcDurationInMillis);
        final String actual = DurationFormatUtils.formatDurationISO(durationInMillis);
        assertEquals(expected, actual);

        log.debug("Expected duration:{}, actual duration:{}", expected, actual);
    }


    private int getOffset(Calendar cal) {
        return cal.getTimeZone().getOffset(cal.getTimeInMillis());
    }

    @Test
    public void testParseMathAddYears() throws ParseException {
        try {
            Calendar now = getNow();
            Date startDate = now.getTime();
            Date endDate = DateMathParser.parseMath(now, "+7Y").getTime();
            assertNotNull(endDate);
            String startPeriod = new java.text.SimpleDateFormat("yyyy").format(startDate);
            String endPeriod = new java.text.SimpleDateFormat("yyyy").format(endDate);
            long diff = Integer.valueOf(endPeriod) - Integer.valueOf(startPeriod);
            assertTrue(diff == 7);
        } catch (IllegalStateException ex) {
            fail();
        }
    }

    @Test
    public void testParseMathRoundToStartOfDay() throws ParseException {
        try {
            Calendar now = getNow();
            Date startDate = now.getTime();
            Date endDate = DateMathParser.parseMath(now, "/D").getTime();
            assertNotNull(endDate);
            String startPeriod = new java.text.SimpleDateFormat("yyyy-MM-dd").format(startDate);
            String modifiedPeriod = startPeriod + "T00:00:00.000";
            String endPeriod = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(endDate);
            assertTrue(endPeriod.equals(modifiedPeriod));

        } catch (IllegalStateException ex) {
            fail();
        }
    }

    @Test
    public void testParseMathMultipleFunctions() throws ParseException {
        try {
            Calendar now = getNow();
            Date startDate = now.getTime();
            Date endDate = DateMathParser.parseMath(now, "+5Y-60M").getTime();
            assertNotNull(endDate);
            long startTime = startDate.getTime();
            long endTime = endDate.getTime();
            assertTrue((startTime == endTime));
        } catch (IllegalStateException ex) {
            fail();
        }
    }

    @Test
    public void testParseMathMultipleFunctionsWithRounding() throws ParseException {
        try {
            Calendar now = getNow();
            Date startDate = now.getTime();
            Date endDate = DateMathParser.parseMath(now, "-5Y+24M/Y").getTime();
            assertNotNull(endDate);
            String startPeriod = new java.text.SimpleDateFormat("yyyy").format(startDate);
            int newYear = Integer.valueOf(startPeriod) - 3;
            String modifiedPeriod = Integer.toString(newYear) + "-01-01T00:00:00.000";
            String endPeriod = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(endDate);
            assertTrue(endPeriod.equals(modifiedPeriod));
        } catch (IllegalStateException ex) {
            fail();
        }
    }
}
