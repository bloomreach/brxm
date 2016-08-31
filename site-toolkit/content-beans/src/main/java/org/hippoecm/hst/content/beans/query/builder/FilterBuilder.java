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

public abstract class FilterBuilder {

    private HstQueryBuilder queryBuilder;
    private boolean negated;

    protected FilterBuilder() {
    }

    public final Filter build(final HstQueryBuilder queryBuilder, final Session session) throws FilterException {
        Filter filter = doBuild(queryBuilder, session);

        if (filter != null && negated()) {
            filter.negate();
        }

        return filter;
    }

    protected abstract Filter doBuild(final HstQueryBuilder queryBuilder, final Session session) throws FilterException;

    public FilterBuilder queryBuilder(final HstQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        return this;
    }

    protected HstQueryBuilder queryBuilder() {
        return queryBuilder;
    }

    public FilterBuilder negate() {
        this.negated = !negated;
        return this;
    }

    protected boolean negated() {
        return negated;
    }

    public abstract FilterBuilder equalTo(Object value);

    public abstract FilterBuilder equalTo(Calendar value, DateTools.Resolution dateResolution);

    public abstract FilterBuilder equalToCaseInsensitive(String value);

    public abstract FilterBuilder notEqualTo(Object value);

    public abstract FilterBuilder notEqualTo(Calendar value, DateTools.Resolution dateResolution);

    public abstract FilterBuilder notEqualToCaseInsensitive(String value);

    public abstract FilterBuilder greaterOrEqualThan(Object value);

    public abstract FilterBuilder greaterOrEqualThan(Calendar value, DateTools.Resolution dateResolution);

    public abstract FilterBuilder greaterThan(Object value);

    public abstract FilterBuilder greaterThan(Calendar value, DateTools.Resolution dateResolution);

    public abstract FilterBuilder lessOrEqualThan(Object value);

    public abstract FilterBuilder lessOrEqualThan(Calendar value, DateTools.Resolution dateResolution);

    public abstract FilterBuilder lessThan(Object value);

    public abstract FilterBuilder lessThan(Calendar value, DateTools.Resolution dateResolution);

    public abstract FilterBuilder contains(String fullTextSearch);

    public abstract FilterBuilder notContains(String fullTextSearch);

    public abstract FilterBuilder between(Object value1, Object value2);

    public abstract FilterBuilder between(Calendar start, Calendar end, DateTools.Resolution dateResolution);

    public abstract FilterBuilder notBetween(Object value1, Object value2);

    public abstract FilterBuilder notBetween(Calendar start, Calendar end, DateTools.Resolution dateResolution);

    public abstract FilterBuilder like(String value);

    public abstract FilterBuilder notLike(String value);

    public abstract FilterBuilder notNull();

    public abstract FilterBuilder isNull();

    public abstract FilterBuilder jcrExpression(String jcrExpression);

}
