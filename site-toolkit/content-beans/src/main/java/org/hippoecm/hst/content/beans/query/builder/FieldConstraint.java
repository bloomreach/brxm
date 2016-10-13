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

import java.util.Calendar;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.repository.util.DateTools;

public class FieldConstraint extends Constraint {

    private FieldConstraintBuilder builder;

    public FieldConstraint(final FieldConstraintBuilder builder) {
        super();
        this.builder = builder;
    }

    @Override
    protected Filter doBuild(final Session session, final DateTools.Resolution defaultResolution) throws FilterException {

        final Filter filter = new FilterImpl(session, defaultResolution);

        FilterConstraint.Operator operator;
        Object value;

        boolean realConstraintFound = false;

        for (FilterConstraint constraint : builder.getFilterConstraints()) {
            if (constraint.isNoop()) {
                // an 'null' constraint was set, for example  .where(constraint(".").contains(null))
                continue;
            }
            realConstraintFound = true;
            operator = constraint.operator();
            value = constraint.value();

            if (operator == FilterConstraint.Operator.EQUAL) {
                if (value instanceof Calendar && constraint.dateResolution() != null) {
                    filter.addEqualTo(builder.fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    if (value instanceof String && !constraint.caseSensitive()) {
                        filter.addEqualToCaseInsensitive(builder.fieldName(), (String)value);
                    } else {
                        filter.addEqualTo(builder.fieldName(), value);
                    }
                }
            } else if (operator == FilterConstraint.Operator.GE) {
                if (value instanceof Calendar && constraint.dateResolution() != null) {
                    filter.addGreaterOrEqualThan(builder.fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    filter.addGreaterOrEqualThan(builder.fieldName(), value);
                }
            } else if (operator == FilterConstraint.Operator.GT) {
                if (value instanceof Calendar && constraint.dateResolution() != null) {
                    filter.addGreaterThan(builder.fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    filter.addGreaterThan(builder.fieldName(), value);
                }
            } else if (operator == FilterConstraint.Operator.LE) {
                if (value instanceof Calendar && constraint.dateResolution() != null) {
                    filter.addLessOrEqualThan(builder.fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    filter.addLessOrEqualThan(builder.fieldName(), value);
                }
            } else if (operator == FilterConstraint.Operator.LT) {
                if (value instanceof Calendar && constraint.dateResolution() != null) {
                    filter.addLessThan(builder.fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    filter.addLessThan(builder.fieldName(), value);
                }
            } else if (operator == FilterConstraint.Operator.CONTAINS) {
                filter.addContains(builder.fieldName(), (String)value);
            } else if (operator == FilterConstraint.Operator.LIKE) {
                filter.addLike(builder.fieldName(), (String)value);
            } else if (operator == FilterConstraint.Operator.BETWEEN) {
                Object[] values = (Object[])value;

                if (values[0] instanceof Calendar && constraint.dateResolution() != null) {
                    filter.addBetween(builder.fieldName(), (Calendar)values[0], (Calendar)values[1], constraint.dateResolution());
                } else {
                    filter.addBetween(builder.fieldName(), values[0], values[1]);
                }
            } else if (operator == FilterConstraint.Operator.NOT_EQUAL) {
                if (value instanceof Calendar && constraint.dateResolution() != null) {
                    filter.addNotEqualTo(builder.fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    if (value instanceof String && !constraint.caseSensitive()) {
                        filter.addNotEqualToCaseInsensitive(builder.fieldName(), (String)value);
                    } else {
                        filter.addNotEqualTo(builder.fieldName(), value);
                    }
                }
            } else if (operator == FilterConstraint.Operator.NOT_LIKE) {
                filter.addNotLike(builder.fieldName(), (String)value);
            } else if (operator == FilterConstraint.Operator.NOT_NULL) {
                filter.addNotNull(builder.fieldName());
            } else if (operator == FilterConstraint.Operator.IS_NULL) {
                filter.addIsNull(builder.fieldName());
            } else if (operator == FilterConstraint.Operator.NOT_CONTAINS) {
                filter.addNotContains(builder.fieldName(), (String)value);
            } else if (operator == FilterConstraint.Operator.NOT_BETWEEN) {
                Object[] values = (Object[])value;

                if (values[0] instanceof Calendar) {
                    filter.addNotBetween(builder.fieldName(), (Calendar)values[0], (Calendar)values[1], constraint.dateResolution());
                } else {
                    filter.addNotBetween(builder.fieldName(), values[0], values[1]);
                }
            } else if (operator == FilterConstraint.Operator.XPATH_EXPRESSION) {
                filter.addJCRExpression((String)value);
            }
        }

        if (!realConstraintFound) {
            return null;
        }
        return filter;
    }

}
