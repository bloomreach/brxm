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

import org.onehippo.cms7.services.search.query.ScopeClause;
import org.onehippo.cms7.services.search.query.reflect.QueryVisitor;
import org.onehippo.cms7.services.search.query.reflect.ScopeNode;

public class ScopeClauseImpl extends ScopedQueryImpl implements ScopeClause, ScopeNode {

    private final boolean include;
    private final String path;

    ScopeClauseImpl(final QueryImpl parent, String path) {
        this(parent, path, true);
    }

    ScopeClauseImpl(final QueryImpl parent, String path, boolean include) {
        super(parent);
        this.path = path;
        this.include = include;
    }

    @Override
    public ScopeClauseImpl or(final String path) {
        return new ScopeClauseImpl(this, path, true);
    }

    @Override
    public ScopeClauseImpl except(final String path) {
        return new ScopeClauseImpl(this, path, false);
    }

    @Override
    public boolean isInclude() {
        return include;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    protected void onVisit(QueryVisitor visitor) {
        visitor.visit((ScopeNode) this);
    }
}
