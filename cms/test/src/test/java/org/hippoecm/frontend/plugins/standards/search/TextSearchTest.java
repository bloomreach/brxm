/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TextSearchTest extends PluginTest {

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
    public void onlyDocumentsInScopeAreFound() throws RepositoryException {
        build(content, session);
        build(alternative, session);
        session.save();

        TextSearchBuilder tsb = new TextSearchBuilder();
        tsb.setText("title");
        tsb.setScope(new String[] { "/test/alternative"} );
        BrowserSearchResult result = tsb.getResultModel().getObject();
        NodeIterator nodes = result.getQueryResult().getNodes();
        assertTrue(nodes.hasNext());
        Node node = nodes.nextNode();
        assertTrue(node.getPath().startsWith("/test/alternative"));
        assertFalse(nodes.hasNext());
    }

    @Test
    public void unReferenceableScopeIsIgnored() throws RepositoryException {
        build(content, session);
        build(nonreferenceable, session);
        session.save();

        TextSearchBuilder tsb = new TextSearchBuilder();
        tsb.setText("title");
        tsb.setScope(new String[] { "/test/alternative"} );
        StringBuilder query = tsb.getQueryStringBuilder();
        String expectedQuery = "//element(*, hippo:document)" +
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
    public void testPathFilter() {
        TextSearchBuilder builder = new TextSearchBuilder();
        builder.setText("x");
        final StringBuilder queryStringBuilder = builder.getQueryStringBuilder();
        final String xpathQuery = queryStringBuilder.toString();

        assertThat(xpathQuery, startsWith("//element(*, hippo:document)"));
    }


}
