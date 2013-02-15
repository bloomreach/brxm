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
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.constraint.OrConstraint;
import org.onehippo.cms7.services.search.query.field.DateField;
import org.onehippo.cms7.services.search.query.field.IntegerField;
import org.onehippo.cms7.services.search.query.field.TextField;

public class QueryUtils {

    private static final QueryBuilder BUILDER = new QueryBuilder() {};

    private QueryUtils() {
    }

    public static TextField text(String property) {
        return BUILDER.text(property);
    }

    public static TextField text() {
        return BUILDER.text();
    }

    public static DateField date(String property) {
        return BUILDER.date(property);
    }

    public static IntegerField integer(String property) {
        return BUILDER.integer(property);
    }

    public static OrConstraint either(Constraint constraint) {
        return BUILDER.either(constraint);
    }

    public static AndConstraint both(Constraint constraint) {
        return BUILDER.both(constraint);
    }

    public static Constraint not(Constraint constraint) {
        return BUILDER.not(constraint);
    }
}
