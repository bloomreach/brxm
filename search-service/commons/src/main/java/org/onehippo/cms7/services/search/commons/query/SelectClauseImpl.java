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

import org.onehippo.cms7.services.search.query.SelectClause;
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.reflect.QueryVisitor;
import org.onehippo.cms7.services.search.query.reflect.SelectNode;

public class SelectClauseImpl extends QueryImpl implements SelectClause, SelectNode {

    private final String property;

    SelectClauseImpl(final QueryImpl parent, String property) {
        super(parent);
        this.property = property;
    }

    @Override
    public SelectClauseImpl and(final String property) {
        return new SelectClauseImpl(this, property);
    }

    @Override
    public WhereClauseImpl where(final Constraint constraint) {
        return new WhereClauseImpl(this, constraint);
    }

    @Override
    public final String getProperty() {
        return property;
    }

    @Override
    protected void onVisit(QueryVisitor visitor) {
        visitor.visit((SelectNode) this);
    }
}
