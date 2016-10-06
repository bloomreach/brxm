/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.content.beans.query.builder;

import org.hippoecm.repository.util.DateTools;

class FilterConstraint {

    enum Operator {
        EQUAL,
        NOT_EQUAL,
        GT, // Greater than
        GE, // Greater than or equal to
        LT, // Less than
        LE, // Less than or equal to
        BETWEEN,
        NOT_BETWEEN,
        CONTAINS,
        NOT_CONTAINS,
        LIKE,
        NOT_LIKE,
        IS_NULL,
        NOT_NULL,
        XPATH_EXPRESSION;
    }

    private Operator operator;
    private Object value;
    private DateTools.Resolution dateResolution;
    private boolean caseSensitive = true;
    private boolean noop;

    public FilterConstraint(Operator operator) {
        this(operator, null, false);
    }

    public FilterConstraint(final Operator operator, final Object value) {
        this(operator, value, value == null);
    }

    private FilterConstraint(final Operator operator, final Object value, final boolean noop) {
        this.operator = operator;
        this.value = value;
        this.noop = noop;
    }

    public Operator operator() {
        return operator;
    }

    public Object value() {
        return value;
    }

    public FilterConstraint dateResolution(final DateTools.Resolution dateResolution) {
        this.dateResolution = dateResolution;
        return this;
    }

    public DateTools.Resolution dateResolution() {
        return dateResolution;
    }

    public FilterConstraint caseSensitive(final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }

    public boolean caseSensitive() {
        return caseSensitive;
    }

    public boolean isNoop() {
        return noop;
    }
}
