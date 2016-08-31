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
    protected Filter doBuild(HstQueryBuilder queryBuilder, Session session) throws FilterException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder equalTo(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder equalTo(Calendar value, DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder equalToCaseInsensitive(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notEqualTo(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notEqualTo(Calendar value, DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notEqualToCaseInsensitive(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder greaterOrEqualThan(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder greaterOrEqualThan(Calendar calendar, DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder greaterThan(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder greaterThan(Calendar value, DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder lessOrEqualThan(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder lessOrEqualThan(Calendar value, DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder lessThan(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder lessThan(Calendar value, DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder contains(String fullTextSearch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notContains(String fullTextSearch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder between(Object value1, Object value2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder between(Calendar start, Calendar end, DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notBetween(Object value1, Object value2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notBetween(Calendar start, Calendar end, DateTools.Resolution dateResolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder like(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder notLike(String value) {
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
    public FilterBuilder jcrExpression(String jcrExpression) {
        throw new UnsupportedOperationException();
    }
}
