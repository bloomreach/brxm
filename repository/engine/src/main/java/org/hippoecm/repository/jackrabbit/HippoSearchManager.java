/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;

import org.apache.jackrabbit.api.stats.RepositoryStatistics;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.query.QueryHandlerFactory;
import org.apache.jackrabbit.core.query.QueryObjectModelImpl;
import org.apache.jackrabbit.core.query.lucene.JackrabbitIndexSearcher;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryFactory;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.query.lucene.join.QueryEngine;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.qom.ConstraintImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.stats.RepositoryStatisticsImpl;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search manager that adds an authorization constraints to each query that is executed.
 * This is relevant for queries built with the Query Object Model (QOM) or languages that
 * are implemented using the QOM.  (e.g. SQL2 support is implemented this way)
 */
public class HippoSearchManager extends SearchManager {

    private static final Logger log = LoggerFactory.getLogger(HippoSearchManager.class);

    public HippoSearchManager(final String workspace,
                              final RepositoryContext repositoryContext,
                              final QueryHandlerFactory qhf,
                              final SharedItemStateManager itemMgr,
                              final PersistenceManager pm,
                              final NodeId rootNodeId,
                              final SearchManager parentMgr,
                              final NodeId excludedNodeId) throws RepositoryException {
        super(workspace, repositoryContext, qhf, itemMgr, pm, rootNodeId, parentMgr, excludedNodeId);
    }

    @Override
    public QueryObjectModel createQueryObjectModel(final SessionContext sessionContext,
                                                   final QueryObjectModelTree qomTree,
                                                   final String language,
                                                   final Node node) throws InvalidQueryException, RepositoryException {
        QueryObjectModelImpl qom = new HippoQueryObjectModelImpl();
        qom.init(sessionContext, getQueryHandler(), qomTree, language, node);
        return qom;
    }

    private static class AuthorizationConstraint extends ConstraintImpl implements PropertyExistence {

        private String selectorName;

        public AuthorizationConstraint(final SessionContext sessionContext, final String selectorName) {
            super(sessionContext);
            this.selectorName = selectorName;
        }

        @Override
        public Object accept(final QOMTreeVisitor visitor, final Object data) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSelectorName() {
            return selectorName;
        }

        @Override
        public String getPropertyName() {
            return "hippo:dummy";
        }
    }

    private static class HippoQueryObjectModelImpl extends QueryObjectModelImpl {

        private final Map<String, Value> variables = new HashMap<String, Value>();

        @Override
        public QueryResult execute() throws RepositoryException {

            long time = System.nanoTime();
            final LuceneQueryFactory lqf = new AuthorizedLuceneQueryFactory(sessionContext.getSessionImpl(),
                                                                            (SearchIndex) handler, variables);

            final QueryEngine engine = new QueryEngine(sessionContext.getSessionImpl(), lqf, variables);
            final HippoQueryObjectModelFactoryImpl factory = new HippoQueryObjectModelFactoryImpl(sessionContext);

            final Constraint constraint = getConstraint();
            final Constraint authorizationConstraint = getAuthorizationConstraint(factory, getSource());

            final Constraint fullConstraint;
            if (constraint != null) {
                fullConstraint = new HippoQueryObjectModelFactoryImpl(sessionContext).and(constraint, authorizationConstraint);
            } else {
                fullConstraint = authorizationConstraint;
            }

            final QueryResult result = engine.execute(getColumns(), getSource(), fullConstraint, getOrderings(), offset, limit);
            time = System.nanoTime() - time;
            final long timeMs = time / 1000000;
            log.debug("executed in {} ms. ({})", timeMs, statement);
            RepositoryStatisticsImpl statistics = sessionContext
                    .getRepositoryContext().getRepositoryStatistics();
            statistics.getCounter(RepositoryStatistics.Type.QUERY_COUNT).incrementAndGet();
            statistics.getCounter(RepositoryStatistics.Type.QUERY_DURATION).addAndGet(timeMs);
            sessionContext.getRepositoryContext().getStatManager().getQueryStat()
                    .logQuery(language, statement, timeMs);
            return result;
        }

        private Constraint getAuthorizationConstraint(final HippoQueryObjectModelFactoryImpl factory, final Source source)
                throws RepositoryException {
            final Constraint constraint;
            if (source instanceof Join) {
                final Join join = (Join) getSource();
                final Constraint leftAuthorization = getAuthorizationConstraint(factory, join.getLeft());
                final Constraint rightAuthorization = getAuthorizationConstraint(factory, join.getRight());
                constraint = factory.and(leftAuthorization, rightAuthorization);
            } else {
                final Selector selector = (Selector) source;
                constraint = new AuthorizationConstraint(sessionContext, selector.getSelectorName());
            }
            return constraint;
        }

        @Override
        public void bindValue(String varName, Value value) throws IllegalArgumentException {
            super.bindValue(varName, value);
            variables.put(varName, value);
        }

    }

    private static class AuthorizedLuceneQueryFactory extends LuceneQueryFactory {

        private final SessionImpl sessionImpl;

        public AuthorizedLuceneQueryFactory(final SessionImpl session, final SearchIndex index, final Map<String, Value> bindVariables)
                throws RepositoryException {
            super(session, index, bindVariables);
            this.sessionImpl = session;
        }

        @Override
        protected Query create(final Constraint constraint, final Map<String, NodeType> selectorMap, final JackrabbitIndexSearcher searcher)
                throws RepositoryException, IOException {
            if (constraint instanceof AuthorizationConstraint) {
                if (sessionImpl instanceof InternalHippoSession) {
                    return ((InternalHippoSession) sessionImpl).getAuthorizationQuery().getQuery();
                } else {
                    return new BooleanQuery();
                }
            } else {
                return super.create(constraint, selectorMap, searcher);
            }
        }
    }

    private static class HippoQueryObjectModelFactoryImpl extends QueryObjectModelFactoryImpl {

        public HippoQueryObjectModelFactoryImpl(final NamePathResolver resolver) {
            super(resolver);
        }

        @Override
        protected QueryObjectModel createQuery(final QueryObjectModelTree qomTree) throws UnsupportedRepositoryOperationException {
            throw new UnsupportedRepositoryOperationException();
        }
    }

}
