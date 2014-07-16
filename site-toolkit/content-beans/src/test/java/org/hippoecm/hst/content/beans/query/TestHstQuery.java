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
package org.hippoecm.hst.content.beans.query;


import javax.jcr.Node;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.repository.util.DateTools;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHstQuery  extends AbstractBeanTestCase {

    public final static String COMMON_QUERY_SCOPE_PART = "(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and not(@jcr:primaryType='nt:frozenNode')";
    public final static String COMMON_QUERY_ORDERBY_PART = " order by @jcr:score descending";
    

    private HstQueryManager queryMngr;
    @Before
    public void setUp() throws Exception {
        super.setUp();
        ObjectConverter objectConverter = getObjectConverter();
        queryMngr = new HstQueryManagerImpl(session,objectConverter, null);
    }

    @Test
    public void single_sub_type_query() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode(), "unittestproject:basedocument", true);
        assertEquals("//element(*,unittestproject:basedocument)[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') " +
                "and not(@jcr:primaryType='nt:frozenNode')] order by @jcr:score descending "
                , query.getQueryAsString(false));
    }

    @Test
    public void multiple_types_query_no_subtypes() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode(), "unittestproject:textpage", "unittestproject:newspage");
        HstQuery queryNoSubTypesExplicit = queryMngr.createQuery(session.getRootNode(), false, "unittestproject:textpage", "unittestproject:newspage");

        assertEquals("//*[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and not(@jcr:primaryType='nt:frozenNode') " +
                "and ((@jcr:primaryType='unittestproject:textpage' " +
                "or @jcr:primaryType='unittestproject:newspage'))] order by @jcr:score descending ",
                query.getQueryAsString(false));

        assertEquals(query.getQueryAsString(false), queryNoSubTypesExplicit.getQueryAsString(false));

    }

    @Test
    public void multiple_types_query_including_subtypes() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode(), true, "unittestproject:textpage", "unittestproject:newspage");
        assertEquals("//*[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and not(@jcr:primaryType='nt:frozenNode') " +
                "and ((@jcr:primaryType='unittestproject:textpage' or @jcr:primaryType='unittestproject:subtextpage' " +
                "or @jcr:primaryType='unittestproject:newspage'))] order by @jcr:score descending ",
                query.getQueryAsString(false));
    }

    @Test
    public void multiple_types_query_including_overlapping_subtypes() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode(), true, "unittestproject:textpage", "unittestproject:newspage",
                "unittestproject:basedocument");

        assertEquals("//*[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and not(@jcr:primaryType='nt:frozenNode') " +
                "and ((@jcr:primaryType='unittestproject:textpage' or @jcr:primaryType='unittestproject:subtextpage' " +
                "or @jcr:primaryType='unittestproject:newspage' or @jcr:primaryType='unittestproject:basedocument'))] " +
                "order by @jcr:score descending ",
                query.getQueryAsString(false));
    }


    @Test
    public void query_order_by() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode());
        query.addOrderByAscending("unittestproject:title");
        query.addOrderByDescending("unittestproject:summary");
        assertEquals("//*[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and not(@jcr:primaryType='nt:frozenNode')] " +
                "order by @unittestproject:title ascending,@unittestproject:summary descending",
                query.getQueryAsString(false));
    }


    @Test
    public void query_order_by_case_insensitive() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode());
        query.addOrderByAscendingCaseInsensitive("unittestproject:title");
        query.addOrderByDescendingCaseInsensitive("unittestproject:summary");
        assertEquals("//*[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and not(@jcr:primaryType='nt:frozenNode')] " +
                "order by fn:lower-case(@unittestproject:title) ascending,fn:lower-case(@unittestproject:summary) descending",
                query.getQueryAsString(false));
    }


    @Test
    public void query_order_by_mixed_case() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode());
        query.addOrderByAscending("unittestproject:title");
        query.addOrderByDescendingCaseInsensitive("unittestproject:summary");
        assertEquals("//*[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and not(@jcr:primaryType='nt:frozenNode')] " +
                "order by @unittestproject:title ascending,fn:lower-case(@unittestproject:summary) descending",
                query.getQueryAsString(false));
    }


    @Test
    public void testNoFilter() throws Exception {

        HstQuery query = queryMngr.createQuery(session.getRootNode());
        assertEquals("//*["+COMMON_QUERY_SCOPE_PART+"]"+COMMON_QUERY_ORDERBY_PART, query.getQueryAsString(false).trim());
        assertEquals("//*["+COMMON_QUERY_SCOPE_PART+"]", query.getQueryAsString(true).trim());
         
    }
    
    @Test
    public void testEmptyFilter() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode());
        Filter emptyFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        query.setFilter(emptyFilter);
        assertEquals("//*["+COMMON_QUERY_SCOPE_PART+"]", query.getQueryAsString(true).trim());
    }
    
    
    
    @Test
    public void testSimpleFilter() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode());
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        mainFilter.addEqualTo("@a", "a");
        assertEquals("@a = 'a'",mainFilter.getJcrExpression());
        mainFilter.addEqualTo("@b", "b");
        assertEquals("@a = 'a' and @b = 'b'", mainFilter.getJcrExpression());
        query.setFilter(mainFilter);
        assertEquals("//*["+COMMON_QUERY_SCOPE_PART+" and (@a = 'a' and @b = 'b')]", query.getQueryAsString(true).trim());
    }
    
    /**
     * Combine some AND-ed filters
     * @throws Exception
     */
    @Test
    public void testANDedChildFilters() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode());
        Filter mainFilter = new FilterImpl(session, DateTools.Resolution.DAY);
        mainFilter.addContains(".", "contains");
        Filter subAnd1a = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subAnd1b = new FilterImpl(session, DateTools.Resolution.DAY);
        Filter subAnd1c = new FilterImpl(session, DateTools.Resolution.DAY);
        subAnd1a.addEqualTo("@a", "a");
        subAnd1b.addEqualTo("@b", "b");
        subAnd1c.addEqualTo("@c", "c");
        mainFilter.addAndFilter(subAnd1a);
        mainFilter.addAndFilter(subAnd1b);
        mainFilter.addAndFilter(subAnd1c);
        query.setFilter(mainFilter);
        assertEquals("//*["+COMMON_QUERY_SCOPE_PART+" and (jcr:contains(., 'contains') and (@a = 'a') and (@b = 'b') and (@c = 'c'))]", query.getQueryAsString(true).trim());
    }
    
    
    /**
     * Combine some AND-ed filters
     * @throws Exception
     */
    @Test
    public void testANDedChildFiltersWithEmptyParent() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode());
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
        query.setFilter(mainFilter);
        assertEquals("//*["+COMMON_QUERY_SCOPE_PART+" and ((@a = 'a') and (@b = 'b') and (@c = 'c'))]", query.getQueryAsString(true).trim());
    }
    
    /**
     * Combine some OR-ed filters
     * @throws Exception
     */
    @Test
    public void testORedChildFilters() throws Exception {
        HstQuery query = queryMngr.createQuery(session.getRootNode());
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
        query.setFilter(mainFilter);
        assertEquals("//*["+COMMON_QUERY_SCOPE_PART+" and (jcr:contains(., 'contains') or (@a = 'a') or (@b = 'b') or (@c = 'c'))]", query.getQueryAsString(true).trim());
    }
    
   
}
