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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.jackrabbit.HippoSharedItemStateManager;
import org.hippoecm.repository.jackrabbit.KeyValue;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.query.lucene.FacetPropExistsQuery;
import org.hippoecm.repository.query.lucene.FacetRangeQuery;
import org.hippoecm.repository.query.lucene.FacetResultCollector;
import org.hippoecm.repository.query.lucene.FacetsQuery;
import org.hippoecm.repository.query.lucene.FixedScoreSimilarity;
import org.hippoecm.repository.query.lucene.HippoDateTools;
import org.hippoecm.repository.query.lucene.InheritedFilterQuery;
import org.hippoecm.repository.query.lucene.ServicingFieldNames;
import org.hippoecm.repository.query.lucene.ServicingIndexingConfiguration;
import org.hippoecm.repository.query.lucene.ServicingNameFormat;
import org.hippoecm.repository.query.lucene.ServicingSearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedNavigationEngineThirdImpl extends ServicingSearchIndex
    implements FacetedNavigationEngine<FacetedNavigationEngineThirdImpl.QueryImpl, FacetedNavigationEngineThirdImpl.ContextImpl> {

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
        AuthorizationQuery authorizationQuery;

        ContextImpl(Session session, String userId, Subject subject, NodeTypeManager ntMgr)
                throws RepositoryException {
            this.authorizationQuery = new AuthorizationQuery(subject, getNamespaceMappings(),
                    (ServicingIndexingConfiguration) getIndexingConfig(), ntMgr, session);
        }
    }

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(FacetedNavigationEngine.class);

    public FacetedNavigationEngineThirdImpl() {
    }

    public ContextImpl prepare(String userId, Subject subject, List<QueryImpl> initialQueries,
            Session session) throws RepositoryException {
        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        return new ContextImpl(session, userId, subject, ntMgr);
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
            List<KeyValue<String, String>> facetsQueryList, List<FacetRange> rangeQuery, QueryImpl openQuery, Map<String, Map<String, Count>> resultset,
            Map<Name,String> inheritedFilter, HitsRequested hitsRequested)
            throws UnsupportedOperationException {
        NamespaceMappings nsMappings = getNamespaceMappings();

        /*
         * facetsQuery: get the query for the facets that are asked for
         */
        FacetsQuery facetsQuery = new FacetsQuery(facetsQueryList, nsMappings);
        
        /*
         * facetRangeQuery : get the query for the ranges of facet values
         */
        FacetRangeQuery facetRangeQuery = new FacetRangeQuery(rangeQuery, nsMappings,  this);
         
        /*
         * inheritedFilter: get the query representation of the interited filters (for example from facetselect)
         */
        
        InheritedFilterQuery inheritedFilterQuery = new InheritedFilterQuery(inheritedFilter, nsMappings,
                (ServicingIndexingConfiguration) getIndexingConfig());

        
        
        /*
         * initialQuery: get the query for initialQuery. This is the hippo:docbase value. 
         */
        org.apache.lucene.search.Query initialLuceneQuery = null;
        if (initialQuery != null && !initialQuery.xpath.equals("")) {
            initialLuceneQuery = new TermQuery(new Term(ServicingFieldNames.HIPPO_PATH, initialQuery.xpath));
        }

        /*
         * authorizationQuery: get the query for the facets the person is allowed to see (which
         * is again a facetsQuery)
         */

        BooleanQuery searchQuery = new BooleanQuery(true);
        if (facetsQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(facetsQuery.getQuery(), Occur.MUST);
        }
        
        if(facetRangeQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(facetRangeQuery.getQuery(), Occur.MUST);
        }
        
        if(inheritedFilterQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(inheritedFilterQuery.getQuery(), Occur.MUST);
        }
        // TODO perhaps create cached user specific filter for authorisation to gain speed
        if (contextImpl.authorizationQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(contextImpl.authorizationQuery.getQuery(), Occur.MUST);
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
             * should not be used for facet searches, therefore set 'false'
             */
            indexReader = getIndexReader(false);
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
                    int numHits = 0;
                    if(hitsRequested.isFixedDrillPath()) {
                        // only in the case of the fixed drillpath we use the count where the facet does not need to exist
                        numHits = searcher.search(searchQuery).length();
                    }
                    
                    ParsedFacet parsedFacet = null;
                    try {
                        parsedFacet = new ParsedFacet(facet, null);
                    } catch (Exception e) {
                        log.error("Error parsing facet: ", e);
                        return new ResultImpl(0, null);
                    }
                    
                    String propertyName = ServicingNameFormat.getInteralPropertyPathName(nsMappings, parsedFacet.getNamespacedProperty());
                    
                    // facet range list has the follow String[] format: String[0] = facetName, String[1] = from, String[2] = to
                    List<String[]> facetRangeList = null;
                    if (parsedFacet.getFacetRanges() != null) {
                         // we have facet ranges
                         facetRangeList = new ArrayList<String[]>();
                         for(FacetRange facetRange : parsedFacet.getFacetRanges()){
                             int type = facetRange.getRangeType();
                             switch (type) { 
                                 case PropertyType.DATE:
                                     // parse the date config
                                     Calendar calBegin = Calendar.getInstance();
                                     Calendar calEnd = Calendar.getInstance();
                                     HippoDateTools.Resolution resolution = HippoDateTools.Resolution.RESOLUTIONSMAP.get(facetRange.getResolution());
                                     if(resolution == null) {
                                         log.error("Skipping unknown resolution : '{}' for facet ranges", facetRange.getResolution());
                                     }
                                     calBegin.add(resolution.getCalendarField(), (int)facetRange.getBegin());
                                     calEnd.add(resolution.getCalendarField(), (int)facetRange.getEnd());
                                     
                                     long begin = HippoDateTools.round(calBegin.getTimeInMillis(), resolution);
                                     long end = HippoDateTools.round(calEnd.getTimeInMillis(), resolution);
                                     String[] facetRangeItem = new String[3];
                                     facetRangeItem[0] = facetRange.getName();
                                     facetRangeItem[1] = String.valueOf(begin);
                                     facetRangeItem[2] = String.valueOf(end);
                                     facetRangeList.add(facetRangeItem);
                                     break;
                                 default:
                                     log.error("Range faceted browsing is not supported for property type beloning to '{}'", parsedFacet.getNamespacedProperty());
                                     return new ResultImpl(0, null);
                             }
                         }
                    }
                    
                    /*
                     * facetPropExists: the node must have the property as facet
                     */
                    
                    FacetPropExistsQuery facetPropExists = new FacetPropExistsQuery(facet,propertyName);
                
                    searchQuery.add(facetPropExists.getQuery(), Occur.MUST);

                    collector = new FacetResultCollector(indexReader, propertyName, resultset.get(facet), facetRangeList,
                            hitsRequested);
                    searcher.search(searchQuery, collector);
                    // set the numHits value
                    if(hitsRequested.isFixedDrillPath()) {
                        collector.setNumhits(numHits);
                    }
                }
                
                return new ResultImpl(collector.getNumhits(), null);
                
            } else {
                // resultset is null, so search for HippoNodeType.HIPPO_RESULTSET
                if (!hitsRequested.isResultRequested()) {
                    // only fetch the count and return:
                    Hits hits = searcher.search(searchQuery);
                    return new ResultImpl(hits.length(), null);
                } else {
                    Set<String> fieldNames = new HashSet<String>();
                    fieldNames.add(FieldNames.UUID);
                    FieldSelector fieldSelector = new SetBasedFieldSelector(fieldNames, new HashSet<String>());

                    int fetchTotal = hitsRequested.getOffset() + hitsRequested.getLimit();
                    Sort sort = null;
                    if(hitsRequested.getOrderByList().size() > 0) {
                        List<SortField> sortFields = new ArrayList<SortField>();
                        for(OrderBy orderBy : hitsRequested.getOrderByList()) {
                            try {
                                String propertyName = ServicingNameFormat.getInteralPropertyPathName(nsMappings, orderBy.getName());
                                String internalFacetName = ServicingNameFormat.getInternalFacetName(propertyName);
                                boolean reverse = orderBy.isDescending();
                                
                                /*
                                 * Ard: we need to check here unfortanetly whether the field we want to sort on in Lucene is actually indexed
                                 * because, imo, Lucene incorrectly throws a RunTimeException (lucene 2.3.2) in  {@link ExtendedFieldCacheImpl#createValue(IndexReader, Object)}
                                 * when trying to sort on a non existing lucene field. Therefor the check below, otherwise 
                                 * 
                                 * tfDocs = searcher.search(searchQuery, (Filter) null, fetchTotal, sort);
                                 * 
                                 * throws an exception when the sort contains a non indexed field
                                 */ 
                                
                                TermEnum termEnum = indexReader.terms(new Term (internalFacetName, ""));
                                Term term = termEnum.term();
                                if (term == null) {
                                  log.warn("Cannot sort on non-indexed property '{}'. Skip sorting on this property.", orderBy.getName());
                                }
                                if (term.field().equals(internalFacetName)) {
                                   // found a field with internalFacetName: we can sort on it! 
                                   sortFields.add(new SortField(internalFacetName, reverse));
                                } else {
                                    log.warn("Cannot sort on non-indexed property '{}'. Skip sorting on this property.", orderBy.getName());
                                }
                                termEnum.close();
                            } catch (IllegalNameException e) {
                                log.error("Cannot order by illegal name: '{}' : '{}'. Skip name ", orderBy.getName(), e.getMessage());
                            }
                        }
                        if(sortFields.size() > 0) {
                            sort = new Sort(sortFields.toArray(new SortField[sortFields.size()]));
                        }
                    }
                    
                    TopDocs tfDocs;
                    if(sort == null) {
                        // when sort == null, use this search without search as is more efficient
                        tfDocs = searcher.search(searchQuery, (Filter) null, fetchTotal);
                    } else {
                        tfDocs = searcher.search(searchQuery, (Filter) null, fetchTotal, sort);
                        //tfDocs = searcher.search(searchQuery, (Filter) null, fetchTotal);
                    }
                    ScoreDoc[] hits = tfDocs.scoreDocs;
                    int position = hitsRequested.getOffset();
                    
                    // LinkedHashSet because ordering should be kept!
                    Set<NodeId> nodeIdHits = new LinkedHashSet<NodeId>();
                    while (position < hits.length) {
                        Document d = indexReader.document(hits[position].doc, fieldSelector);
                        Field uuidField = d.getField(FieldNames.UUID);
                        if (uuidField != null) {
                            nodeIdHits.add(NodeId.valueOf(uuidField.stringValue()));
                        }
                        position++;
                    }
                    return new ResultImpl(nodeIdHits.size(), nodeIdHits);
                }
            }

        } catch (IllegalNameException e) {
            log.error("Error during creating view: ", e);
        } catch (IOException e) {
            log.error("Error during creating view: ", e);
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
        // an exception happend
        
        return new ResultImpl(0, null);
    }
    
    public Result view(String queryName, QueryImpl initialQuery, ContextImpl contextImpl,
            List<KeyValue<String, String>> facetsQueryList, QueryImpl openQuery, Map<String, Map<String, Count>> resultset,
            Map<Name,String> inheritedFilter, HitsRequested hitsRequested)
            throws UnsupportedOperationException {
        
        return this.view(queryName, initialQuery, contextImpl, facetsQueryList, null, openQuery, resultset, inheritedFilter, hitsRequested);
    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
             List<KeyValue<String, String>> facetsQuery, QueryImpl openQuery,Map<Name,String> inheritedFilter, HitsRequested hitsRequested) {
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

}
