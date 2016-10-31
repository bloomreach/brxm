/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.jcr.query;

import org.junit.Test;
import org.onehippo.cms7.services.search.commons.query.InitialQueryImpl;
import org.onehippo.cms7.services.search.commons.query.QueryImpl;
import org.onehippo.cms7.services.search.jcr.TestUtils;
import org.onehippo.cms7.services.search.query.QueryBuilder;
import org.onehippo.cms7.services.search.query.QueryUtils;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class TestJcrQueryBuilder extends RepositoryTestCase {

    public final static String COMMON_SCOPE = "(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe')";
    public final static String COMMON_ORDERBY = " order by @jcr:score descending";

    @Test
    public void testEmptyQuery() throws Exception {
        InitialQueryImpl query = new InitialQueryImpl();

        String queryAsString = TestUtils.getQueryAsString(query, session);

        assertEquals("//*" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void testNoFilter() throws Exception {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/");

        String queryAsString = TestUtils.getQueryAsString(query, session);

        assertEquals("//*[" + COMMON_SCOPE + "]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void testSimpleFilter() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").where(text("a").isEqualTo("a")).and(text("b").isEqualTo("b"));
            }
        }.build();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + " and (@a = 'a' and (@b = 'b'))]" + COMMON_ORDERBY, queryAsString);
    }

    /**
     * Combine some AND-ed filters
     *
     * @throws Exception
     */
    @Test
    public void testANDedChildFilters() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").where(text().contains("contains"))
                        .and(text("a").isEqualTo("a"))
                        .and(text("b").isEqualTo("b"))
                        .and(text("c").isEqualTo("c"));
            }
        }.build();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        //*[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and ((jcr:contains(.,'contains') or jcr:contains(.,'contains*')) and (@a = 'a') and (@b = 'b') and (@c = 'c'))] order by @jcr:score descending
        assertEquals("//*[" + COMMON_SCOPE + " and ((jcr:contains(.,'contains') or jcr:contains(.,'contains*')) and (@a = 'a') and (@b = 'b') and (@c = 'c'))]" + COMMON_ORDERBY, queryAsString);
    }

    /**
     * Combine some OR-ed filters
     *
     * @throws Exception
     */
    @Test
    public void testORedChildFilters() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").where(text().contains("contains"))
                        .or(both(text("a").isEqualTo("a")))
                        .or(both(text("b").isEqualTo("b")))
                        .or(both(text("c").isEqualTo("c")));
            }
        }.build();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + " and ((jcr:contains(.,'contains') or jcr:contains(.,'contains*')) or (@a = 'a') or (@b = 'b') or (@c = 'c'))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void testOrderByClause() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").orderBy("a");
            }
        }.build();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + "] order by @a ascending", queryAsString);
    }

    @Test
    public void testOrderByDescendingClause() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").orderBy("a").descending();
            }
        }.build();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + "] order by @a descending", queryAsString);
    }

    @Test
    public void test_empty_value_for_TextContraint() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").where(text("a").isEqualTo(""));
            }
        }.build();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + " and (@a = '')]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
     public void test_empty_value_for_TextContraint_via_QueryUtils() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").where(QueryUtils.text("a").isEqualTo(""));
            }
        }.build();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + " and (@a = '')]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_quotes_value_for_TextContraint() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").where(text("a").isEqualTo("''")).and(text("b").isEqualTo("''"));
            }
        }.build();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + " and (@a = '''' and (@b = ''''))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_adding_return_parent_node_to_empty_query() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.returnParentNode();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*/.." + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_adding_subnode_to_query_with_constrains() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").returnParentNode();

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + "]/.." + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_contains_empty_string_is_optimized_away() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").where(QueryUtils.text().contains(""))
                .and(QueryUtils.text().contains("this"))
                .and(QueryUtils.text().contains(""));

        String queryAsString = TestUtils.getQueryAsString(query, session);
        assertEquals("//*[" + COMMON_SCOPE + " and (((jcr:contains(.,'this') or jcr:contains(.,'this*'))))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_contains_with_small_term_with_wildcarding() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").where(QueryUtils.text().contains("ab"));

        // small terms are not wildcarded
        String queryAsString = TestUtils.getQueryAsString(query, session, true, 3);
        assertEquals("//*[" + COMMON_SCOPE + " and (jcr:contains(.,'ab'))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_contains_with_small_term_without_wildcarding() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").where(QueryUtils.text().contains("ab"));

        String queryAsString = TestUtils.getQueryAsString(query, session, false, 3);
        assertEquals("//*[" + COMMON_SCOPE + " and (jcr:contains(.,'ab'))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_contains_with_big_term_with_wildcarding_at_min_length() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").where(QueryUtils.text().contains("abcdefghij"));

        String queryAsString = TestUtils.getQueryAsString(query, session, true, "abcdefghij".length());
        assertEquals("//*[" + COMMON_SCOPE + " and ((jcr:contains(.,'abcdefghij') or jcr:contains(.,'abcdefghij*')))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_contains_with_big_term_with_wildcarding_at_min_length_plus_one() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").where(QueryUtils.text().contains("abcdefghij"));

        String queryAsString = TestUtils.getQueryAsString(query, session, true, "abcdefghij".length() + 1);
        assertEquals("//*[" + COMMON_SCOPE + " and (jcr:contains(.,'abcdefghij'))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_contains_with_big_term_without_wildcarding() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").where(QueryUtils.text().contains("abcdefghij"));

        String queryAsString = TestUtils.getQueryAsString(query, session, false, 99999999/*not used!*/);
        assertEquals("//*[" + COMMON_SCOPE + " and (jcr:contains(.,'abcdefghij'))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_contains_with_mixed_terms_with_wildcarding() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").where(QueryUtils.text().contains("12 123456"));

        String queryAsString = TestUtils.getQueryAsString(query, session);

        assertEquals("//*[" + COMMON_SCOPE + " and ((jcr:contains(.,'12 123456') or jcr:contains(.,'123456*')))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void test_contains_with_mixed_terms_without_wildcarding() {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/").where(QueryUtils.text().contains("12 123456"));

        String queryAsString = TestUtils.getQueryAsString(query, session, false, -1);

        assertEquals("//*[" + COMMON_SCOPE + " and (jcr:contains(.,'12 123456'))]" + COMMON_ORDERBY, queryAsString);
    }

 }
