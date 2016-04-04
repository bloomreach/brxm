/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.search.jcr.service.HippoJcrSearchService;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms7.services.search.query.QueryUtils.text;

public class TestReturnParentNodeSortingExpectations extends RepositoryTestCase {

    private HippoJcrSearchService searchService;

    @Before
    final public void createTestData() throws RepositoryException {
        String[] content = {
                "/test", "nt:unstructured",
                    "/test/content", "hippostd:folder",
                        "/test/content/doc1", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                            "sortprop", "1",
                            "/test/content/doc1/doc1", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", "live",
                                "sortprop", "2",
                            "/test/content/doc1/doc1", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", "preview",
                                "sortprop", "4",
                        "/test/content/doc2", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                            "sortprop", "2",
                            "/test/content/doc2/doc2", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", "live",
                                "sortprop", "1",
                        "/test/content/doc3", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                            "sortprop", "3",
                            "/test/content/doc3/doc3", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", "live",
                                "sortprop", "3"
        };

        build(content, session);

        session.save();
    }

    @Before
    final public void createSearchService() {
        searchService = new HippoJcrSearchService();
        searchService.setSession(session);
    }

    private String getNodeName(Hit hit) throws RepositoryException {
        return session.getNodeByIdentifier(hit.getSearchDocument().getContentId().toIdentifier()).getName();
    }

    @Test
    public void expect_sorting_on_variant_property() throws RepositoryException {
        Query searchQuery = searchService.createQuery()
                .ofType("hippo:document")
                .where(text("hippo:availability").contains("live"))
                .orderBy("sortprop").ascending();
        QueryResult queryResult = searchService.search(searchQuery);
        HitIterator hits = queryResult.getHits();

        assertEquals("doc2", getNodeName(hits.nextHit()));
        assertEquals("doc1", getNodeName(hits.nextHit()));
        assertEquals("doc3", getNodeName(hits.nextHit()));

        searchQuery = searchService.createQuery()
                .ofType("hippo:document")
                .where(text("hippo:availability").contains("live"))
                .orderBy("sortprop").descending();
        queryResult = searchService.search(searchQuery);
        hits = queryResult.getHits();

        assertEquals("doc3", getNodeName(hits.nextHit()));
        assertEquals("doc1", getNodeName(hits.nextHit()));
        assertEquals("doc2", getNodeName(hits.nextHit()));
    }

    @Test
    public void expect_sorting_on_handle_property() throws RepositoryException  {
        Query searchQuery = searchService.createQuery()
                .ofType("hippo:document")
                .where(text("hippo:availability").contains("live"))
                .returnParentNode()
                .orderBy("sortprop").ascending();
        QueryResult queryResult = searchService.search(searchQuery);
        HitIterator hits = queryResult.getHits();

        assertEquals("doc1", getNodeName(hits.nextHit()));
        assertEquals("doc2", getNodeName(hits.nextHit()));
        assertEquals("doc3", getNodeName(hits.nextHit()));

        searchQuery = searchService.createQuery()
                .ofType("hippo:document")
                .where(text("hippo:availability").contains("live"))
                .returnParentNode()
                .orderBy("sortprop").descending();
        queryResult = searchService.search(searchQuery);
        hits = queryResult.getHits();

        assertEquals("doc3", getNodeName(hits.nextHit()));
        assertEquals("doc2", getNodeName(hits.nextHit()));
        assertEquals("doc1", getNodeName(hits.nextHit()));
    }

}
