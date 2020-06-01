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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.content.beans.query.builder.FilterConstraint.Operator;
import org.hippoecm.repository.util.DateTools;

public class FieldConstraintBuilder extends ConstraintBuilder {

    private final String fieldName;

    private List<FilterConstraint> filterConstraints;

    protected FieldConstraintBuilder(final String fieldName) {
        super();
        this.fieldName = fieldName;
    }


    String fieldName() {
        return fieldName;
    }


    List<FilterConstraint> getFilterConstraints() {
        return filterConstraints;
    }

    @Override
    public Constraint equalTo(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.EQUAL, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint equalTo(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.EQUAL, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint equalToCaseInsensitive(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.EQUAL, value).caseSensitive(false);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint notEqualTo(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_EQUAL, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint notEqualTo(final Calendar value, DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_EQUAL, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint notEqualToCaseInsensitive(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_EQUAL, value).caseSensitive(false);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint greaterOrEqualThan(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.GE, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint greaterOrEqualThan(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.GE, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint greaterThan(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.GT, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint greaterThan(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.GT, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint lessOrEqualThan(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.LE, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint lessOrEqualThan(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.LE, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint lessThan(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.LT, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint lessThan(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.LT, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint contains(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.CONTAINS, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint notContains(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_CONTAINS, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint between(final Object value1, final Object value2) {
        if (value1 == null || value2 == null) {
            // trigger noop constraint
            FilterConstraint constraint = new FilterConstraint(Operator.BETWEEN, null);
            addFilterConstraint(constraint);
            return new FieldConstraint(this);
        }
        FilterConstraint constraint = new FilterConstraint(Operator.BETWEEN, new Object[]{value1, value2});
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint between(final Calendar start, final Calendar end, final DateTools.Resolution dateResolution) {
        if (start == null || end == null) {
            // trigger noop constraint
            FilterConstraint constraint = new FilterConstraint(Operator.BETWEEN, null).dateResolution(dateResolution);
            addFilterConstraint(constraint);
            return new FieldConstraint(this);
        }
        FilterConstraint constraint = new FilterConstraint(Operator.BETWEEN, new Object[]{start, end}).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint notBetween(final Object value1, final Object value2) {
        if (value1 == null || value2 == null) {
            // trigger noop constraint
            FilterConstraint constraint = new FilterConstraint(Operator.NOT_BETWEEN, null);
            addFilterConstraint(constraint);
            return new FieldConstraint(this);
        }
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_BETWEEN, new Object[]{value1, value2});
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint notBetween(final Calendar start, final Calendar end, final DateTools.Resolution dateResolution) {
        if (start == null || end == null) {
            // trigger noop constraint
            FilterConstraint constraint = new FilterConstraint(Operator.NOT_BETWEEN, null).dateResolution(dateResolution);
            addFilterConstraint(constraint);
            return new FieldConstraint(this);
        }
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_BETWEEN, new Object[]{start, end}).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint like(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.LIKE, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint notLike(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_LIKE, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint exists() {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_NULL);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint notExists() {
        FilterConstraint constraint = new FilterConstraint(Operator.IS_NULL);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    @Override
    public Constraint jcrExpression(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.XPATH_EXPRESSION, value);
        addFilterConstraint(constraint);
        return new FieldConstraint(this);
    }

    private void addFilterConstraint(final FilterConstraint filterConstraint) {
        if (filterConstraints == null) {
            filterConstraints = new ArrayList<>();
        }

        filterConstraints.add(filterConstraint);
    }
}
