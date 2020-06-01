/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.core.query.lucene.QueryResultImpl;
import org.hippoecm.repository.decorating.AbstractDecorator;
import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.query.lucene.HippoQueryResult;

public class QueryResultDecorator extends AbstractDecorator implements QueryResult {

    protected final QueryResult result;
    protected long totalSize;

    protected QueryResultDecorator(DecoratorFactory factory, SessionDecorator session, QueryResult result) {
        super(factory, session);
        this.result = result;
        QueryResult impl = org.hippoecm.repository.decorating.QueryResultDecorator.unwrap(result);
        if (impl instanceof HippoQueryResult) {
            totalSize = ((HippoQueryResult)impl).getSizeTotal();
        } else if (impl instanceof QueryResultImpl) {
            totalSize = ((QueryResultImpl)org.hippoecm.repository.decorating.QueryResultDecorator.unwrap(result)).getTotalSize();
        } else {
            totalSize = -1L;
        }
    }
    /**
     * @inheritDoc
     */
    public String[] getColumnNames() throws RepositoryException {
        return result.getColumnNames();
    }

    /**
     * @inheritDoc
     */
    public RowIterator getRows() throws RepositoryException {
        return new RowIteratorDecorator(factory, session, result.getRows());
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes() throws RepositoryException {
        return new NodeIteratorDecorator(factory, session, result.getNodes(), totalSize);
    }

    public String[] getSelectorNames() throws RepositoryException {
        return result.getSelectorNames();
    }

}
