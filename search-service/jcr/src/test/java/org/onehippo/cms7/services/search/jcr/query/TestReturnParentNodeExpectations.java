/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms7.services.search.query.QueryUtils.text;

public class TestReturnParentNodeExpectations extends RepositoryTestCase {

    private HippoJcrSearchService searchService;

    @Before
    final public void createTestData() throws RepositoryException {
        String[] content = {
                "/test", "nt:unstructured",
                    "/test/content", "hippostd:folder",
                        "/test/content/document-with-properties", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                            "myproperty", "handle",
                            "/test/content/document-with-properties/document-with-properties", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", "live",
                                "myproperty", "live",
                                "/test/content/document-with-properties/document-with-properties/description", "hippostd:html",
                                    "hippostd:content", "live variant",
                            "/test/content/document-with-properties/document-with-properties", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", "preview",
                                "myproperty", "preview",
                                "/test/content/document-with-properties/document-with-properties[2]/description", "hippostd:html",
                                    "hippostd:content", "preview variant",
        };

        build(content, session);

        session.save();
    }

    @Before
    final public void createSearchService() {
        searchService = new HippoJcrSearchService();
        searchService.setSession(session);
    }

    @Test
    public void expect_properties_of_variant_to_be_returned() {
        Query searchQuery = searchService.createQuery()
                .ofType("hippo:document")
                .select("myproperty")
                .where(text("hippo:availability").contains("live"));
        QueryResult queryResult = searchService.search(searchQuery);

        assertEquals("live", queryResult.getHits().nextHit().getSearchDocument().getFieldValue("myproperty"));
    }

    @Test
    public void expect_properties_of_handle_to_be_returned() {
        Query searchQuery = searchService.createQuery()
                .ofType("hippo:document")
                .select("myproperty")
                .where(text("hippo:availability").contains("live"))
                .returnParentNode();
        QueryResult queryResult = searchService.search(searchQuery);

        assertEquals("handle", queryResult.getHits().nextHit().getSearchDocument().getFieldValue("myproperty"));
    }

}
