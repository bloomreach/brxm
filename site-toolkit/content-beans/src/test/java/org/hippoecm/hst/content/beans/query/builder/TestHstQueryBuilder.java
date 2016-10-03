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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import com.google.common.collect.Lists;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.BasePage;
import org.hippoecm.hst.content.beans.NewsPage;
import org.hippoecm.hst.content.beans.PersistableTextPage;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.RuntimeQueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.repository.util.DateTools;
import org.junit.Before;
import org.junit.Test;

import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.and;
import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.constraint;
import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.or;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestHstQueryBuilder extends AbstractBeanTestCase {

    private HstQueryManager queryManager;
    private MockHstRequestContext requestContext;
    private Node baseContentNode;
    private Node galleryContentNode;
    private Node assetsContentNode;
    private HippoBean baseContentBean;
    private HippoBean galleryContentBean;
    private HippoBean assetsContentBean;

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
        galleryContentNode = session.getNode("/unittestcontent/gallery");
        assetsContentNode = session.getNode("/unittestcontent/assets");
        baseContentBean = (HippoBean)objectConverter.getObject(baseContentNode);
        galleryContentBean = (HippoBean)objectConverter.getObject(galleryContentNode);
        assetsContentBean = (HippoBean)objectConverter.getObject(assetsContentNode);
        requestContext.setSiteContentBaseBean(baseContentBean);
        ModifiableRequestContextProvider.set(requestContext);
    }

    @Test
    public void basic_query_without_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean).build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void basic_query_with_primaryType_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean, "unittestproject:textpage");
        HstQuery hstQuery2 = queryManager.createQuery(baseContentBean.getNode(), "unittestproject:textpage", false);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes("unittestproject:textpage")
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
        assertHstQueriesEquals(hstQuery2, hstQueryInFluent);
    }

    @Test
    public void basic_query_with_primaryTypes_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean, "unittestproject:textpage", "unittestproject:newspage");

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes("unittestproject:textpage", "unittestproject:newspage")
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);

        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes("unittestproject:textpage")
                .ofPrimaryTypes("unittestproject:newspage")
                .build();

        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent2);
    }

    @Test
    public void basic_query_with_primaryTypeClazz_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean, NewsPage.class);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes(NewsPage.class)
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);

    }

    @Test
    public void basic_query_with_primaryTypeClazzes_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean, NewsPage.class, PersistableTextPage.class);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes(NewsPage.class, PersistableTextPage.class)
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);

        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes(NewsPage.class)
                .ofPrimaryTypes(PersistableTextPage.class)
                .build();

        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent2);

    }

    @Test
    public void basic_query_with_primaryType_and_clazzes_mixed() throws Exception {
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes(PersistableTextPage.class, NewsPage.class)
                .build();

        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes(NewsPage.class)
                .ofPrimaryTypes("unittestproject:textpage")
                .build();

        HstQuery hstQueryInFluent3 = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes("unittestproject:textpage")
                .ofPrimaryTypes(NewsPage.class)
                .build();

        HstQuery hstQueryInFluent4 = HstQueryBuilder.create(baseContentBean)
                .ofPrimaryTypes("unittestproject:textpage")
                .ofPrimaryTypes("unittestproject:newspage")
                .build();

        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent2);
        assertHstQueriesEquals(hstQueryInFluent2, hstQueryInFluent3);
        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent4);

    }

    @Test
    public void basic_query_with_nodetype_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean.getNode(), "unittestproject:textpage", true);
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofTypes(PersistableTextPage.class)
                .build();
        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .ofTypes("unittestproject:textpage")
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
        assertHstQueriesEquals(hstQuery, hstQueryInFluent2);
    }

    @Test
    public void basic_query_with__base_nodetype_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean.getNode(), "unittestproject:basedocument", true);
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofTypes(BasePage.class)
                .build();
        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .ofTypes("unittestproject:basedocument")
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
        assertHstQueriesEquals(hstQuery, hstQueryInFluent2);
    }

    @Test
    public void basic_query_with_nodetypes_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean.getNode(), true, "unittestproject:textpage", "unittestproject:newspage");
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofTypes(PersistableTextPage.class, NewsPage.class)
                .build();
        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .ofTypes("unittestproject:textpage", "unittestproject:newspage")
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
        assertHstQueriesEquals(hstQuery, hstQueryInFluent2);
    }

    @Test
    public void basic_query_with_base_nodetypes_constraints() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean.getNode(), true, "unittestproject:textpage", "unittestproject:newspage", "unittestproject:basedocument");
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofTypes(PersistableTextPage.class, NewsPage.class, BasePage.class)
                .build();
        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .ofTypes("unittestproject:textpage", "unittestproject:newspage", "unittestproject:basedocument")
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
        assertHstQueriesEquals(hstQuery, hstQueryInFluent2);
    }

    @Test
    public void basic_query_with_base_nodetypes_constraints_mixed() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean.getNode(), true, "unittestproject:newspage", "unittestproject:textpage");
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .ofTypes(PersistableTextPage.class)
                .ofTypes("unittestproject:newspage")
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);

        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .ofTypes("unittestproject:newspage")
                .ofTypes(PersistableTextPage.class)
                .build();

        HstQuery hstQueryInFluent3 = HstQueryBuilder.create(baseContentBean)
                .ofTypes(NewsPage.class, PersistableTextPage.class)
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent2);
        assertHstQueriesEquals(hstQuery, hstQueryInFluent3);
    }

    @Test(expected = RuntimeQueryException.class)
    public void basic_query_mix_primary_nodetypes_and_nodetypes_constraints_not_supported() throws Exception {
        HstQueryBuilder.create(baseContentBean)
                .ofTypes("unittestproject:textpage")
                .ofPrimaryTypes("unittestproject:newspage")
                .build();
    }

    @Test
    public void simple_property_does_exist_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addNotNull("myhippoproject:customid");
        hstQuery.setFilter(filter);
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").exists()
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void no_constraint_on_filderBuilder_defaults_to_property_must_exist_query() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addNotNull("myhippoproject:customid");
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);

        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").exists()
                )
                .build();

        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent2);
    }

    @Test
    public void simple_property_does_not_exist_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addIsNull("myhippoproject:customid");
        hstQuery.setFilter(filter);
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").notExists()
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_string_equal_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_string_not_equal_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addNotEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").notEqualTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }


    @Test
    public void simple_string_equal_case_insensitive_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualToCaseInsensitive("myhippoproject:customid", "ABc");
        hstQuery.setFilter(filter);
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalToCaseInsensitive("ABc")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_boolean_equal_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", Boolean.TRUE);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo(Boolean.TRUE)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_double_equal_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", 4.0D);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo(4.0D)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_long_equal_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", 4L);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo(4L)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_calendar_equal_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        Calendar calendar = Calendar.getInstance();
        filter.addEqualTo("myhippoproject:customid", calendar);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo(calendar)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_date_equal_with_resolution_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        Date date = new Date();
        filter.addEqualTo("myhippoproject:customid", date);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo(date)
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_calendar_equal_with_resolution_property_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        Calendar calendar = Calendar.getInstance();
        filter.addEqualTo("myhippoproject:customid", calendar, DateTools.Resolution.DAY);
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo(calendar, DateTools.Resolution.DAY)
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
            HstQueryBuilder.create(baseContentBean)
                    .where(
                            constraint("myhippoproject:customid").equalTo(this)
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

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        and(
                                constraint("myhippoproject:customid").equalTo("123")
                        )
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void simple_duplicate_constraint_in_filter_works() throws Exception {
        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo("foo").equalTo("bar")
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

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        and(
                                constraint("myhippoproject:customid").equalTo("123"),
                                or(
                                        constraint("myhippoproject:title").like("Hello%"),
                                        constraint("myhippoproject:description").contains("foo")
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

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        and(
                                constraint("myhippoproject:customid").equalTo("123"),
                                or(
                                        constraint("myhippoproject:title").like("Hello%"),
                                        constraint("myhippoproject:description").contains("foo")
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

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(
                        and(
                                constraint("myhippoproject:customid").equalTo("123").negate(),
                                or(
                                        constraint("myhippoproject:title").like("Hello%"),
                                        constraint("myhippoproject:description").contains("foo")
                                )
                        ).negate()
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void excluding_scopes_beans_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        hstQuery.excludeScopes(Lists.newArrayList(assetsContentBean, galleryContentBean));
        Filter filter = hstQuery.createFilter();
        filter.addNotEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .excludeScopes(assetsContentBean, galleryContentBean)
                .where(
                        constraint("myhippoproject:customid").notEqualTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void excluding_scopes_nodes_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        hstQuery.excludeScopes(new Node[]{galleryContentNode, assetsContentNode});
        Filter filter = hstQuery.createFilter();
        filter.addNotEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .excludeScopes(galleryContentNode, assetsContentNode)
                .where(
                        constraint("myhippoproject:customid").notEqualTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void multiple_scopes_nodes_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        hstQuery.addScopes(new Node[]{galleryContentNode, assetsContentNode});
        Filter filter = hstQuery.createFilter();
        filter.addNotEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentNode, galleryContentNode, assetsContentNode)
                .where(
                        constraint("myhippoproject:customid").notEqualTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void multiple_scopes_beans_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        hstQuery.addScopes(Lists.newArrayList(assetsContentBean, galleryContentBean));
        Filter filter = hstQuery.createFilter();
        filter.addNotEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean, assetsContentBean, galleryContentBean)
                .where(
                        constraint("myhippoproject:customid").notEqualTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void multiple_scopes_and_exclusion_beans_filter() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        hstQuery.addScopes(Lists.newArrayList(assetsContentBean));
        hstQuery.excludeScopes(Lists.newArrayList(galleryContentBean));
        Filter filter = hstQuery.createFilter();
        filter.addNotEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean, assetsContentBean)
                .excludeScopes(galleryContentBean)
                .where(
                        constraint("myhippoproject:customid").notEqualTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    private void assertHstQueriesEquals(final HstQuery hstQuery1, final HstQuery hstQuery2) throws Exception {
        assertEquals(hstQuery1.getQueryAsString(true), hstQuery2.getQueryAsString(true));
        assertEquals(hstQuery1.getOffset(), hstQuery2.getOffset());
        assertEquals(hstQuery1.getLimit(), hstQuery2.getLimit());
    }

    @Test(expected = RuntimeQueryException.class)
    public void duplicate_where_clause_throws_query_exception() throws Exception {
        HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo("foo")
                ).where(
                        constraint("myhippoproject:customid").equalTo("bar")
                )
                .build();
    }

    @Test(expected = RuntimeQueryException.class)
    public void duplicate_offset_throws_query_exception() throws Exception {
        HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo("foo")
                )
                .offset(2)
                .offset(3)
                .build();
    }

    @Test(expected = RuntimeQueryException.class)
    public void duplicate_limit_throws_query_exception() throws Exception {
        HstQueryBuilder.create(baseContentBean)
                .where(
                        constraint("myhippoproject:customid").equalTo("foo")
                )
                .limit(2)
                .limit(3)
                .build();
    }

    @Test
    public void duplicate_exclude_scopes_is_allowed() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        hstQuery.excludeScopes(Lists.newArrayList(galleryContentBean, assetsContentBean));
        Filter filter = hstQuery.createFilter();
        filter.addNotEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean, assetsContentBean)
                .excludeScopes(galleryContentBean)
                .excludeScopes(assetsContentBean)
                .where(
                        constraint("myhippoproject:customid").notEqualTo("123")
                )
                .build();

        assertHstQueriesEquals(hstQuery, hstQueryInFluent);
    }

    @Test
    public void duplicate_order_by_ascending_allowed() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);
        hstQuery.addOrderByAscending("myhippoproject:title");
        hstQuery.addOrderByAscending("myhippoproject:date");
        hstQuery.setOffset(10);
        hstQuery.setLimit(5);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(constraint("myhippoproject:customid").equalTo("123"))
                .orderByAscending("myhippoproject:title")
                .orderByAscending("myhippoproject:date")
                .offset(10).limit(5)
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);


        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .where(constraint("myhippoproject:customid").equalTo("123"))
                .orderByAscending("myhippoproject:title", "myhippoproject:date")
                .offset(10).limit(5)
                .build();
        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent2);
    }

    @Test
    public void duplicate_order_by_ascending_case_insensitive_allowed() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);
        hstQuery.addOrderByAscendingCaseInsensitive("myhippoproject:title");
        hstQuery.addOrderByAscendingCaseInsensitive("myhippoproject:date");
        hstQuery.setOffset(10);
        hstQuery.setLimit(5);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(constraint("myhippoproject:customid").equalTo("123"))
                .orderByAscendingCaseInsensitive("myhippoproject:title")
                .orderByAscendingCaseInsensitive("myhippoproject:date")
                .offset(10).limit(5)
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);


        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .where(constraint("myhippoproject:customid").equalTo("123"))
                .orderByAscendingCaseInsensitive("myhippoproject:title" , "myhippoproject:date")
                .offset(10).limit(5)
                .build();
        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent2);
    }


    @Test
    public void duplicate_order_by_descending_allowed() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);
        hstQuery.addOrderByDescending("myhippoproject:title");
        hstQuery.addOrderByDescending("myhippoproject:date");
        hstQuery.setOffset(10);
        hstQuery.setLimit(5);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(constraint("myhippoproject:customid").equalTo("123"))
                .orderByDescending("myhippoproject:title")
                .orderByDescending("myhippoproject:date")
                .offset(10).limit(5)
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);

        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .where(constraint("myhippoproject:customid").equalTo("123"))
                .orderByDescending("myhippoproject:title", "myhippoproject:date")
                .offset(10).limit(5)
                .build();

        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent2);
    }

    @Test
    public void duplicate_order_by_descending_case_insensitive_allowed() throws Exception {
        HstQuery hstQuery = queryManager.createQuery(baseContentBean);
        Filter filter = hstQuery.createFilter();
        filter.addEqualTo("myhippoproject:customid", "123");
        hstQuery.setFilter(filter);
        hstQuery.addOrderByDescendingCaseInsensitive("myhippoproject:title");
        hstQuery.addOrderByDescendingCaseInsensitive("myhippoproject:date");
        hstQuery.setOffset(10);
        hstQuery.setLimit(5);

        HstQuery hstQueryInFluent = HstQueryBuilder.create(baseContentBean)
                .where(constraint("myhippoproject:customid").equalTo("123"))
                .orderByDescendingCaseInsensitive("myhippoproject:title")
                .orderByDescendingCaseInsensitive("myhippoproject:date")
                .offset(10).limit(5)
                .build();
        assertHstQueriesEquals(hstQuery, hstQueryInFluent);

        HstQuery hstQueryInFluent2 = HstQueryBuilder.create(baseContentBean)
                .where(constraint("myhippoproject:customid").equalTo("123"))
                .orderByDescendingCaseInsensitive("myhippoproject:title", "myhippoproject:date")
                .offset(10).limit(5)
                .build();
        assertHstQueriesEquals(hstQueryInFluent, hstQueryInFluent2);

    }
}
