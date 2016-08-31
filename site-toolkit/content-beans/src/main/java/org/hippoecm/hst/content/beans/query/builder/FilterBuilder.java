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

    protected FilterBuilder() {
    }

    public abstract Filter build(final HstQueryBuilder queryBuilder, final Session session) throws FilterException;

    public abstract FilterBuilder queryBuilder(final HstQueryBuilder queryBuilder);

    public abstract FilterBuilder equalTo(Object value);

    public abstract FilterBuilder equalTo(Calendar value, DateTools.Resolution resolution);

    public abstract FilterBuilder equalToCaseInsensitive(String value);

    public abstract FilterBuilder contains(String fullTextSearch);

    public abstract FilterBuilder notContains(String fullTextSearch);

    public abstract FilterBuilder between(Object value1, Object value2);

    public abstract FilterBuilder between(Calendar start, Calendar end, DateTools.Resolution resolution);

    public abstract FilterBuilder like(String value);

    public abstract FilterBuilder jcrExpression(String jcrExpression);

}
