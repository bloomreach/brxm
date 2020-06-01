/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.search;

import java.util.Set;
import java.util.TreeSet;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class GeneralSearchBuilderTest extends PluginTest {

    String[] content = {
            "/test", "nt:unstructured",
            "/test/content", "nt:unstructured",
            "/test/content/a", "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/a/a", "frontendtest:document",
            "jcr:mixinTypes", "mix:referenceable",
            "title", "title",
            "introduction", "introduction",
            "ab", "ab",
    };
    String[] alternative = {
            "/test/alternative", "nt:unstructured",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/alternative/a", "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/alternative/a/a", "frontendtest:document",
            "jcr:mixinTypes", "mix:referenceable",
            "title", "title",
            "ab", "ab",
    };
    String[] nonreferenceable = {
            "/test/alternative", "nt:unstructured",
            "/test/alternative/a", "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/alternative/a/a", "frontendtest:document",
            "jcr:mixinTypes", "mix:referenceable",
            "title", "title",
            "ab", "ab",
    };

    @Test
    public void wildcardsAreIgnored() throws RepositoryException {
        build(content, session);
        session.save();

        GeneralSearchBuilder gsb = new GeneralSearchBuilder();
        gsb.setText("*itle");
        BrowserSearchResult result = gsb.getResultModel().getObject();
        QueryResult qr = result.getQueryResult();
        assertFalse(qr.getNodes().hasNext());

        gsb.setText("?itle");
        qr = gsb.getResultModel().getObject().getQueryResult();
        assertFalse(qr.getNodes().hasNext());
    }

    @Test
    public void specialCharactersAreIgnored() throws RepositoryException {
        build(content, session);
        session.save();

        GeneralSearchBuilder gsb = new GeneralSearchBuilder();
        gsb.setText("|!(){}[]^title\"~*?:\\");
        BrowserSearchResult result = gsb.getResultModel().getObject();
        QueryResult qr = result.getQueryResult();
        assertTrue(qr.getNodes().hasNext());
    }

    @Test
    public void keywordsSmallerThanThreeLettersAreNotIgnored() throws RepositoryException {
        build(content, session);
        session.save();

        GeneralSearchBuilder gsb = new GeneralSearchBuilder();
        gsb.setText("ab");
        BrowserSearchResult result = gsb.getResultModel().getObject();
        QueryResult qr = result.getQueryResult();
        assertTrue(qr.getNodes().hasNext());
    }

    @Test
    public void multipleKeywordsAreAllPresent() throws RepositoryException {
        build(content, session);
        build(alternative, session);
        session.save();

        GeneralSearchBuilder gsb = new GeneralSearchBuilder();
        gsb.setWildcardSearch(true);
        gsb.setText("tit intr");
        assertNotNull(gsb.getResultModel());
        BrowserSearchResult bsr = gsb.getResultModel().getObject();
        int count = 0;
        for (NodeIterator iter = bsr.getQueryResult().getNodes(); iter.hasNext(); ) {
            iter.next();
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void shortKeywordsAreNotIgnored() throws RepositoryException {
        build(content, session);
        build(alternative, session);
        session.save();

        GeneralSearchBuilder gsb = new GeneralSearchBuilder();
        gsb.setWildcardSearch(true);
        gsb.setText("tit i");
        assertNotNull(gsb.getResultModel());
        BrowserSearchResult bsr = gsb.getResultModel().getObject();
        int count = 0;
        for (NodeIterator iter = bsr.getQueryResult().getNodes(); iter.hasNext(); ) {
            iter.nextNode();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void wildcardSearchFindsWordHead() throws RepositoryException {
        build(content, session);
        session.save();

        GeneralSearchBuilder gsb = new GeneralSearchBuilder();
        gsb.setText("tit");
        gsb.setWildcardSearch(true);
        BrowserSearchResult result = gsb.getResultModel().getObject();
        QueryResult qr = result.getQueryResult();
        assertTrue(qr.getNodes().hasNext());
    }

    @Test
    public void excludedTypesAreNotFound() throws RepositoryException {
        build(content, session);
        session.save();

        GeneralSearchBuilder gsb = new GeneralSearchBuilder();
        gsb.setText("title");
        gsb.setExcludedPrimaryTypes(new String[]{"frontendtest:document"});
        BrowserSearchResult result = gsb.getResultModel().getObject();
        QueryResult qr = result.getQueryResult();
        assertFalse(qr.getNodes().hasNext());
    }

    @Test
    public void queryWithExcludedTypes() throws RepositoryException {
        build(content, session);
        session.save();
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        tsb.setText("title");
        tsb.setExcludedPrimaryTypes(new String[] { "frontend:document", "backend:document" });
        StringBuilder query = tsb.getQueryStringBuilder();
        final String xpathQuery = query.toString();
        assertThat(xpathQuery, is("//element()[not(@jcr:primaryType='frontend:document' or @jcr:primaryType='backend:document') and jcr:contains(.,'title')] order by @jcr:score descending"));
    }

    @Test
    public void unReferenceableScopeIsIgnored() throws RepositoryException {
        build(content, session);
        build(nonreferenceable, session);
        session.save();

        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        tsb.setText("title");
        tsb.setScope(new String[] { "/test/alternative"} );
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[jcr:contains(.,'title')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
        BrowserSearchResult result = tsb.getResultModel().getObject();
        NodeIterator nodes = result.getQueryResult().getNodes();
        Set<String> paths = new TreeSet<>();
        while (nodes.hasNext()) {
            paths.add(nodes.nextNode().getPath());
        }
        assertTrue(paths.contains("/test/alternative/a/a"));
        assertTrue(paths.contains("/test/content/a/a"));
    }

    @Test
    public void queryWithNoConfiguredPrimaryType() throws Exception {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        tsb.setText("title");
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[jcr:contains(.,'title')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }


    @Test
    public void queryWithSingleConfiguredPrimaryType() throws Exception {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        tsb.setText("title");
        tsb.setIncludePrimaryTypes(new String[]{"frontend:document"});
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()[@jcr:primaryType='frontend:document']" +
                "[jcr:contains(.,'title')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }

    @Test
    public void queryWithMultipleConfiguredPrimaryType() throws Exception {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        tsb.setText("title");
        tsb.setIncludePrimaryTypes(new String[]{"frontend:document", "backend:document"});
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()[@jcr:primaryType='frontend:document' or @jcr:primaryType='backend:document']" +
                "[jcr:contains(.,'title')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }

    @Test
    public void querySpacesAndMultiWord() throws Exception {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        // leading and trailing space does not matter
        tsb.setText("   title   ");
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[jcr:contains(.,'title')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        tsb.setText("  title  bar  ");
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[jcr:contains(.,'title bar')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

    }

    @Test
    public void queryMultiWordWithORandAND() throws Exception {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        tsb.setText(" title OR bar AND lux");
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[jcr:contains(.,'title OR bar AND lux')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        // consecutive operators are skipped, hence 'title OR AND bar OR OR AND AND lux' should result in 'title OR bar OR AND lux'
        tsb.setText(" title OR AND bar OR OR AND AND lux");
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[jcr:contains(.,'title AND bar AND lux')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        // 'or' and 'and' (lowercase) are *not* and operator
        tsb.setText(" title OR or bar OR or AND and lux");
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[jcr:contains(.,'title OR or bar OR or AND and lux')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        // leading and trailing operators are skipped
        tsb.setText(" OR title bar OR lux AND ");
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[jcr:contains(.,'title bar OR lux')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        // multiple leading and trailing operators are skipped
        tsb.setText(" AND OR title bar OR lux AND AND ");
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[jcr:contains(.,'title bar OR lux')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        // assert search with dashes and _
        tsb.setText("AND hippo-cms great_version AND ");
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[jcr:contains(.,'hippo-cms great_version')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }


    @Test
    public void queryWildcards() throws Exception {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        // set wildcards to true
        tsb.setWildcardSearch(true);

        /*
         * Because wildcards are set to true, we expect TWO jcr:contains being or-ed, where one contains wildcards and one not
         * Thus for example  'title' should result in
         *
         *  (jcr:contains(.,'title') or jcr:contains(.,'title*'))
         *
         *  we do this in this way, because the wildcard postfixing and Lucene tokenizing and stemming do not cooperate that well. Assume the following
         *  Dutch word 'slapen'
         *
         *  This gets indexed as 'slap'. Now, 'slapen*' does not return a hit. A bigger problem are words like:
         *
         *  'hippo-cms'
         *
         *  When we would do the wild card search, the search would be : 'hippo-cms*' : but there are no terms in the Lucene index
         *  starting with 'hippo-cms' (during indexing, 'hippo-cms' was tokenized on '-') thus the wildcard search leads to no results.
         *
         *  Hence, we search for wildcard postfixed terms OR-ed with the original query
         */

        tsb.setText(" title");

        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[(jcr:contains(.,'title') or jcr:contains(.,'title*'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        tsb.setText(" title bar lux ");

        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[(jcr:contains(.,'title bar lux') or jcr:contains(.,'title* bar* lux*'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));


        tsb.setText("A quick brown fox jumps at");
        // when a term is shorter then #getMinimalLength() it should be SKIPPED for wildcard searching
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[(jcr:contains(.,'A quick brown fox jumps at') or jcr:contains(.,'quick* brown* fox* jumps*'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        tsb.setText("A quick fox AND jumps OR lazy");
        // OR and AND should never get wildcard postfix
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[(jcr:contains(.,'A quick fox AND jumps OR lazy') or jcr:contains(.,'quick* fox* AND jumps* OR lazy*'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        tsb.setText("OR or A quick fox AND jumps at OR AND");
        // OR and AND should never get wildcard postfix
        // after the ignored first OR, the 'or' should be skipped for wildcard search because too short
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[(jcr:contains(.,'or A quick fox AND jumps at') or jcr:contains(.,'quick* fox* AND jumps*'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        // dashes and _ should work well
        tsb.setText("AND hippo-cms great_version AND ");
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[(jcr:contains(.,'hippo-cms great_version') or jcr:contains(.,'hippo-cms* great_version*'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));

        // only smaller than 3 letters text thus no wildcard postfix version
        tsb.setText("AND is it so OR ");
        query = tsb.getQueryStringBuilder();
        expectedQuery = "//element()" +
                "[jcr:contains(.,'is it so')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }

    @Test
    public void queryDiacriticsAreRemoved() throws Exception {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        // set wildcards to true
        tsb.setWildcardSearch(true);
        tsb.setText("très Plattenbandförderer ");

        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[(jcr:contains(.,'tres Plattenbandforderer') or jcr:contains(.,'tres* Plattenbandforderer*'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }

    @Test
    public void queryWordsWithMinusSignAndExclamations() throws Exception {

        validateQueryWithoutWildcardInjection("CMS-345", "CMS-345");
        validateQueryWithoutWildcardInjection("The !qui!ck!", "The quick");
        validateQueryWithoutWildcardInjection("The -quick-", "The -quick");
        validateQueryWithoutWildcardInjection("The -quick -*", "The -quick");

        validateQueryWithWildcardInjection("The !quick!", "The quick", "The* quick*");
        validateQueryWithWildcardInjection("The -quick-", "The -quick", "The* -quick*");
        validateQueryWithWildcardInjection("The !qui!ck!", "The quick", "The* quick*");
        validateQueryWithWildcardInjection("The -qui-ck-", "The -qui-ck", "The* -qui-ck*");
        validateQueryWithWildcardInjection("The -quick -*", "The -quick", "The* -quick*");
    }

    private void validateQueryWithoutWildcardInjection(String queryString, String expectation) {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        tsb.setText(queryString);
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[jcr:contains(.,'"+expectation+"')] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }

    private void validateQueryWithWildcardInjection(String queryString, String expectationNoWildcard, String expectationWithWildcard) {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        tsb.setWildcardSearch(true);
        tsb.setText(queryString);
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[(jcr:contains(.,'"+expectationNoWildcard+"') or jcr:contains(.,'"+expectationWithWildcard+"'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }

    @Test
    public void queryWordsWithApostrophe() {
        GeneralSearchBuilder tsb = new GeneralSearchBuilder();
        // set wildcards to true
        tsb.setWildcardSearch(true);
        tsb.setText(" doesn't  ");
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element()" +
                "[(jcr:contains(.,'doesn''t') or jcr:contains(.,'doesn''t*'))] order by @jcr:score descending";
        assertTrue("Query: " + query.toString() + " is not equal to expected xpath",
                (query.toString()).equals(expectedQuery));
    }


}
