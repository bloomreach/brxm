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
package org.onehippo.cms7.services.search.query.reflect;

import org.onehippo.cms7.services.search.query.AndClause;
import org.onehippo.cms7.services.search.query.InitialQuery;
import org.onehippo.cms7.services.search.query.OrClause;
import org.onehippo.cms7.services.search.query.OrderClause;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.ScopeClause;
import org.onehippo.cms7.services.search.query.ScopedQuery;
import org.onehippo.cms7.services.search.query.TypedQuery;
import org.onehippo.cms7.services.search.query.steps.WhereStep;

public class QueryBuilderVisitor implements QueryVisitor {

    private Query query;

    public QueryBuilderVisitor(InitialQuery initial, QueryNode node) {
        this.query = initial;
        node.accept(this);
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public void visit(final QueryNode queryNode) {
        this.query = query.limitTo(queryNode.getLimit()).offsetBy(queryNode.getOffset());
    }

    @Override
    public void visit(final TypedQueryNode typedQuery) {
        this.query = ((ScopedQuery) query).ofType(typedQuery.getType());
    }

    @Override
    public void visit(final OrNode orclause) {
        this.query = ((OrClause) query).or(orclause.getConstraint());
    }

    @Override
    public void visit(final AndNode andclause) {
        this.query = ((AndClause) query).and(andclause.getConstraint());
    }

    @Override
    public void visit(final OrderNode orderNode) {
        OrderClause clause = query.orderBy(orderNode.getProperty());
        if ("ascending".equals(orderNode.getOrder())) {
            query = clause.ascending();
        } else {
            query = clause.descending();
        }
    }

    @Override
    public void visit(final SelectNode selectNode) {
        this.query = ((TypedQuery) query).select(selectNode.getProperty());
    }

    @Override
    public void visit(final WhereNode whereNode) {
        this.query = ((WhereStep) query).where(whereNode.getConstraint());
    }

    @Override
    public void visit(final ScopeNode scopeClause) {
        if (query instanceof InitialQuery) {
            this.query = ((InitialQuery) query).from(scopeClause.getPath());
        } else if (query instanceof ScopeClause) {
            if (scopeClause.isInclude()) {
                this.query = ((ScopeClause) query).or(scopeClause.getPath());
            } else {
                this.query = ((ScopeClause) query).except(scopeClause.getPath());
            }
        }
    }
}
