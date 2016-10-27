/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.query;

public interface Query {

    Query EMPTY = new EmptyQuery();

    /**
     * Instructs the search service to return the de-duplicated set of parent nodes of the search results. This is
     * very convenient to obtain a set of all handles that have at least one variant that matches the queries
     * constraints. Note that properties selected using {@link TypedQuery#select(java.lang.String)} and properties
     * used for sorting using {@link #orderBy(java.lang.String)} must be at the parent's level.
     *
     * @return this query object
     */
    Query returnParentNode();

    Query offsetBy(int offset);

    Query limitTo(int limit);

    /**
     * Instructs the search service to sort the result using the given property. See also the note at
     * {@link #returnParentNode()} when used in combination with that setting.
     *
     * @param property name of property to sort on
     * @return this query object
     */
    OrderClause orderBy(String property);
}

final class EmptyQuery implements Query, OrderClause {

    @Override
    public Query returnParentNode() {
        return this;
    }

    @Override
    public Query offsetBy(final int offset) {
        return this;
    }

    @Override
    public Query limitTo(final int limit) {
        return this;
    }

    @Override
    public OrderClause orderBy(final String property) {
        return this;
    }

    @Override
    public Query ascending() {
        return this;
    }

    @Override
    public Query descending() {
        return this;
    }
}
