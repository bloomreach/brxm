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
package org.onehippo.cms7.services.search.jcr.service;

import java.util.Collection;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.search.document.SearchDocument;
import org.onehippo.cms7.services.search.jcr.HippoSearchNodeType;
import org.onehippo.cms7.services.search.query.InitialQuery;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.constraint.DateConstraint;
import org.onehippo.cms7.services.search.query.reflect.QueryNode;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.search.query.QueryUtils.date;
import static org.onehippo.cms7.services.search.query.QueryUtils.integer;
import static org.onehippo.cms7.services.search.query.QueryUtils.not;
import static org.onehippo.cms7.services.search.query.QueryUtils.text;

public class TestHippoJcrSearchService extends RepositoryTestCase {

    private HippoJcrSearchService searchService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        searchService = new HippoJcrSearchService();
        searchService.setSession(session);
    }

    @Test
    public void testSearch() throws Exception {
        Query searchQuery = searchService.createQuery().ofType("testproject:basedocument").where(text().contains("homepage"));
        assertNotNull(searchQuery);

        QueryResult queryResult = searchService.search(searchQuery);
        assertTrue("Total hit count is zero", queryResult.getTotalHitCount() > 0);

        final Hit hit = queryResult.getHits().next();
        final SearchDocument searchDocument = hit.getSearchDocument();
        final String primaryTypeName = searchDocument.getPrimaryTypeName();
        assertEquals("testproject:textpage", primaryTypeName);
    }

    @Test
    public void testSelect() throws Exception {
        Query searchQuery = searchService.createQuery().ofType("testproject:basedocument").select("title");
        assertNotNull(searchQuery);

        QueryResult queryResult = searchService.search(searchQuery);
        assertTrue("Total hit count is zero", queryResult.getTotalHitCount() > 0);

        final Hit hit = queryResult.getHits().next();
        final SearchDocument searchDocument = hit.getSearchDocument();
        final Collection<String> fieldNames = searchDocument.getFieldNames();
        assertEquals(1, fieldNames.size());
        assertEquals("title", fieldNames.iterator().next());
    }

    @Test
    public void testPersist() throws Exception {
        Query searchQuery = searchService.createQuery().ofType("testproject:basedocument").where(text().contains("homepage"));
        assertNotNull(searchQuery);

        Node queryNode = createQueryNode();
        searchService.persist(queryNode.getIdentifier(), searchService.asQueryNode(searchQuery));

        assertEquals("testproject:basedocument", queryNode.getProperty(HippoSearchNodeType.NODETYPE).getString());
        assertEquals(1, queryNode.getNodes().getSize());

        Node constraintNode = queryNode.getNode(HippoSearchNodeType.CONSTRAINT);
        assertFalse(constraintNode.hasProperty(HippoSearchNodeType.PROPERTY));
        assertEquals("homepage", constraintNode.getProperty(HippoSearchNodeType.VALUE).getString());
        assertEquals("contains", constraintNode.getProperty(HippoSearchNodeType.RELATION).getString());
    }

    @Test
    public void testPersistRetrieve() throws Exception {
        InitialQuery initialQuery = searchService.createQuery();
        testPersistRetrieveQuery(initialQuery.from("/content"));
        testPersistRetrieveQuery(initialQuery.ofType("testproject:basedocument"));
        testPersistRetrieveQuery(initialQuery.where(not(text("author").isEqualTo("admin"))));
        testPersistRetrieveQuery(initialQuery.where(text("title").contains("hello")));
        testPersistRetrieveQuery(initialQuery.where(text("title").isEqualTo("world")));
        testPersistRetrieveQuery(initialQuery.limitTo(10).offsetBy(5));
        testPersistRetrieveQuery(initialQuery.orderBy("author").descending());
        testPersistRetrieveQuery(initialQuery.where(integer("count").from(5).andTo(10)));
        testPersistRetrieveQuery(initialQuery.where(date("publication").from(new Date(), DateConstraint.Resolution.HOUR)));
        testPersistRetrieveQuery(initialQuery.where(date("publication").from(new Date(), DateConstraint.Resolution.EXACT)));
    }

    private void testPersistRetrieveQuery(Query searchQuery) throws Exception {
        Node node = createQueryNode();
        try {
            QueryNode queryNode = searchService.asQueryNode(searchQuery);
            searchService.persist(node.getIdentifier(), queryNode);

            QueryNode retrieved = searchService.retrieve(node.getIdentifier());
            assertEquals(queryNode.toString(), retrieved.toString());
        } finally {
            node.remove();
        }
    }

    private Node createQueryNode() throws RepositoryException {
        return session.getRootNode().addNode("testQuery", HippoSearchNodeType.NT_QUERY);
    }

}
