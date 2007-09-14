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

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.lucene.document.Document;

public class ServicingSearchIndex extends SearchIndex {

    public ServicingSearchIndex() {
        super();
    }

    @Override
    protected Document createDocument(NodeState node, NamespaceMappings nsMappings) throws RepositoryException {
        ServicingNodeIndexer indexer = new ServicingNodeIndexer(node,
                getContext().getItemStateManager(), nsMappings, super.getTextExtractor());
        indexer.setSupportHighlighting(super.getSupportHighlighting());
        indexer.setServicingIndexingConfiguration((ServicingIndexingConfiguration)super.getIndexingConfig());
        Document doc = indexer.createDoc(); 
        mergeAggregatedNodeIndexes(node, doc);
        return doc;
    }

    @Override
    public ExecutableQuery createExecutableQuery(SessionImpl session, ItemManager itemMgr, String statement,
            String language) throws InvalidQueryException {
        /*
         * Here we need our own ServicingQueryImpl which is capable of
         * facetted navigation
         */ 
        ServicingQueryImpl query = new ServicingQueryImpl(session, itemMgr, this,
                getContext().getPropertyTypeRegistry(), statement, language, getQueryNodeFactory());
        query.setRespectDocumentOrder(super.getRespectDocumentOrder());
        return query;
        
        //return super.createExecutableQuery(session, itemMgr, statement, language);
    }

    
}
