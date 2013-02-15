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
package org.onehippo.cms7.services.search.commons.query;

import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.reflect.QueryNode;
import org.onehippo.cms7.services.search.query.reflect.QueryVisitor;
import org.onehippo.cms7.services.search.query.reflect.StringQueryVisitor;

public class QueryImpl implements Query, QueryNode {

    private final QueryImpl parent;

    private final int offset;
    private final int limit;

    QueryImpl(QueryImpl parent) {
        this.parent = parent;
        this.offset = -1;
        this.limit = -1;
    }

    QueryImpl(QueryImpl parent, int offset, int limit) {
        this.parent = parent;
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public QueryImpl offsetBy(final int offset) {
        return new QueryImpl(this, offset, this.limit);
    }

    @Override
    public QueryImpl limitTo(final int limit) {
        return new QueryImpl(this, this.offset, limit);
    }

    @Override
    public OrderClauseImpl orderBy(final String property) {
        return new OrderClauseImpl(this, property);
    }

    @Override
    public final void accept(QueryVisitor visitor) {
        if (parent != null) {
            parent.accept(visitor);
        }
        onVisit(visitor);
    }

    protected void onVisit(QueryVisitor visitor) {
        visitor.visit(this);
    }

    protected final QueryImpl getParent() {
        return parent;
    }

    @Override
    public final int getLimit() {
        return limit;
    }

    @Override
    public final int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        StringQueryVisitor visitor = new StringQueryVisitor();
        accept(visitor);
        return visitor.getString();
    }
}
