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
package org.onehippo.cms7.services.search.query;

import org.onehippo.cms7.services.search.query.constraint.AndConstraint;
import org.onehippo.cms7.services.search.query.constraint.CompoundConstraint;
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.constraint.NotConstraint;
import org.onehippo.cms7.services.search.query.constraint.OrConstraint;
import org.onehippo.cms7.services.search.query.field.DateField;
import org.onehippo.cms7.services.search.query.field.IntegerField;
import org.onehippo.cms7.services.search.query.field.TextField;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.SearchService;

public abstract class QueryBuilder {

    protected QueryBuilder() {
    }

    protected final TextField text(String property) {
        return new TextField(property);
    }

    protected final TextField text() {
        return new TextField();
    }

    protected final DateField date(String property) {
        return new DateField(property);
    }

    protected final IntegerField integer(String property) {
        return new IntegerField(property);
    }

    protected final OrConstraint either(Constraint constraint) {
        return new CompoundConstraint(constraint, CompoundConstraint.Type.OR);
    }

    protected final AndConstraint both(Constraint constraint) {
        return new CompoundConstraint(constraint, CompoundConstraint.Type.AND);
    }

    protected final Constraint not(Constraint constraint) {
        return new NotConstraint(constraint);
    }

}
