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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.search.jcr.HippoSearchNodeType;
import org.onehippo.cms7.services.search.query.AndClause;
import org.onehippo.cms7.services.search.query.InitialQuery;
import org.onehippo.cms7.services.search.query.OrClause;
import org.onehippo.cms7.services.search.query.OrderClause;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.ScopeClause;
import org.onehippo.cms7.services.search.query.ScopedQuery;
import org.onehippo.cms7.services.search.query.TypedQuery;
import org.onehippo.cms7.services.search.query.WhereClause;
import org.onehippo.cms7.services.search.query.constraint.Constraint;

public class JcrQueryReader {

    private final JcrQueryNode node;

    public JcrQueryReader(Node node) {
        this.node = new JcrQueryNode(node);
    }

    public Query buildQuery(InitialQuery initialQuery) throws RepositoryException {
        ScopeClause from = null;
        for (String include : node.getIncludes()) {
            if (from == null) {
                from = initialQuery.from(include);
            } else {
                from = from.or(include);
            }
        }
        for (String exclude : node.getExcludes()) {
            from = from.except(exclude);
        }

        ScopedQuery scoped = from;
        if (scoped == null) {
            scoped = initialQuery;
        }

        TypedQuery typed;
        String nodeType = node.getNodeType();
        if (nodeType != null) {
            typed = scoped.ofType(nodeType);
        } else {
            typed = scoped;
        }

        WhereClause where = null;
        AndClause and = null;
        OrClause or = null;
        for (Node childNode : new NodeIterable(node.getNode().getNodes(HippoSearchNodeType.CONSTRAINT))) {
            JcrConstraintNode constraintNode = new JcrConstraintNode(childNode);
            Constraint constraint = constraintNode.getConstraint();
            if (constraint == null) {
                continue;
            }

            switch (node.getType()) {
                case AND:
                    and = (and != null ? and.and(constraint) : typed.where(constraint));
                    break;
                case OR:
                    or = (or != null ? or.or(constraint) : typed.where(constraint));
                    break;
                case UNKNOWN:
                    where = typed.where(constraint);
                    break;
            }
            if (node.getType() == JcrConstraintNode.CompoundType.UNKNOWN) {
                break;
            }
        }

        Query query;
        if (where != null) {
            query = where;
        } else if (and != null) {
            query = and;
        } else if (or != null) {
            query = or;
        } else {
            query = typed;
        }

        for (JcrQueryNode.Ordering ordering : node.getOrderings()) {
            OrderClause clause = query.orderBy(ordering.getProperty());
            if ("ascending".equals(ordering.getOrdering())) {
                query = clause.ascending();
            } else {
                query = clause.descending();
            }
        }

        query = query.limitTo(node.getLimit()).offsetBy(node.getOffset());

        return query;
    }
}
