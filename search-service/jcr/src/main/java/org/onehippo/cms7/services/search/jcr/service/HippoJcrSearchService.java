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
package org.onehippo.cms7.services.search.jcr.service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryManager;

import org.onehippo.cms7.services.search.commons.query.InitialQueryImpl;
import org.onehippo.cms7.services.search.commons.query.QueryImpl;
import org.onehippo.cms7.services.search.jcr.HippoSearchNodeType;
import org.onehippo.cms7.services.search.jcr.query.JcrQueryBuilder;
import org.onehippo.cms7.services.search.jcr.query.JcrQueryVisitor;
import org.onehippo.cms7.services.search.jcr.result.JcrQueryResult;
import org.onehippo.cms7.services.search.query.InitialQuery;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.reflect.QueryNode;
import org.onehippo.cms7.services.search.query.reflect.QueryVisitor;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.QueryPersistService;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoJcrSearchService implements SearchService, QueryPersistService {

    private static final Logger log = LoggerFactory.getLogger(HippoJcrSearchService.class);

    public static final boolean DEFAULT_WILDCARD_POSTFIX_ENABLED = true;
    public static final int DEFAULT_WILDCARD_POSTFIX_MINLENGTH = 3;

    private boolean wildcardPostfixEnabled = DEFAULT_WILDCARD_POSTFIX_ENABLED;
    private int wildcardPostfixMinLength = DEFAULT_WILDCARD_POSTFIX_MINLENGTH;

    private Session session;

    /**
     * Default constructor
     */
    public HippoJcrSearchService() {
    }

    /**
     * Parameterized constructor
     */
    public HippoJcrSearchService(final Session session, final boolean wildcardPostfixEnabled, final int wildcardPostfixMinLength) {
        this.session = session;
        this.wildcardPostfixEnabled = wildcardPostfixEnabled;
        this.wildcardPostfixMinLength = wildcardPostfixMinLength;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public InitialQuery createQuery() throws SearchServiceException {
        return new InitialQueryImpl();
    }

    @Override
    public QueryResult search(Query searchQuery) throws SearchServiceException {
        if (!(searchQuery instanceof QueryImpl)) {
            throw new IllegalArgumentException("Search service only accepts queries created by itself");
        }
        QueryNode query = (QueryNode) searchQuery;
        final JcrQueryBuilder queryBuilder = new JcrQueryBuilder(session, this.isWildcardPostfixEnabled(), this.getWildcardPostfixMinLength());
        query.accept(new JcrQueryVisitor(queryBuilder, session));
        final String queryString = queryBuilder.getQueryString();
        try {
            long start = System.currentTimeMillis();
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final javax.jcr.query.Query jcrQuery = queryManager.createQuery(queryString, javax.jcr.query.Query.XPATH);
            jcrQuery.setOffset(queryBuilder.getOffset());
            if (queryBuilder.getLimit() >= 0) {
                jcrQuery.setLimit(queryBuilder.getLimit());
            }
            final javax.jcr.query.QueryResult jcrQueryResult = jcrQuery.execute();
            log.info("Executing query took '{}' ms. Executed query is : {} ", (System.currentTimeMillis() - start) , queryString);
            return new JcrQueryResult(jcrQueryResult);
        } catch (InvalidQueryException e) {
            throw new SearchServiceException("Invalid query '" + queryString + "'", e);
        } catch (RepositoryException e) {
            throw new SearchServiceException("Could not execute query '" + queryString + "'", e);
        }
    }

    @Override
    public QueryNode asQueryNode(final Query searchQuery) throws SearchServiceException {
        if (!(searchQuery instanceof QueryImpl)) {
            throw new IllegalArgumentException("Search service only accepts queries created by itself");
        }
        return (QueryNode) searchQuery;
    }


    @Override
    public void persist(String id, final QueryNode query) throws SearchServiceException {
        try {
            Node node = session.getNodeByIdentifier(id);
            if (!node.isNodeType(HippoSearchNodeType.NT_QUERY)) {
                throw new SearchServiceException("Invalid node type");
            }
            QueryVisitor persistingVisitor = new JcrPersistingQueryVisitor(node);
            query.accept(persistingVisitor);
        } catch (RepositoryException re) {
            throw new SearchServiceException(re);
        }
    }

    @Override
    public QueryNode retrieve(final String queryId) throws SearchServiceException {
        try {
            Node queryNode = session.getNodeByIdentifier(queryId);
            JcrQueryReader reader = new JcrQueryReader(queryNode);
            return (QueryNode) reader.buildQuery(new InitialQueryImpl());
        } catch (RepositoryException re) {
            throw new SearchServiceException(re);
        }
    }

    public boolean isWildcardPostfixEnabled() {
        return wildcardPostfixEnabled;
    }

    public int getWildcardPostfixMinLength() {
        return wildcardPostfixMinLength;
    }

}
