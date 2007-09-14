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
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.OrderQueryNode;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.query.QueryNodeFactory;
import org.apache.jackrabbit.core.query.lucene.LazyQueryResultImpl;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryBuilder;
import org.apache.jackrabbit.core.query.lucene.QueryImpl;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.query.lucene.WorkspaceTraversalResult;
import org.apache.jackrabbit.name.QName;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicingQueryImpl extends QueryImpl {
    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(ServicingQueryImpl.class);
    
    public ServicingQueryImpl(SessionImpl session,
            ItemManager itemMgr,
            SearchIndex index,
            PropertyTypeRegistry propReg,
            String statement,
            String language,
            QueryNodeFactory factory) throws InvalidQueryException {
        super(session, itemMgr, index, propReg, statement, language, factory);
    }
    
    /**
     * Executes this query and returns a <code>{@link QueryResult}</code>.
     *
     * @param offset the offset in the total result set
     * @param limit the maximum result size
     * @return a <code>QueryResult</code>
     * @throws RepositoryException if an error occurs
     */
    public QueryResult execute(long offset, long limit, boolean facetView) throws RepositoryException {
        if (log.isDebugEnabled()) {
            log.debug("Executing query: \n" + root.dump());
        }

        // check for special query
        if (allNodesQueryNode.equals(root)) {
            return new WorkspaceTraversalResult(session,
                    new QName[] { QName.JCR_PRIMARYTYPE, QName.JCR_PATH, QName.JCR_SCORE },
                    session.getNamespaceResolver());
        }

        // build lucene query
        Query query = LuceneQueryBuilder.createQuery(root, session,
                index.getContext().getItemStateManager(), index.getNamespaceMappings(),
                index.getTextAnalyzer(), propReg, index.getSynonymProvider());

        OrderQueryNode orderNode = root.getOrderNode();

        OrderQueryNode.OrderSpec[] orderSpecs;
        if (orderNode != null) {
            orderSpecs = orderNode.getOrderSpecs();
        } else {
            orderSpecs = new OrderQueryNode.OrderSpec[0];
        }
        QName[] orderProperties = new QName[orderSpecs.length];
        boolean[] ascSpecs = new boolean[orderSpecs.length];
        for (int i = 0; i < orderSpecs.length; i++) {
            orderProperties[i] = orderSpecs[i].getProperty();
            ascSpecs[i] = orderSpecs[i].isAscending();
        }
        
        if(facetView) { 
            return new FacetQueryResultImpl(index, itemMgr, session.getNamespaceResolver(),
                  session.getAccessManager(), this, query, getSelectProperties(),
                  orderProperties, ascSpecs, super.getRespectDocumentOrder(), offset, limit);
        }else{
           return new LazyQueryResultImpl(index, itemMgr, session.getNamespaceResolver(),
                   session.getAccessManager(), this, query, getSelectProperties(),
                   orderProperties, ascSpecs, super.getRespectDocumentOrder(), offset, limit);
           }
       
    }
    
//    
//    public ServicingQueryImpl(SessionImpl session, ItemManager itemMgr, SearchIndex index,
//            PropertyTypeRegistry propReg, String statement, String language) throws InvalidQueryException {
//        super(session, itemMgr, index, propReg, statement, language);
//    }
//    
//    /**
//     * Executes this query and returns a <code>{@link QueryResult}</code>.
//     *
//     * @param offset the offset in the total result set
//     * @param limit the maximum result size
//     * @return a <code>QueryResult</code>
//     * @throws RepositoryException if an error occurs
//     */
//    public QueryResult execute(long offset, long limit, boolean facetView) throws RepositoryException {
//        super.execute(offset, limit);
//
//    }
}
