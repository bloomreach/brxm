/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.lucene;

import java.io.IOException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.query.lucene.LazyQueryResultImpl;
import org.apache.jackrabbit.core.query.lucene.QueryHits;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.lucene.search.Query;

public class FacetQueryResultImpl extends LazyQueryResultImpl implements QueryResult {

    public FacetQueryResultImpl(SearchIndex index, ItemManager itemMgr, NamespaceResolver namespaceResolver, AccessManager accessManager, ServicingQueryImpl impl, Query query, QName[] selectProperties, QName[] orderProperties, boolean[] ascSpecs, boolean respectDocumentOrder, long offset, long limit) throws RepositoryException {
       super(index,itemMgr,namespaceResolver,accessManager,impl,query,selectProperties,orderProperties,ascSpecs,respectDocumentOrder,offset,limit);
    }

    @Override
    protected QueryHits executeQuery() throws IOException {
        return super.executeQuery();
    }

    @Override
    public String[] getColumnNames() throws RepositoryException {
        return super.getColumnNames();
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        return super.getNodes();
    }

    @Override
    public RowIterator getRows() throws RepositoryException {
        return super.getRows();
    }

}
