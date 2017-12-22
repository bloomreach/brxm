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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
    public void queryWordsWithApostrophe() {
        GeneralSearchBuilder gsb = new GeneralSearchBuilder();
        // set wildcards to true
        gsb.setWildcardSearch(true);
        gsb.setText(" doesn't  ");
        StringBuilder query = gsb.getQueryStringBuilder();
        String expectedQuery = "//element(*, hippo:document)" +
                "[(hippo:paths = 'cafebabe-cafe-babe-cafe-babecafebabe') and jcr:contains(.,'doesn''t')] order by @jcr:score descending";
    }

}
