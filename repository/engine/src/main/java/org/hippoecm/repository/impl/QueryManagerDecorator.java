/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.QueryObjectModelFactory;

public class QueryManagerDecorator extends SessionBoundDecorator implements QueryManager {

    protected final QueryManager manager;

    public static QueryManager unwrap(final QueryManager manager) {
        if (manager instanceof QueryManagerDecorator) {
            return ((QueryManagerDecorator)manager).manager;
        }
        return manager;
    }

    QueryManagerDecorator(final SessionDecorator session, final QueryManager manager) {
        super(session);
        this.manager = unwrap(manager);
    }

    public QueryDecorator createQuery(String statement, final String language) throws InvalidQueryException, RepositoryException {
        statement = QueryDecorator.mangleArguments(statement);
        return new QueryDecorator(session, manager.createQuery(statement, language));
    }

    public Query getQuery(Node node) throws InvalidQueryException, RepositoryException {
        return new QueryDecorator(session, manager.getQuery(NodeDecorator.unwrap(node)), node);
    }

    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return manager.getSupportedQueryLanguages();
    }

    public QueryObjectModelFactory getQOMFactory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
