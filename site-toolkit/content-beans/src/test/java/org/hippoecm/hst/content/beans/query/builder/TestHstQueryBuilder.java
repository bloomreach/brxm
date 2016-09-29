/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.query.builder;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.content.beans.query.builder.FilterBuilder.and;
import static org.hippoecm.hst.content.beans.query.builder.FilterBuilder.filterBuilder;
import static org.hippoecm.hst.content.beans.query.builder.FilterBuilder.or;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestHstQueryBuilder extends AbstractBeanTestCase {

    private static Logger log = LoggerFactory.getLogger(TestHstQueryBuilder.class);

    private HstQueryManager queryManager;
    private MockHstRequestContext requestContext;
    private Node baseContentNode;
    private HippoBean baseContentBean;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ObjectConverter objectConverter = getObjectConverter();
        queryManager = new HstQueryManagerImpl(session, objectConverter, null);
        requestContext = new MockHstRequestContext() {
            @Override
            public boolean isPreview() {
                return false;
            }
        };
        requestContext.setDefaultHstQueryManager(queryManager);
        Map<Session, HstQueryManager> nonDefaultHstQueryManagers = new HashMap<>();
        nonDefaultHstQueryManagers.put(session, queryManager);
        requestContext.setNonDefaultHstQueryManagers(nonDefaultHstQueryManagers);
        requestContext.setSession(session);
        baseContentNode = session.getNode("/unittestcontent");
        baseContentBean = (HippoBean)objectConverter.getObject(baseContentNode);
        requestContext.setSiteContentBaseBean(baseContentBean);
        ModifiableRequestContextProvider.set(requestContext);
    }

    @Test
    public void testSimple() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        String xpathQuery = hstQuery.getQueryAsString(true);
        log.debug("xpathQuery: {}", xpathQuery);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);

        hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }


    @Test
    public void simple_string_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);
        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        filterBuilder("myhippoproject:customid").equalTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_boolean_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", Boolean.TRUE);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        filterBuilder("myhippoproject:customid").equalTo(Boolean.TRUE)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_double_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", 4.0D);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        filterBuilder("myhippoproject:customid").equalTo(4.0D)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_long_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", 4L);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        filterBuilder("myhippoproject:customid").equalTo(4L)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_calendar_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        Calendar calendar = Calendar.getInstance();
        filter.addEqualTo("myhippoproject:customid", calendar);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        filterBuilder("myhippoproject:customid").equalTo(calendar)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_unallowed_object_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        try {
            filter.addEqualTo("myhippoproject:customid", this);
            fail("FilterException expected ");
        } catch (FilterException e) {

        }
        try {
            HstQueryBuilder.create()
                    .scopes(baseContentBean)
                    .filter(
                            filterBuilder("myhippoproject:customid").equalTo(this)
                    )
                    .build();
            fail("FilterException expected ");
        } catch (FilterException e) {

        }
    }


    @Test
    public void simple_filter_no_constraint_results_in_must_exist_property_constraint() throws Exception {

    }

    @Test
    public void simple_nodescope_filter() throws Exception {

    }

    @Test
    public void simple_nodescopes_filter() throws Exception {

    }

    @Test
    public void simple_nestedproperty_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        Filter nestedFilter1 = hstQuery.createFilter();
        nestedFilter1.addEqualTo("myhippoproject:customid", "123");
        filter.addAndFilter(nestedFilter1);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        and(
                                filterBuilder("myhippoproject:customid").equalTo("123")
                        )
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_duplicate_constraint_in_filter_works() throws Exception {
        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        filterBuilder("myhippoproject:customid").equalTo("foo").equalTo("bar")
                )
                .build();

        String xpathQueryInFluent = hstQueryInFluent.getQueryAsString(true);
        assertTrue(xpathQueryInFluent.contains("(@myhippoproject:customid = 'foo' and @myhippoproject:customid = 'bar')"));
    }

    @Test
    public void nested_property_filters() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        Filter nestedFilter1 = hstQuery.createFilter();
        nestedFilter1.addEqualTo("myhippoproject:customid", "123");
        Filter nestedFilter2 = hstQuery.createFilter();
        Filter nestedFilter21 = hstQuery.createFilter();
        nestedFilter21.addLike("myhippoproject:title", "Hello%");
        Filter nestedFilter22 = hstQuery.createFilter();
        nestedFilter22.addContains("myhippoproject:description", "foo");
        nestedFilter2.addOrFilter(nestedFilter21);
        nestedFilter2.addOrFilter(nestedFilter22);
        filter.addAndFilter(nestedFilter1);
        filter.addAndFilter(nestedFilter2);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        and(
                                filterBuilder("myhippoproject:customid").equalTo("123"),
                                or(
                                        filterBuilder("myhippoproject:title").like("Hello%"),
                                        filterBuilder("myhippoproject:description").contains("foo")
                                )
                        )
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }


    @Test
    public void nested_property_filters_with_order_by() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        Filter nestedFilter1 = hstQuery.createFilter();
        nestedFilter1.addEqualTo("myhippoproject:customid", "123");
        Filter nestedFilter2 = hstQuery.createFilter();
        Filter nestedFilter21 = hstQuery.createFilter();
        nestedFilter21.addLike("myhippoproject:title", "Hello%");
        Filter nestedFilter22 = hstQuery.createFilter();
        nestedFilter22.addContains("myhippoproject:description", "foo");
        nestedFilter2.addOrFilter(nestedFilter21);
        nestedFilter2.addOrFilter(nestedFilter22);
        filter.addAndFilter(nestedFilter1);
        filter.addAndFilter(nestedFilter2);
        hstQuery.setFilter(filter);
        hstQuery.addOrderByAscending("myhippoproject:title");
        hstQuery.addOrderByDescending("myhippoproject:date");
        hstQuery.setOffset(10);
        hstQuery.setLimit(5);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        and(
                                filterBuilder("myhippoproject:customid").equalTo("123"),
                                or(
                                        filterBuilder("myhippoproject:title").like("Hello%"),
                                        filterBuilder("myhippoproject:description").contains("foo")
                                )
                        )
                )
                .orderByAscending("myhippoproject:title").orderByDescending("myhippoproject:date")
                .offset(10).limit(5)
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void nested_property_filters_with_negate() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        Filter nestedFilter1 = hstQuery.createFilter();
        nestedFilter1.addEqualTo("myhippoproject:customid", "123");
        nestedFilter1.negate();
        Filter nestedFilter2 = hstQuery.createFilter();
        Filter nestedFilter21 = hstQuery.createFilter();
        nestedFilter21.addLike("myhippoproject:title", "Hello%");
        Filter nestedFilter22 = hstQuery.createFilter();
        nestedFilter22.addContains("myhippoproject:description", "foo");
        nestedFilter2.addOrFilter(nestedFilter21);
        nestedFilter2.addOrFilter(nestedFilter22);
        filter.addAndFilter(nestedFilter1);
        filter.addAndFilter(nestedFilter2);
        filter.negate();
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create()
                .scopes(baseContentBean)
                .filter(
                        and(
                                filterBuilder("myhippoproject:customid").equalTo("123").negate(),
                                or(
                                        filterBuilder("myhippoproject:title").like("Hello%"),
                                        filterBuilder("myhippoproject:description").contains("foo")
                                )
                        ).negate()
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    private void assertHstQueriesEquals(final HstQuery hstQuery1, final HstQuery hstQuery2) throws Exception {
        assertEquals(hstQuery1.getQueryAsString(true), hstQuery2.getQueryAsString(true));
        assertEquals(hstQuery1.getOffset(), hstQuery2.getOffset());
        assertEquals(hstQuery1.getLimit(), hstQuery2.getLimit());
    }
}
