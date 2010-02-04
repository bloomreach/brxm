/*
 *  Copyright 2010 Hippo.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.junit.Test;

public class TextSearchTest extends PluginTest {

    String[] content = {
        "/test", "nt:unstructured",
            "/test/content", "nt:unstructured",
                "/test/content/a", "hippo:handle",
                    "jcr:mixinTypes", "hippo:hardhandle",
                    "/test/content/a/a", "frontendtest:document",
                        "jcr:mixinTypes", "hippo:harddocument",
                        "hippostd:state", "published",
                        "title", "title",
                        "ab", "ab",
    };
    String[] alternative = {
        "/test/alternative", "nt:unstructured",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/alternative/a", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
                "/test/alternative/a/a", "frontendtest:document",
                    "jcr:mixinTypes", "hippo:harddocument",
                    "hippostd:state", "published",
                    "title", "title",
                    "ab", "ab",
    };

    @Test
    public void wildcardsAreIgnored() throws RepositoryException {
        build(session, content);
        session.save();

        TextSearchBuilder tsb = new TextSearchBuilder();
        tsb.setText("*itle");
        BrowserSearchResult result = tsb.getResultModel().getObject();
        QueryResult qr = result.getQueryResult();
        assertFalse(qr.getNodes().hasNext());

        tsb.setText("?itle");
        qr = tsb.getResultModel().getObject().getQueryResult();
        assertFalse(qr.getNodes().hasNext());
    }

    @Test
    public void keywordsSmallerThanThreeLettersAreIgnored() throws RepositoryException {
        build(session, content);
        session.save();

        TextSearchBuilder tsb = new TextSearchBuilder();
        tsb.setText("ab");
        assertNull(tsb.getResultModel());
    }

    @Test
    public void wildcardSearchFindsWordHead() throws RepositoryException {
        build(session, content);
        session.save();

        TextSearchBuilder tsb = new TextSearchBuilder();
        tsb.setText("tit");
        tsb.setWildcardSearch(true);
        BrowserSearchResult result = tsb.getResultModel().getObject();
        QueryResult qr = result.getQueryResult();
        assertTrue(qr.getNodes().hasNext());
    }

    @Test
    public void excludedTypesAreNotFound() throws RepositoryException {
        build(session, content);
        session.save();

        TextSearchBuilder tsb = new TextSearchBuilder();
        tsb.setText("title");
        tsb.setExcludedPrimaryTypes(new String[] { "frontendtest:document" });
        BrowserSearchResult result = tsb.getResultModel().getObject();
        QueryResult qr = result.getQueryResult();
        assertFalse(qr.getNodes().hasNext());
    }

    @Test
    public void onlyDocumentsInScopeAreFound() throws RepositoryException {
        build(session, content);
        build(session, alternative);
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

}
