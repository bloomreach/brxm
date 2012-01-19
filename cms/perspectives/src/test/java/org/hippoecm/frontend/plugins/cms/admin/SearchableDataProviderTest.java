/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link org.hippoecm.frontend.plugins.cms.admin.SearchableDataProvider}
 */
public class SearchableDataProviderTest {

    @Test
    public void escapeJcrContainsQuery() {
        assertEquals("foo", SearchableDataProvider.escapeJcrContainsQuery("foo"));
        assertEquals("foo bar", SearchableDataProvider.escapeJcrContainsQuery("foo bar"));
        assertEquals("foo", SearchableDataProvider.escapeJcrContainsQuery("  foo   "));
        assertEquals("", SearchableDataProvider.escapeJcrContainsQuery("   "));
        assertEquals("", SearchableDataProvider.escapeJcrContainsQuery(""));
        assertEquals("", SearchableDataProvider.escapeJcrContainsQuery(null));

        // keep wildcards and 'and' keyword as-is
        assertEquals("adm*", SearchableDataProvider.escapeJcrContainsQuery("adm*"));
        assertEquals("a?min", SearchableDataProvider.escapeJcrContainsQuery("a?min"));
        assertEquals("foo and bar", SearchableDataProvider.escapeJcrContainsQuery("foo and bar"));

        // replace freestanding 'or's with OR, so JCR processes them as an 'OR' query
        assertEquals("foo OR bar", SearchableDataProvider.escapeJcrContainsQuery("foo or bar"));
        assertEquals("editor", SearchableDataProvider.escapeJcrContainsQuery("editor"));

        // remove 'and' and 'or' from the start and end of the query, otherwise Lucene throws a ParseException
        assertEquals("foo OR bar", SearchableDataProvider.escapeJcrContainsQuery("foo or bar or"));
        assertEquals("foo OR bar", SearchableDataProvider.escapeJcrContainsQuery("or foo or bar"));
        assertEquals("foo OR bar", SearchableDataProvider.escapeJcrContainsQuery("foo or bar or "));
        assertEquals("foo OR bar", SearchableDataProvider.escapeJcrContainsQuery(" or foo or bar"));
        assertEquals("foo bar", SearchableDataProvider.escapeJcrContainsQuery("foo bar and "));
        assertEquals("foo bar", SearchableDataProvider.escapeJcrContainsQuery(" and foo bar"));

        // escape special JCR characters
        assertEquals("foo\\!", SearchableDataProvider.escapeJcrContainsQuery("foo!"));
        assertEquals("foo[0\\]", SearchableDataProvider.escapeJcrContainsQuery("foo[0]"));

        // replace single quotes with double ones
        assertEquals("''foo''", SearchableDataProvider.escapeJcrContainsQuery("'foo'"));

        // escape parentheses
        assertEquals("foo \\(bar\\)", SearchableDataProvider.escapeJcrContainsQuery("foo (bar)"));

        // remove all standalone occurences of '*', '**', etc. to avoid an expensive search
        assertEquals("", SearchableDataProvider.escapeJcrContainsQuery("*"));
        assertEquals("foo", SearchableDataProvider.escapeJcrContainsQuery("foo *"));
        assertEquals("foo", SearchableDataProvider.escapeJcrContainsQuery("* foo"));
        assertEquals("", SearchableDataProvider.escapeJcrContainsQuery("* or *"));
        assertEquals("", SearchableDataProvider.escapeJcrContainsQuery("* or * or *"));
        assertEquals("", SearchableDataProvider.escapeJcrContainsQuery("**"));
        assertEquals("", SearchableDataProvider.escapeJcrContainsQuery("  ** or *** or * "));
    }

    @Test
    public void removeStartIgnoreCase() {
        assertEquals("test", SearchableDataProvider.removeStartIgnoreCase("test", "or"));
        assertEquals(" test", SearchableDataProvider.removeStartIgnoreCase("or test", "or"));
        assertEquals(" test", SearchableDataProvider.removeStartIgnoreCase("OR test", "or"));
        assertEquals("foo or bar", SearchableDataProvider.removeStartIgnoreCase("foo or bar", "or"));
        assertEquals("foo OR bar", SearchableDataProvider.removeStartIgnoreCase("foo OR bar", "or"));
        assertEquals("", SearchableDataProvider.removeStartIgnoreCase("", "or"));
        assertEquals(null, SearchableDataProvider.removeStartIgnoreCase(null, "or"));
    }

    @Test
    public void removeEndIgnoreCase() {
        assertEquals("test", SearchableDataProvider.removeEndIgnoreCase("test", "or"));
        assertEquals("test ", SearchableDataProvider.removeEndIgnoreCase("test or", "or"));
        assertEquals("test ", SearchableDataProvider.removeEndIgnoreCase("test OR", "or"));
        assertEquals("foo or bar", SearchableDataProvider.removeEndIgnoreCase("foo or bar", "or"));
        assertEquals("foo OR bar", SearchableDataProvider.removeEndIgnoreCase("foo OR bar", "or"));
        assertEquals("", SearchableDataProvider.removeEndIgnoreCase("", "or"));
        assertEquals(null, SearchableDataProvider.removeEndIgnoreCase(null, "or"));
    }
    
    @Test
    public void replaceStart() {
        assertEquals("qbar", SearchableDataProvider.replaceStart("foobar", "foo", "q"));
        assertEquals("foobar", SearchableDataProvider.replaceStart("foobar", "bar", "q"));
        assertEquals("a", SearchableDataProvider.replaceStart("a", "bar", "q"));
    }

    @Test
    public void replaceEnd() {
        assertEquals("food", SearchableDataProvider.replaceEnd("foobar", "bar", "d"));
        assertEquals("foobar", SearchableDataProvider.replaceEnd("foobar", "foo", "q"));
    }
}
