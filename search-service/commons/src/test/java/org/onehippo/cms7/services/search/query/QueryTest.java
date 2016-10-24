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
package org.onehippo.cms7.services.search.query;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;
import org.onehippo.cms7.services.search.commons.query.InitialQueryImpl;
import org.onehippo.cms7.services.search.commons.query.QueryImpl;
import org.onehippo.cms7.services.search.query.constraint.DateConstraint;
import org.onehippo.cms7.services.search.query.reflect.QueryNode;
import org.onehippo.cms7.services.search.query.reflect.StringQueryVisitor;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms7.services.search.query.QueryUtils.both;
import static org.onehippo.cms7.services.search.query.QueryUtils.date;
import static org.onehippo.cms7.services.search.query.QueryUtils.either;
import static org.onehippo.cms7.services.search.query.QueryUtils.integer;
import static org.onehippo.cms7.services.search.query.QueryUtils.not;
import static org.onehippo.cms7.services.search.query.QueryUtils.text;

public class QueryTest {

    static class DummySearchService implements SearchService{

        @Override
        public boolean isAlive() {
            return false;
        }

        @Override
        public InitialQuery createQuery() throws SearchServiceException {
            return new InitialQueryImpl();
        }

        @Override
        public QueryResult search(final Query searchQuery) throws SearchServiceException {
            throw new SearchServiceException();
        }

        @Override
        public QueryNode asQueryNode(final Query query) throws SearchServiceException {
            throw new SearchServiceException();
        }

    }

    /*
     * Query
     *   ScopedQuery
     *   SelectedQuery
     *   FilteredQuery
     */
    @Test
    public void testAPI() throws Exception {
        SearchService searchService = new DummySearchService();
        searchService.createQuery()
                .from("/content/documents")
                .ofType("hippo:document")
                .select("title")
                    .and("summary")
                .where(text("title").contains("hippo"))
                    .and(date("hippostd:publicationdate").from(new Date()).andTo(new Date()))
                    .and(integer("counter").from(0))
                    .and(not(text("author").isEqualTo("me")))
                .limitTo(10)
                .orderBy("hippostd:creationDate").descending();

        searchService.createQuery()
                .select("title")
                .where(text().contains("event"))
                .limitTo(1)
                .offsetBy(5);

        searchService.createQuery()
                .ofType("hippo:document")
                .select("author")
                .where(text("title").contains("ard"))
                    .and(text().contains("haha"));

        searchService.createQuery()
                .ofType("hippo:document")
                .where(text("title").contains("ard"))
                    .or(text().contains("haha"))
                    .or(date("publication").from(new Date()));

        searchService.createQuery()
                .from("/content/documents")
                .where(text("title").contains("ard"))
                    .and(either(text("summary").contains("unico")).or(text("body").contains("nour")))
                    .and(date("publication").from(new Date()));

        searchService.createQuery()
                .where(text("title").contains("nour"))
                    .or(both(text().contains("mdb"))
                        .and(integer("citation").from(10)));

        searchService.createQuery()
                .from("/content/documents")
                    .or("/content/taxonomy")
                    .except("/content/documents/common")
                .where(text("title").isEqualTo("ho ho ho"));
    }

    @Test
    public void testStringVisitor() throws Exception {

        Locale.setDefault(Locale.Category.FORMAT, Locale.ENGLISH);

        long fixedTime = 1348827767244L;
        QueryImpl query =  new InitialQueryImpl()
                .ofType("hippo:document")
                .select("author")
                .where(text("title").contains("ard"))
                .and(text().contains("haha"))
                .and(either(integer("count").to(5)).or(date("publicationDate").from(new Date(fixedTime))));
        final StringQueryVisitor visitor = new StringQueryVisitor();
        query.accept(visitor);
        assertEquals(
                " of type hippo:document select author "
                        + "where (text title contains ard) "
                        + "and (text [any] contains haha) "
                        + "and ((int count <= 5) or (date publicationDate >= Sep 28, 2012, resolution = DAY))",
                visitor.getString());
    }

    @Test
    public void testStringVisitorRangeQuery() throws Exception {

        Locale.setDefault(Locale.Category.FORMAT, Locale.ENGLISH);

        long fixedTime = 1348827767244L;
        Date after = new Date(fixedTime);
        Calendar beforeCal = Calendar.getInstance();
        beforeCal.setTimeInMillis(fixedTime);
        beforeCal.add(Calendar.DAY_OF_YEAR, 1);
        Date before = beforeCal.getTime();
        QueryImpl query =  new InitialQueryImpl()
                .ofType("hippo:document")
                .select("author")
                .where(text("title").contains("ard"))
                .and(text().contains("haha"))
                .and(either(integer("count").from(5)).or(date("publicationDate").from(after).andTo(before)));
        final StringQueryVisitor visitor = new StringQueryVisitor();
        query.accept(visitor);
        assertEquals(
                " of type hippo:document select author "
                        + "where (text title contains ard) "
                        + "and (text [any] contains haha) "
                        + "and ((int count >= 5) or (date publicationDate in [Sep 28, 2012, Sep 29, 2012], resolution = DAY))",
                visitor.getString());
    }

    @Test
    public void testStringVisitorRangeQueryNonDefaultResolution() throws Exception {

        Locale.setDefault(Locale.Category.FORMAT, Locale.ENGLISH);

        long fixedTime = 1348827767244L;
        Date after = new Date(fixedTime);
        Calendar beforeCal = Calendar.getInstance();
        beforeCal.setTimeInMillis(fixedTime);
        beforeCal.add(Calendar.DAY_OF_YEAR, 1);
        Date before = beforeCal.getTime();
        QueryImpl query =  new InitialQueryImpl()
                .ofType("hippo:document")
                .select("author")
                .where(text("title").contains("ard"))
                .and(text().contains("haha"))
                .and(either(integer("count").to(5)).or(date("publicationDate").from(after, DateConstraint.Resolution.YEAR).andTo(before)));
        final StringQueryVisitor visitor = new StringQueryVisitor();
        query.accept(visitor);
        assertEquals(
                " of type hippo:document select author "
                        + "where (text title contains ard) "
                        + "and (text [any] contains haha) "
                        + "and ((int count <= 5) or (date publicationDate in [Sep 28, 2012, Sep 29, 2012], resolution = YEAR))",
                visitor.getString());
    }

    @Test
    public void testContext() throws Exception {
        Query query;
        final SearchService searchService = new DummySearchService();

        query = new QueryBuilder() {
            protected Query getAapDocuments() {
                final InitialQuery initial = searchService.createQuery();
                return initial.from("/content/documents").where(text().contains("aap"));
            }
        }.getAapDocuments();

        query = new QueryBuilder() {
            protected Query complete() {
                final InitialQuery initial = searchService.createQuery();
                return initial.ofType("hippo:document").where(text("title").contains("ard")).or(
                        text().contains("haha")).or(date("publication").from(new Date()));
            }
        }.complete();
    }

    @Test
    public void test_empty_value_for_TextContraint() {
        QueryImpl query =  new InitialQueryImpl().where(QueryUtils.text("a").isEqualTo(""));
        final StringQueryVisitor visitor = new StringQueryVisitor();
        query.accept(visitor);
        assertEquals(" where (text a = '')",
                visitor.getString());
    }

    @Test
    public void test_quotes_value_for_TextContraint() {
        QueryImpl query =  new InitialQueryImpl().where(QueryUtils.text("a").isEqualTo("''"));
        final StringQueryVisitor visitor = new StringQueryVisitor();
        query.accept(visitor);
        assertEquals(" where (text a = '')",
                visitor.getString());
    }
}
