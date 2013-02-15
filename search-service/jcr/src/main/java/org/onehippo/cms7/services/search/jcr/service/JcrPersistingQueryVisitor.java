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

import java.util.Calendar;
import java.util.Stack;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.search.jcr.HippoSearchNodeType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JcrPersistingQueryVisitor implements QueryVisitor {

    static final Logger log = LoggerFactory.getLogger(JcrPersistingQueryVisitor.class);

    private final JcrQueryNode node;
    private final Stack<JcrConstraintNode> filter = new Stack<JcrConstraintNode>();

    JcrPersistingQueryVisitor(Node node) {
        this.node = new JcrQueryNode(node);
        filter.push(new JcrConstraintNode(node));
        this.node.clear();
    }

    @Override
    public void visit(final QueryNode query) {
        node.setLimit(query.getLimit());
        node.setOffset(query.getOffset());
    }

    @Override
    public void visit(final TypedQueryNode typedQuery) {
        node.setNodeType(typedQuery.getType());
    }

    @Override
    public void visit(final OrderNode orderClause) {
        String property = orderClause.getProperty();
        String order = orderClause.getOrder();
        node.addOrdering(new JcrQueryNode.Ordering(property, order));
    }

    @Override
    public void visit(final SelectNode selectClause) {
    }

    @Override
    public void visit(final ScopeNode scopeClause) {
        String path = scopeClause.getPath();
        if (scopeClause.isInclude()) {
            node.addInclude(path);
        } else {
            node.addExclude(path);
        }
    }

    @Override
    public void visit(final WhereNode whereClause) {
        Constraint constraint = whereClause.getConstraint();
        addSubConstraint(constraint);
    }

    @Override
    public void visit(final OrNode orclause) {
        JcrConstraintNode constraintNode = filter.peek();
        if (constraintNode.getType() == JcrConstraintNode.CompoundType.AND) {
            throw new IllegalStateException(
                    "Type of compound node was already set to 'AND'; mixing AND and OR clauses is not supported");
        }
        if (constraintNode.getType() == JcrConstraintNode.CompoundType.UNKNOWN) {
            constraintNode.setType(JcrConstraintNode.CompoundType.OR);
        }

        addSubConstraint(orclause.getConstraint());
    }

    @Override
    public void visit(final AndNode andclause) {
        JcrConstraintNode constraintNode = filter.peek();
        if (constraintNode.getType() == JcrConstraintNode.CompoundType.OR) {
            throw new IllegalStateException(
                    "Type of compound node was already set to 'OR'; mixing AND and OR clauses is not supported");
        }
        if (constraintNode.getType() == JcrConstraintNode.CompoundType.UNKNOWN) {
            constraintNode.setType(JcrConstraintNode.CompoundType.AND);
        }

        addSubConstraint(andclause.getConstraint());
    }

    private void addSubConstraint(final Constraint constraint) {
        boolean compound = (constraint instanceof CompoundConstraint);

        Node node = filter.peek().getNode();
        Node subNode;
        try {
            if (compound) {
                subNode = node.addNode(
                        HippoSearchNodeType.CONSTRAINT, HippoSearchNodeType.NT_COMPOUNDCONSTRAINT);
            } else {
                subNode = node.addNode(
                        HippoSearchNodeType.CONSTRAINT, HippoSearchNodeType.NT_PRIMITIVECONSTRAINT);
            }
        } catch (RepositoryException e) {
            log.error("Could not create child node to host constraint");
            return;
        }

        filter.push(new JcrConstraintNode(subNode));
        try {
            storeConstraint(constraint);
        } finally {
            filter.pop();
        }
    }

    void storeConstraint(Constraint constraint) {
        if (constraint instanceof NotConstraint) {
            storeNotConstraint((NotConstraint) constraint);
        } else if (constraint instanceof CompoundConstraint) {
            storeCompoundConstraint((CompoundConstraint) constraint);
        } else if (constraint instanceof TextConstraint) {
            storeTextConstraint((TextConstraint) constraint);
        } else if (constraint instanceof DateConstraint) {
            storeDateConstraint((DateConstraint) constraint);
        } else if (constraint instanceof IntegerConstraint) {
            storeIntegerConstraint((IntegerConstraint) constraint);
        } else if (constraint instanceof ExistsConstraint) {
            storeExistsConstraint((ExistsConstraint) constraint);
        } else {
            log.warn("Unknown constraint type");
        }
    }

    private void storeExistsConstraint(final ExistsConstraint constraint) {
        JcrConstraintNode constraintNode = filter.peek();
        constraintNode.addNotNull(constraint.getProperty());
    }

    private void storeIntegerConstraint(final IntegerConstraint constraint) {
        JcrConstraintNode constraintNode = filter.peek();
        String property = constraint.getProperty();
        int value = constraint.getValue();
        int upper = constraint.getUpper();
        switch (constraint.getType()) {
            case EQUAL:
                constraintNode.addEqualTo(property, value);
                break;
            case TO:
                constraintNode.addLessOrEqualThan(property, value);
                break;
            case FROM:
                constraintNode.addGreaterOrEqualThan(property, value);
                break;
            case BETWEEN:
                constraintNode.addBetween(property, value, upper);
                break;
        }
    }

    private void storeDateConstraint(final DateConstraint constraint) {
        JcrConstraintNode constraintNode = filter.peek();
        String property = constraint.getProperty();
        Calendar value = constraint.getValue();
        Calendar upper = constraint.getUpper();
        switch (constraint.getType()) {
            case EQUAL:
                constraintNode.addEqualTo(property, value);
                break;
            case TO:
                constraintNode.addLessOrEqualThan(property, value);
                break;
            case FROM:
                constraintNode.addGreaterOrEqualThan(property, value);
                break;
            case BETWEEN:
                constraintNode.addBetween(property, value, upper);
                break;
        }
        constraintNode.setResolution(constraint.getResolution());
    }

    private void storeTextConstraint(final TextConstraint constraint) {
        JcrConstraintNode constraintNode = filter.peek();
        String property = constraint.getProperty();
        if (property == null) {
            property = ".";
        }
        String value = constraint.getValue();
        switch (constraint.getType()) {
            case EQUAL:
                constraintNode.addEqualTo(property, value);
                break;
            case CONTAINS:
                constraintNode.addContains(property, value);
        }
    }

    private void storeNotConstraint(final NotConstraint constraint) {
        storeConstraint(constraint.getConstraint());
        filter.peek().negate();
    }

    private void storeCompoundConstraint(final CompoundConstraint constraint) {
        for (final Constraint subConstraint : constraint.getConstraints()) {
            if (subConstraint instanceof CompoundConstraint) {
                CompoundConstraint compoundConstraint = (CompoundConstraint) subConstraint;
                if (compoundConstraint.getType() == CompoundConstraint.Type.AND) {
                    filter.peek().setType(JcrConstraintNode.CompoundType.AND);
                } else {
                    filter.peek().setType(JcrConstraintNode.CompoundType.OR);
                }
            }
            addSubConstraint(subConstraint);
        }
    }

}
