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
import org.hippoecm.repository.util.DateTools;

class ConstraintBuilderAdapter extends ConstraintBuilder {

    protected ConstraintBuilderAdapter() {
        super();
    }

    @Override
    protected Filter doBuild(final Session session, final DateTools.Resolution defaultResolution) throws FilterException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder equalTo(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder equalTo(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder equalToCaseInsensitive(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder notEqualTo(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder notEqualTo(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder notEqualToCaseInsensitive(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder greaterOrEqualThan(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder greaterOrEqualThan(final Calendar calendar, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder greaterThan(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder greaterThan(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder lessOrEqualThan(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder lessOrEqualThan(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder lessThan(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder lessThan(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder contains(final String fullTextSearch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder notContains(final String fullTextSearch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder between(final Object value1, final Object value2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder between(final Calendar start, final Calendar end, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder notBetween(final Object value1, final Object value2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder notBetween(final Calendar start, final Calendar end, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder like(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder notLike(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder exists() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder notExists() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintBuilder jcrExpression(final String jcrExpression) {
        throw new UnsupportedOperationException();
    }
}
