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

import org.hippoecm.repository.util.DateTools;

abstract class AbstractFilterBuilderAdapter extends FilterBuilder {

    private HstQueryBuilder queryBuilder;

    protected AbstractFilterBuilderAdapter() {
        super();
    }

    @Override
    public FilterBuilder queryBuilder(final HstQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        return this;
    }

    public HstQueryBuilder queryBuilder() {
        return queryBuilder;
    }

    @Override
    public FilterBuilder equalTo(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder equalTo(Calendar value, DateTools.Resolution resolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder equalToCaseInsensitive(String value) {
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
    public FilterBuilder between(Calendar start, Calendar end, DateTools.Resolution resolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder like(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterBuilder jcrExpression(String jcrExpression) {
        throw new UnsupportedOperationException();
    }

}
