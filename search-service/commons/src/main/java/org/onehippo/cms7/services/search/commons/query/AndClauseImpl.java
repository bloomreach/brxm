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

import org.onehippo.cms7.services.search.query.AndClause;
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.reflect.AndNode;
import org.onehippo.cms7.services.search.query.reflect.QueryVisitor;

public class AndClauseImpl extends QueryImpl implements AndClause, AndNode {

    private final Constraint constraint;

    AndClauseImpl(final QueryImpl parent, Constraint constraint) {
        super(parent);
        this.constraint = constraint;
    }

    @Override
    public AndClauseImpl and(final Constraint constraint) {
        return new AndClauseImpl(this, constraint);
    }

    @Override
    public final Constraint getConstraint() {
        return constraint;
    }

    @Override
    protected void onVisit(QueryVisitor visitor) {
        visitor.visit((AndNode) this);
    }

}
