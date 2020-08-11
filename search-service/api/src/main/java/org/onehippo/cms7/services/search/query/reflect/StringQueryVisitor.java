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

import org.onehippo.cms7.services.search.query.reflect.AndNode;
import org.onehippo.cms7.services.search.query.reflect.OrNode;
import org.onehippo.cms7.services.search.query.reflect.OrderNode;
import org.onehippo.cms7.services.search.query.reflect.QueryNode;
import org.onehippo.cms7.services.search.query.reflect.QueryVisitor;
import org.onehippo.cms7.services.search.query.reflect.ScopeNode;
import org.onehippo.cms7.services.search.query.reflect.SelectNode;
import org.onehippo.cms7.services.search.query.reflect.TypedQueryNode;
import org.onehippo.cms7.services.search.query.reflect.WhereNode;

public class StringQueryVisitor implements QueryVisitor {

    private StringBuilder sb = new StringBuilder();
    private boolean scopeStarted;
    private boolean selectStarted;

    @Override
    public void visit(final QueryNode query) {
        if (query.getOffset() != -1) {
            sb.append(" offset ");
            sb.append(query.getOffset());
        }
        if (query.getLimit() != -1) {
            sb.append(" limit ");
            sb.append(query.getLimit());
        }
    }

    @Override
    public void visit(final TypedQueryNode typedQuery) {
        sb.append(" of type ");
        sb.append(typedQuery.getType());
    }

    @Override
    public void visit(final OrNode orclause) {
        sb.append(" or ");
        sb.append(orclause.getConstraint());
    }

    @Override
    public void visit(final AndNode andclause) {
        sb.append(" and ");
        sb.append(andclause.getConstraint());
    }

    @Override
    public void visit(final OrderNode orderClause) {
        sb.append(" order by ");
        sb.append(orderClause.getProperty());
        sb.append(" ");
        sb.append(orderClause.getOrder());
    }

    @Override
    public void visit(final SelectNode selectClause) {
        if (!this.selectStarted) {
            sb.append(" select ");
            selectStarted = true;
        } else {
            sb.append(" and ");
        }
        sb.append(selectClause.getProperty());
    }

    @Override
    public void visit(final WhereNode whereClause) {
        sb.append(" where ");
        sb.append(whereClause.getConstraint());
    }

    @Override
    public void visit(final ScopeNode scopeClause) {
        if (!this.scopeStarted) {
            sb.append(" from ");
            scopeStarted = true;
        } else {
            if (scopeClause.isInclude()) {
                sb.append(" or ");
            } else {
                sb.append(" except ");
            }
        }
        sb.append(scopeClause.getPath());
    }

    public String getString() {
        return sb.toString();
    }
}
