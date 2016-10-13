/*
 *  Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.query;


import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.repository.util.DateTools;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHstFilters extends AbstractBeanTestCase {

    @Test
    public void testEmptyFilter() throws Exception {
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        assertEquals(null,mainFilter.getJcrExpression());
    }

    @Test
    public void test_equal_to() throws Exception {
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        mainFilter.addEqualTo("@a", "a");
        assertEquals("@a = 'a'",mainFilter.getJcrExpression());

        mainFilter.addEqualTo("@b", "b");
        assertEquals("@a = 'a' and @b = 'b'",mainFilter.getJcrExpression());
    }

    @Test
    public void test_equal_to_case_insenstive() throws Exception {
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        mainFilter.addEqualToCaseInsensitive("@a", "a");
        assertEquals("@a = fn:lower-case('a')",mainFilter.getJcrExpression());

        mainFilter.addEqualToCaseInsensitive("@b", "b");
        assertEquals("@a = fn:lower-case('a') and @b = fn:lower-case('b')",mainFilter.getJcrExpression());
    }

    @Test
    public void test_equal_to_case_insensitive_value_to_lowercase() throws Exception {
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        mainFilter.addEqualToCaseInsensitive("@A", "aaAB");
        assertEquals("@A = fn:lower-case('aaab')",mainFilter.getJcrExpression());

        mainFilter.addEqualToCaseInsensitive("@B", "BB");
        assertEquals("@A = fn:lower-case('aaab') and @B = fn:lower-case('bb')",mainFilter.getJcrExpression());
    }
    
    @Test
    public void test_getStringValue_conversions() throws Exception {

        final FilterImpl filter = new FilterImpl(session, null);

        // String, Boolean
        assertEquals("'stringvalue'", filter.getStringValue("stringvalue"));
        assertEquals("'true'", filter.getStringValue(Boolean.TRUE));

        // various Numbers
        assertEquals("1", filter.getStringValue(1));
        assertEquals("2", filter.getStringValue(2L));
        assertEquals("3", filter.getStringValue(new BigInteger("3")));
        assertEquals("4.0", filter.getStringValue(4.0D));
        assertEquals("5.0", filter.getStringValue(5.0F));
        assertEquals("6.00", filter.getStringValue(new BigDecimal(6.0D).setScale(2, BigDecimal.ROUND_UNNECESSARY)));

        // Calendar/Date
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(1475155000000L);
        assertEquals("xs:dateTime('"+ISO8601.format(cal)+"')", filter.getStringValue(cal));
        assertEquals("xs:dateTime('" + ISO8601.format(cal) + "')", filter.getStringValue(cal.getTime()));
    }

    @Test(expected = FilterException.class)
    public void test_getStringValue_unsupported() throws Exception {

        final FilterImpl filter = new FilterImpl(session, null);
        assertEquals("exception", filter.getStringValue(Collections.EMPTY_LIST));
    }

    /**
     * Combine some AND-ed filters
     * @throws Exception
     */
    @Test
    public void testANDedChildFilters() throws Exception {
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        mainFilter.addContains(".", "contains");
        Filter subAnd1a = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subAnd1b = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subAnd1c = new FilterImpl(session, DateTools.Resolution.DAY);
        subAnd1a.addEqualTo("@a", "a");
        subAnd1b.addEqualTo("@b", "b");
        subAnd1c.addEqualToCaseInsensitive("@c", "c");
        mainFilter.addAndFilter(subAnd1a);
        mainFilter.addAndFilter(subAnd1b);
        mainFilter.addAndFilter(subAnd1c);
        assertEquals("jcr:contains(., 'contains') and (@a = 'a') and (@b = 'b') and (@c = fn:lower-case('c'))",mainFilter.getJcrExpression());
    }
    
    
    /**
     * Combine some AND-ed filters
     * @throws Exception
     */
    @Test
    public void testANDedChildFiltersWithEmptyParent() throws Exception {
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subAnd1a = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subAnd1b = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subAnd1c = new FilterImpl(session, DateTools.Resolution.DAY);
        subAnd1a.addEqualTo("@a", "a");
        subAnd1b.addEqualTo("@b", "b");
        subAnd1c.addEqualTo("@c", "c");
        mainFilter.addAndFilter(subAnd1a);
        mainFilter.addAndFilter(subAnd1b);
        mainFilter.addAndFilter(subAnd1c);
        assertEquals("(@a = 'a') and (@b = 'b') and (@c = 'c')",mainFilter.getJcrExpression());
    }
    
    /**
     * Combine some OR-ed filters
     * @throws Exception
     */
    @Test
    public void testORedChildFilters() throws Exception {
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        mainFilter.addContains(".", "contains");
        Filter subOr1a = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subOr1b = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subOr1c = new FilterImpl(session, DateTools.Resolution.DAY);
        subOr1a.addEqualTo("@a", "a");
        subOr1b.addEqualTo("@b", "b");
        subOr1c.addEqualTo("@c", "c");
        mainFilter.addOrFilter(subOr1a);
        mainFilter.addOrFilter(subOr1b);
        mainFilter.addOrFilter(subOr1c);
        assertEquals("jcr:contains(., 'contains') or (@a = 'a') or (@b = 'b') or (@c = 'c')",mainFilter.getJcrExpression());
    }
    
    /**
    * Combine some OR-ed filters
    * @throws Exception
    */
   @Test
   public void testORedChildFiltersWithEmptyParent() throws Exception {
       Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
       Filter subOr1a = new FilterImpl(session, DateTools.Resolution.DAY);
       Filter subOr1b = new FilterImpl(session, DateTools.Resolution.DAY);
       Filter subOr1c = new FilterImpl(session, DateTools.Resolution.DAY);
       subOr1a.addEqualTo("@a", "a");
       subOr1b.addEqualTo("@b", "b");
       subOr1c.addEqualTo("@c", "c");
       mainFilter.addOrFilter(subOr1a);
       mainFilter.addOrFilter(subOr1b);
       mainFilter.addOrFilter(subOr1c);
       assertEquals("(@a = 'a') or (@b = 'b') or (@c = 'c')",mainFilter.getJcrExpression());
   }
    
    /**
     * Combine some AND-ed filters
     * @throws Exception
     */
    @Test
    public void testNested_AND_OR_ChildFilters() throws Exception {
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subFilter1 = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter sub1a = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter sub1b = new FilterImpl(session, DateTools.Resolution.DAY);
        sub1a.addEqualTo("@a", "a");
        sub1b.addEqualTo("@b", "b");
        subFilter1.addAndFilter(sub1a);
        subFilter1.addAndFilter(sub1b);
        mainFilter.addAndFilter(subFilter1);
        assertEquals("((@a = 'a') and (@b = 'b'))",mainFilter.getJcrExpression());
        
        // add a constraint to subFilter1
        subFilter1.addEqualTo("@c", "c");
        assertEquals("(@c = 'c' and (@a = 'a') and (@b = 'b'))",mainFilter.getJcrExpression());
        
        // add another constraint to subFilter1@d = 'd'
        
        subFilter1.addEqualTo("@d", "d");
        //System.out.println(mainFilter.getJcrExpression());
        assertEquals("(@c = 'c' and @d = 'd' and (@a = 'a') and (@b = 'b'))",mainFilter.getJcrExpression());
        
        // add a constraint to mainFilter
        mainFilter.addEqualTo("@e", "e");
        assertEquals("@e = 'e' and (@c = 'c' and @d = 'd' and (@a = 'a') and (@b = 'b'))",mainFilter.getJcrExpression());
        
    }
    
   
}
