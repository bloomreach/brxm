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
package org.onehippo.cms7.services.search.jcr.query;

import java.util.Calendar;
import java.util.Iterator;

import javax.jcr.Session;

import org.onehippo.cms7.services.search.query.reflect.QueryVisitor;
import org.onehippo.cms7.services.search.query.constraint.CompoundConstraint;
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.constraint.DateConstraint;
import org.onehippo.cms7.services.search.query.constraint.ExistsConstraint;
import org.onehippo.cms7.services.search.query.constraint.IntegerConstraint;
import org.onehippo.cms7.services.search.query.constraint.NotConstraint;
import org.onehippo.cms7.services.search.query.constraint.TextConstraint;
import org.onehippo.cms7.services.search.query.reflect.AndNode;
import org.onehippo.cms7.services.search.query.reflect.OrNode;
import org.onehippo.cms7.services.search.query.reflect.OrderNode;
import org.onehippo.cms7.services.search.query.reflect.QueryNode;
import org.onehippo.cms7.services.search.query.reflect.ScopeNode;
import org.onehippo.cms7.services.search.query.reflect.SelectNode;
import org.onehippo.cms7.services.search.query.reflect.TypedQueryNode;
import org.onehippo.cms7.services.search.query.reflect.WhereNode;

public class JcrQueryVisitor implements QueryVisitor {

    private Filter filter;
    private final JcrQueryBuilder builder;
    private final Session session;

    public JcrQueryVisitor(final JcrQueryBuilder queryBuilder, final Session session) {
        this.builder = queryBuilder;
        this.session = session;
    }

    @Override
    public void visit(final QueryNode query) {
        if (query.getLimit() != -1) {
            builder.setLimit(query.getLimit());
        }
        if (query.getOffset() != -1) {
            builder.setOffset(query.getOffset());
        }
    }

    @Override
    public void visit(final TypedQueryNode typedQuery) {
        if (typedQuery.getType() != null) {
            if (builder.getNodeType() != null) {
                throw new IllegalStateException("node type was already specified");
            }
            builder.setNodeType(typedQuery.getType());
        }
    }

    @Override
    public void visit(final OrNode orclause) {
        if (this.filter == null) {
            throw new IllegalStateException("Where clause was already completed");
        }
        try {
            filter.addOrFilter(getFilter(orclause.getConstraint()));
        } catch (JcrQueryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(final AndNode andclause) {
        if (this.filter == null) {
            throw new IllegalStateException("Where clause was already completed");
        }
        try {
            filter.addAndFilter(getFilter(andclause.getConstraint()));
        } catch (JcrQueryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(final OrderNode orderClause) {
        builder.addOrderBy("@" + orderClause.getProperty() + " " + orderClause.getOrder());
    }

    @Override
    public void visit(final SelectNode selectClause) {
        builder.addSelect(selectClause.getProperty());
    }

    @Override
    public void visit(final WhereNode whereClause) {
        if (this.filter != null) {
            throw new IllegalStateException("Where clause was already specified");
        }
        try {
            this.filter = getFilter(whereClause.getConstraint());
            builder.setFilter(filter);
        } catch (JcrQueryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(final ScopeNode scopeClause) {
        if (scopeClause.isInclude()) {
            builder.addScope(scopeClause.getPath());
        } else {
            builder.addExclusion(scopeClause.getPath());
        }
    }

    Filter getFilter(Constraint constraint) throws JcrQueryException {
        if (constraint instanceof NotConstraint) {
            return getFilter((NotConstraint) constraint);
        } else if (constraint instanceof CompoundConstraint) {
            return getFilter((CompoundConstraint) constraint);
        } else if (constraint instanceof TextConstraint) {
            return getFilter((TextConstraint) constraint);
        } else if (constraint instanceof DateConstraint) {
            return getFilter((DateConstraint) constraint);
        } else if (constraint instanceof IntegerConstraint) {
            return getFilter((IntegerConstraint) constraint);
        } else if (constraint instanceof ExistsConstraint) {
            return getFilter((ExistsConstraint) constraint);
        } else {
            throw new JcrQueryException("Unknown constraint type");
        }
    }

    private Filter getFilter(final ExistsConstraint constraint) throws JcrQueryException {
        Filter filter = new Filter(session);
        filter.addNotNull(constraint.getProperty());
        return filter;
    }

    private Filter getFilter(final IntegerConstraint constraint) throws JcrQueryException {
        Filter filter = new Filter(session);
        String property = constraint.getProperty();
        int value = constraint.getValue();
        int upper = constraint.getUpper();
        switch (constraint.getType()) {
            case EQUAL:
                filter.addEqualTo(property, value);
                break;
            case TO:
                filter.addLessOrEqualThan(property, value);
                break;
            case FROM:
                filter.addGreaterOrEqualThan(property, value);
                break;
            case BETWEEN:
                filter.addBetween(property, value, upper);
                break;
        }
        return filter;
    }

    private Filter getFilter(final DateConstraint constraint) throws JcrQueryException {
        Filter filter = new Filter(session);
        String property = constraint.getProperty();
        Calendar value = constraint.getValue();
        Calendar upper = constraint.getUpper();
        final DateConstraint.Resolution resolution = constraint.getResolution();
        switch (constraint.getType()) {
            case EQUAL:
                filter.addEqualTo(property, value, resolution);
                break;
            case TO:
                filter.addLessOrEqualThan(property, value, resolution);
                break;
            case FROM:
                filter.addGreaterOrEqualThan(property, value, resolution);
                break;
            case BETWEEN:
                filter.addBetween(property, value, upper, resolution);
                break;
        }
        return filter;
    }

    private Filter getFilter(final TextConstraint constraint) throws JcrQueryException {
        Filter filter = new Filter(session);
        String property = constraint.getProperty();
        if (property == null) {
            property = ".";
        }
        String value = constraint.getValue();
        switch (constraint.getType()) {
            case EQUAL:
                filter.addEqualTo(property, value);
                break;
            case CONTAINS:
                filter.addContains(property, value);
        }
        return filter;
    }

    private Filter getFilter(final CompoundConstraint constraint) throws JcrQueryException {
        Iterator<Constraint> constraints = constraint.getConstraints().iterator();
        Filter filter = getFilter(constraints.next());
        while (constraints.hasNext()) {
            Constraint subConstraint = constraints.next();
            switch (constraint.getType()) {
                case OR:
                    filter.addOrFilter(getFilter(subConstraint));
                    break;
                case AND:
                    filter.addAndFilter(getFilter(subConstraint));
                    break;
            }
        }
        return filter;
    }

    private Filter getFilter(final NotConstraint constraint) throws JcrQueryException {
        Filter filter = getFilter(constraint.getConstraint());
        filter.negate();
        return filter;
    }

}
