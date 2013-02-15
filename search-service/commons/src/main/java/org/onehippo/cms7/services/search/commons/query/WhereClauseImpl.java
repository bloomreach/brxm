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
package org.onehippo.cms7.services.search.commons.query;

import org.onehippo.cms7.services.search.query.WhereClause;
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.reflect.QueryVisitor;
import org.onehippo.cms7.services.search.query.reflect.WhereNode;

public class WhereClauseImpl extends QueryImpl implements WhereClause, WhereNode {

    private final Constraint constraint;

    WhereClauseImpl(final QueryImpl parent, Constraint constraint) {
        super(parent);
        this.constraint = constraint;
    }

    @Override
    public AndClauseImpl and(final Constraint constraint) {
        return new AndClauseImpl(this, constraint);
    }

    @Override
    public OrClauseImpl or(final Constraint constraint) {
        return new OrClauseImpl(this, constraint);
    }

    @Override
    public Constraint getConstraint() {
        return constraint;
    }

    @Override
    protected void onVisit(QueryVisitor visitor) {
        visitor.visit((WhereNode) this);
    }
}
