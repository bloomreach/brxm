/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.security.auth.Subject;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path.Element;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.jackrabbit.HippoSharedItemStateManager;
import org.hippoecm.repository.jackrabbit.KeyValue;
import org.hippoecm.repository.query.lucene.AuthorizationFilter;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.query.lucene.FacetPropExistsQuery;
import org.hippoecm.repository.query.lucene.FacetResultCollector;
import org.hippoecm.repository.query.lucene.FacetsQuery;
import org.hippoecm.repository.query.lucene.FacetsQueryFilter;
import org.hippoecm.repository.query.lucene.FixedScoreSimilarity;
import org.hippoecm.repository.query.lucene.ServicingFieldNames;
import org.hippoecm.repository.query.lucene.ServicingIndexingConfiguration;
import org.hippoecm.repository.query.lucene.ServicingSearchIndex;
import org.hippoecm.repository.query.lucene.caching.CachedAuthorizationBitSet;
import org.hippoecm.repository.query.lucene.caching.CachedQueryBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class deprecated: never used anyway, but kept for reference because some examples of possible nice improvements
 *
 */
@Deprecated 
public class FacetedNavigationEngineFourthImpl extends ServicingSearchIndex
    implements FacetedNavigationEngine<FacetedNavigationEngineFourthImpl.QueryImpl, FacetedNavigationEngineFourthImpl.ContextImpl> {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    class QueryImpl extends FacetedNavigationEngine.Query {
        String xpath;

        public QueryImpl(String xpath) {
            this.xpath = xpath;
        }

        public String toString() {
            return xpath;
        }
    }

    class ResultImpl extends FacetedNavigationEngine.Result {
        int length;
        Iterator<NodeId> iter = null;

        ResultImpl(int length, Set<NodeId> result) {
            this.length = length;
            if (result != null) {
                this.iter = result.iterator();
            }
        }

        public int length() {
            return length;
        }

        public Iterator<NodeId> iterator() {
            return iter;
        }

        public String toString() {
            return getClass().getName() + "[length=" + length + "]";
        }
    }

    class ContextImpl extends FacetedNavigationEngine.Context {
        Session session;
        String userId;
        Subject subject;
        AuthorizationQuery authorizationQuery;
        NodeTypeManager ntMgr;

        ContextImpl(Session session, String userId, Subject subject, NodeTypeManager ntMgr) throws RepositoryException {
            this.session = session;
            this.userId = userId;
            this.subject = subject;
            this.ntMgr = ntMgr;
            this.authorizationQuery = new AuthorizationQuery(subject, getNamespaceMappings(),
                    (ServicingIndexingConfiguration) getIndexingConfig(), ntMgr, session);

        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, CachedAuthorizationBitSet> cachedAuthorizationBitSetsMap = new LRUMap(50);

    private Map<String, CachedQueryBitSet> cachedQueryBitSetsMap = new LRUMap(500);

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(FacetedNavigationEngine.class);

    public FacetedNavigationEngineFourthImpl() {
    }

    public ContextImpl prepare(String userId, Subject subject, List<QueryImpl> initialQueries, Session session)
            throws RepositoryException {
        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        ContextImpl contextImpl = new ContextImpl(session, userId, subject, ntMgr);
        return contextImpl;
    }

    public void unprepare(ContextImpl authorization) {
        // deliberate ignore
    }

    public void reload(Map<Name, String[]> facetValues) {
        // deliberate ignore
    }

    public boolean requiresReload() {
        return false;
    }

    public boolean requiresNotify() {
        return false;
    }

    public void notify(String docId, Map<Name, String[]> oldFacets, Map<Name, String[]> newFacets) {
        // deliberate ignore
    }

    public void purge() {
        // deliberate ignore
    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl contextImpl,
            List<KeyValue<String, String>> facetsQueryList, QueryImpl openQuery, Map<String, Map<String, Count>> resultset,
            Map<String,String> inheritedFilter, HitsRequested hitsRequested)
            throws UnsupportedOperationException {

        NamespaceMappings nsMappings = getNamespaceMappings();

        /*
         * facetsQuery: get the query for the facets that are asked for
         */
        FacetsQuery facetsQuery = new FacetsQuery(facetsQueryList, nsMappings);


        /*
         * initialQuery: get the query for initialQuery
         */
        org.apache.lucene.search.Query initialLuceneQuery = null;
        if (initialQuery != null && !initialQuery.xpath.equals("")) {
            initialLuceneQuery = new TermQuery(new Term(ServicingFieldNames.HIPPO_PATH, initialQuery.xpath));
        }

        BooleanQuery searchQuery = new BooleanQuery(true);

        if (facetsQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(facetsQuery.getQuery(), Occur.MUST);
        }

        if (initialLuceneQuery != null) {
            searchQuery.add(initialLuceneQuery, Occur.MUST);
        }

        FacetResultCollector collector = null;
        IndexReader indexReader = null;
        IndexSearcher searcher = null;
        try {
            /*
             * if getIndexReader(true) you will also get version storage index which
             * cannot be used for facet searches, therefore set 'false'
             */
            //indexReader = getIndex().getIndexReader();
            indexReader = getIndexReader(false);


            /*
             * authorizationFilter: get the filter for the nodes the person is allowed to see
             */
            Filter authorizationFilter = new AuthorizationFilter(indexReader, contextImpl.authorizationQuery, cachedAuthorizationBitSetsMap);

            /*
             * authorizationFilter: get the filter for the nodes the person is allowed to see
             */
            Filter facetsQueryFilter = new FacetsQueryFilter(indexReader, facetsQuery.getQuery(), cachedQueryBitSetsMap);
            //BooleanClause[] clauses = facetsQuery.getQuery().getClauses();
            //for(BooleanClause bc : clauses) {
            //    bc.getQuery().toString();
            //}


            searcher = new IndexSearcher(indexReader);
            searcher.setSimilarity(new FixedScoreSimilarity());
            // In principle, below, there is always one facet
            if (resultset != null) {
                for (String facet : resultset.keySet()) {
                    /*
                     * Nodes not having this facet, still should be counted if they are a hit
                     * in the query without this facet. Therefor, first get the count query without
                     * FacetPropExistsQuery.
                     */
                    long timestamp = 0;
                    timestamp = System.currentTimeMillis();
                    int numHits = searcher.search(searchQuery, authorizationFilter).length();
                    if (log.isDebugEnabled()) {
                        log.debug("lucene query no collector took: \t" + (System.currentTimeMillis() - timestamp)
                                + " ms for #" + numHits + ". Query: " + searchQuery.toString());
                    }
                    System.out.println("lucene query no collector took: \t" + (System.currentTimeMillis() - timestamp)
                            + " ms for #" + numHits + ". Query: " + searchQuery.toString());
                    StringBuffer propertyName = new StringBuffer();
                    Element[] pathElements = PathFactoryImpl.getInstance().create(facet).getElements();
                    propertyName.append(nsMappings.translatePropertyName(pathElements[pathElements.length-1].getName()));
                    for(int i=0; i<pathElements.length-1; i++) {
                        propertyName.append("/");
                        propertyName.append(nsMappings.translatePropertyName(pathElements[i].getName()));
                    }
                    /*
                     * facetPropExists: the node must have the property as facet
                     */
                    FacetPropExistsQuery facetPropExists = new FacetPropExistsQuery( new String(propertyName));
                    searchQuery.add(facetPropExists.getQuery(), Occur.MUST);

                    if (log.isDebugEnabled()) {
                        timestamp = System.currentTimeMillis();
                    }
                    collector = new FacetResultCollector(indexReader, new String(propertyName), resultset.get(facet), null,
                            hitsRequested);
                    searcher.search(searchQuery,authorizationFilter, collector);


                    // set the numHits value
                    collector.setNumhits(numHits);
                    if (log.isDebugEnabled()) {
                        log.debug("lucene query with collector took: \t" + (System.currentTimeMillis() - timestamp)
                                + " ms for #" + collector.getNumhits() + ". Query: " + searchQuery.toString());
                    }

                    System.out.println("lucene query with collector1 took: \t" + (System.currentTimeMillis() - timestamp)
                            + " ms for #" + collector.getNumhits() + ". Query: " + searchQuery.toString());

                }
            } else {
                // resultset is null, so search for HippoNodeType.HIPPO_RESULTSET
                long timestamp = 0;
                if (log.isDebugEnabled()) {
                    timestamp = System.currentTimeMillis();
                }
                collector = new FacetResultCollector(indexReader, null, null, null, hitsRequested);
                searcher.search(searchQuery, authorizationFilter, collector);
                if (log.isDebugEnabled()) {
                    log.debug("lucene query with collector took: \t" + (System.currentTimeMillis() - timestamp)
                            + " ms for #" + collector.getNumhits() + ". Query: " + searchQuery.toString());
                }

                System.out.println("lucene query with collector2 took: \t" + (System.currentTimeMillis() - timestamp)
                        + " ms for #" + collector.getNumhits() + ". Query: " + searchQuery.toString());

            }

        } catch (IllegalNameException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (indexReader != null) {
                try {
                    indexReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
        
      //  return this.new ResultImpl(collector.getNumhits(), collector.getHits());

    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
            List<KeyValue<String, String>> facetsQuery, QueryImpl openQuery, Map<String,String> inheritedFilter, HitsRequested hitsRequested) {
        return view(queryName, initialQuery, authorization, facetsQuery, openQuery, null, inheritedFilter, hitsRequested);
    }

    public QueryImpl parse(String query) {
        return this.new QueryImpl(query);
    }

    @Override
    protected void doInit() throws IOException {
        QueryHandlerContext context = getContext();
        HippoSharedItemStateManager stateMgr = (HippoSharedItemStateManager) context.getItemStateManager();
        stateMgr.repository.setFacetedNavigationEngine(this);
        super.doInit();

    }

    public org.hippoecm.repository.FacetedNavigationEngine.Result view(String queryName, QueryImpl initialQuery,
            ContextImpl authorization, List<KeyValue<String, String>> facetsQuery, List<FacetRange> rangeQuery,
            QueryImpl openQuery,
            Map<String, Map<String, org.hippoecm.repository.FacetedNavigationEngine.Count>> resultset,
            Map<String, String> inheritedFilter, HitsRequested hitsRequested) throws UnsupportedOperationException {
        // TODO Auto-generated method stub
        return null;
    }

}
