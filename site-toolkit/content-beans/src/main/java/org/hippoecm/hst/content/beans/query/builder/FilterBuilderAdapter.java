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

class FilterBuilderAdapter extends FilterBuilder {

    protected FilterBuilderAdapter() {
        super();
    }

    @Override
    protected Filter doBuild(final HstQueryBuilder queryBuilder, final Session session) throws FilterException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder equalTo(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder equalTo(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder equalToCaseInsensitive(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notEqualTo(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notEqualTo(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notEqualToCaseInsensitive(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder greaterOrEqualThan(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder greaterOrEqualThan(final Calendar calendar, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder greaterThan(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder greaterThan(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder lessOrEqualThan(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder lessOrEqualThan(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder lessThan(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder lessThan(final Calendar value, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder contains(final String fullTextSearch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notContains(final String fullTextSearch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder between(final Object value1, final Object value2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder between(final Calendar start, final Calendar end, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notBetween(final Object value1, final Object value2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notBetween(final Calendar start, final Calendar end, final DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder like(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notLike(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notNull() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder isNull() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder jcrExpression(final String jcrExpression) {
        throw new UnsupportedOperationException();
    }
}
