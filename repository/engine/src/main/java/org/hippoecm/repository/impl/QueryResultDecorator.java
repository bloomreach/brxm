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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.core.query.lucene.QueryResultImpl;
import org.hippoecm.repository.query.lucene.HippoQueryResult;

public class QueryResultDecorator extends SessionBoundDecorator implements QueryResult {

    protected final QueryResult result;
    protected long totalSize;

    public static QueryResult unwrap(final QueryResult decorated) {
        if(decorated instanceof QueryResultDecorator) {
            return ((QueryResultDecorator)decorated).result;
        }
        return decorated;
    }

    QueryResultDecorator(final SessionDecorator session, final QueryResult result) {
        super(session);
        this.result = unwrap(result);
        if (result instanceof HippoQueryResult) {
            totalSize = ((HippoQueryResult)result).getSizeTotal();
        } else if (result instanceof QueryResultImpl) {
            totalSize = ((QueryResultImpl)result).getTotalSize();
        } else {
            totalSize = -1L;
        }
    }

    public String[] getColumnNames() throws RepositoryException {
        return result.getColumnNames();
    }

    public RowIterator getRows() throws RepositoryException {
        return new RowIteratorDecorator(session, result.getRows());
    }

    public NodeIterator getNodes() throws RepositoryException {
        return new NodeIteratorDecorator(session, result.getNodes(), totalSize);
    }

    public String[] getSelectorNames() throws RepositoryException {
        return result.getSelectorNames();
    }
}
