/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms7.services.search.query.QueryBuilder;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class TestJcrQueryBuilder extends RepositoryTestCase {

    public final static String COMMON_SCOPE = "(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe')";
    public final static String COMMON_ORDERBY = " order by @jcr:score descending";


    @Test
    public void testNoFilter() throws Exception {
        InitialQueryImpl initialQuery = new InitialQueryImpl();
        QueryImpl query = initialQuery.from("/");

        String queryAsString = getQueryAsString(query);

        assertEquals("//*[" + COMMON_SCOPE + "]" + COMMON_ORDERBY, queryAsString);
    }

    private String getQueryAsString(final QueryImpl query) {
        final JcrQueryBuilder builder = new JcrQueryBuilder(session);
        JcrQueryVisitor visitor = new JcrQueryVisitor(builder, session);
        query.accept(visitor);
        return builder.getQueryString();
    }

    @Test
    public void testSimpleFilter() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").where(text("a").isEqualTo("a")).and(text("b").isEqualTo("b"));
            }
        }.build();

        String queryAsString = getQueryAsString(query);
        assertEquals("//*["+ COMMON_SCOPE +" and (@a = 'a' and (@b = 'b'))]" + COMMON_ORDERBY, queryAsString);
    }
    
    /**
     * Combine some AND-ed filters
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

        String queryAsString = getQueryAsString(query);
        //*[(@hippo:paths='cafebabe-cafe-babe-cafe-babecafebabe') and ((jcr:contains(.,'contains') or jcr:contains(.,'contains*')) and (@a = 'a') and (@b = 'b') and (@c = 'c'))] order by @jcr:score descending
        assertEquals("//*["+ COMMON_SCOPE +" and ((jcr:contains(.,'contains') or jcr:contains(.,'contains*')) and (@a = 'a') and (@b = 'b') and (@c = 'c'))]" + COMMON_ORDERBY, queryAsString);
    }

    /**
     * Combine some OR-ed filters
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

        String queryAsString = getQueryAsString(query);
        assertEquals("//*["+ COMMON_SCOPE +" and ((jcr:contains(.,'contains') or jcr:contains(.,'contains*')) or (@a = 'a') or (@b = 'b') or (@c = 'c'))]" + COMMON_ORDERBY, queryAsString);
    }

    @Test
    public void testOrderByClause() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").orderBy("a");
            }
        }.build();

        String queryAsString = getQueryAsString(query);
        assertEquals("//*["+ COMMON_SCOPE + "] order by @a ascending", queryAsString);
    }

    @Test
    public void testOrderByDescendingClause() throws Exception {
        QueryImpl query = new QueryBuilder() {
            protected QueryImpl build() {
                InitialQueryImpl initialQuery = new InitialQueryImpl();
                return initialQuery.from("/").orderBy("a").descending();
            }
        }.build();

        String queryAsString = getQueryAsString(query);
        assertEquals("//*["+ COMMON_SCOPE + "] order by @a descending", queryAsString);
    }

}
