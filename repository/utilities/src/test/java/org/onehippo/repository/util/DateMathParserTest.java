/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.util;

import java.util.Calendar;
import java.util.Date;
import java.text.ParseException;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DateMathParserTest  {

    private static Calendar getNow() {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(new Date());
        return startCal;
    }

    @Test
    public void testParseMathAddDays() throws ParseException {
        try {
            Calendar now = getNow();
            Date startDate = now.getTime();
            Date endDate = DateMathParser.parseMath(now, "+7D").getTime();
            assertNotNull(endDate);
            long startTime = startDate.getTime();
            long endTime = endDate.getTime();
            long diffTime = endTime - startTime;
            long diff = diffTime / (1000 * 60 * 60 * 24);
            assertTrue(diff == 7);
        }
        catch (IllegalStateException ex) {
            fail();
        }
    }
    
    @Test
    public void testParseMathSubtractDays() throws ParseException {
        try {
            Calendar now = getNow();
            Date startDate = now.getTime();
            Date endDate = DateMathParser.parseMath(now, "-7D").getTime();
            assertNotNull(endDate);
            long startTime = startDate.getTime();
            long endTime = endDate.getTime();
            long diff = (startTime / (1000 * 60 * 60 * 24)) - (endTime / (1000 * 60 * 60 * 24));
            assertTrue(diff == 7);
        }
        catch (IllegalStateException ex) {
            fail();
        }
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
        }
        catch (IllegalStateException ex) {
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
            
        }
        catch (IllegalStateException ex) {
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
        }
        catch (IllegalStateException ex) {
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
        }
        catch (IllegalStateException ex) {
            fail();
        }
    }
}
