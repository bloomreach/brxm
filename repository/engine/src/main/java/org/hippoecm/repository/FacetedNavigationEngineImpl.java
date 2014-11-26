/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.InvalidQueryException;
import javax.security.auth.Subject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.JackrabbitQueryParser;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryBuilder;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.Util;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.OpenBitSet;
import org.hippoecm.repository.jackrabbit.HippoSharedItemStateManager;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.query.lucene.FacetFiltersQuery;
import org.hippoecm.repository.query.lucene.FacetPropExistsQuery;
import org.hippoecm.repository.query.lucene.FacetRangeQuery;
import org.hippoecm.repository.query.lucene.FacetsQuery;
import org.hippoecm.repository.query.lucene.InheritedFilterQuery;
import org.hippoecm.repository.query.lucene.RangeFields;
import org.hippoecm.repository.query.lucene.ServicingFieldNames;
import org.hippoecm.repository.query.lucene.ServicingNameFormat;
import org.hippoecm.repository.query.lucene.ServicingSearchIndex;
import org.hippoecm.repository.query.lucene.util.CachingMultiReaderQueryFilter;
import org.hippoecm.repository.query.lucene.util.SetDocIdSetBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedNavigationEngineImpl extends ServicingSearchIndex
        implements
        FacetedNavigationEngine<FacetedNavigationEngineImpl.QueryImpl, FacetedNavigationEngineImpl.ContextImpl> {

    class QueryAndSort {
        org.apache.lucene.search.Query query;
        Sort sort;

        QueryAndSort(org.apache.lucene.search.Query query, Sort sort){
            this.query = query;
            this.sort = sort;
        }
    }

    class QueryImpl extends FacetedNavigationEngine.Query {
        String statement;
        String language;
        String[] scopes;
        FacetFilters facetFilters;
        QueryAndSort queryAndSort = null;

        @SuppressWarnings("deprecation")
        public QueryImpl(String parameter) throws IllegalArgumentException{
            if (parameter.length() >= javax.jcr.query.Query.XPATH.length() + 2 && parameter.substring(0, javax.jcr.query.Query.XPATH.length() + 1).equalsIgnoreCase(javax.jcr.query.Query.XPATH + "(")) {
                language = javax.jcr.query.Query.XPATH;
                statement = parameter.substring(javax.jcr.query.Query.XPATH.length() + 1, parameter.length() - 1);
            } else if(parameter.length() >= javax.jcr.query.Query.SQL.length() + 2 && parameter.substring(0, javax.jcr.query.Query.SQL.length() + 1).equalsIgnoreCase(javax.jcr.query.Query.SQL + "(")) {
                language = javax.jcr.query.Query.SQL;
                statement = parameter.substring(javax.jcr.query.Query.SQL.length() + 1, parameter.length() - 1);
            } else if(parameter.matches("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}.*")) {
                statement = parameter;
                language = "";
                String docbases = parameter;
                if(parameter.indexOf(Query.DOCBASE_FILTER_DELIMITER) > -1) {
                    // parse the filters
                    docbases = docbases.substring(0,parameter.indexOf(Query.DOCBASE_FILTER_DELIMITER));
                    String filters = parameter.substring(parameter.indexOf(Query.DOCBASE_FILTER_DELIMITER) + 1);
                    facetFilters = FacetFilters.fromString(filters);
                }
                scopes = docbases.split(",");
            } else {
                QueryParser parser = new JackrabbitQueryParser(FieldNames.FULLTEXT, getTextAnalyzer(), getSynonymProvider(), null);
                //parser.setOperator(QueryParser.DEFAULT_OPERATOR_AND);
                try {
                    org.apache.lucene.search.Query query = parser.parse(parameter);
                    queryAndSort = new QueryAndSort(query, null);
                } catch (ParseException ex) {
                    throw new IllegalArgumentException("Unable to parse query", ex);
                }
            }
        }

        @SuppressWarnings("deprecation")
        public QueryAndSort getLuceneQueryAndSort(ContextImpl context) {
            if (queryAndSort == null) {
                if (javax.jcr.query.Query.XPATH.equals(language)) {
                    try {
                        QueryRootNode root = org.apache.jackrabbit.spi.commons.query.QueryParser.parse(statement,
                                "xpath", context.session, getQueryNodeFactory());
                        org.apache.lucene.search.Query query = LuceneQueryBuilder.createQuery(root, context.session,
                                getContext().getItemStateManager(),
                                getNamespaceMappings(), getTextAnalyzer(),
                                getContext().getPropertyTypeRegistry(),
                                getSynonymProvider(),
                                getIndexFormatVersion(), null);

                        // if there is a sort, set the sort
                        Sort sort = null;
                        if(root.getOrderNode() != null) {
                            OrderQueryNode.OrderSpec[] orderSpecs = root.getOrderNode().getOrderSpecs();
                            Path[] orderProperties = new Path[orderSpecs.length];
                            boolean[] ascSpecs = new boolean[orderSpecs.length];
                            for (int i = 0; i < orderSpecs.length; i++) {
                                orderProperties[i] = orderSpecs[i].getPropertyPath();
                                ascSpecs[i] = orderSpecs[i].isAscending();
                            }
                            // TODO: get orderfuncs from somewhere?
                            final String[] orderFuncs = new String[orderProperties.length];
                            sort = new Sort(createSortFields(orderProperties, ascSpecs, orderFuncs));
                        }
                        queryAndSort = new QueryAndSort(query, sort);

                    } catch (InvalidQueryException ex) {
                        throw new IllegalArgumentException("Unable to parse query", ex);
                    } catch (RepositoryException ex) {
                        throw new IllegalArgumentException("error while building query", ex);
                    }
                } else if (javax.jcr.query.Query.SQL.equals(language)) {
                    log.warn("SQL not supported as free query in faceted navigation nodes");
                }
            }
            return queryAndSort;
        }

        public String toString() {
            return statement;
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
        SessionImpl session;
        private AuthorizationQuery authorizationQuery;

        ContextImpl(SessionImpl session, String userId, Subject subject, NodeTypeManager ntMgr) throws RepositoryException {
            this.session = session;
            this.authorizationQuery = ((InternalHippoSession) session).getAuthorizationQuery();
        }

        BooleanQuery getAuthorizationQuery() {
            return authorizationQuery != null ? authorizationQuery.getQuery() : null;
        }

        DocIdSet getAuthorisationIdSet(IndexReader reader) throws IOException {
            final CachingMultiReaderQueryFilter authorizationFilter = getAuthorizationFilter(session);
            if (authorizationFilter == null) {
                return null;
            }
            return authorizationFilter.getDocIdSet(reader);
        }
    }

    private static class DocIdSetFilter extends Filter {

        private final OpenBitSet docIdSet;

        private DocIdSetFilter(OpenBitSet docIdSet) throws IOException {
            this.docIdSet = docIdSet;
        }

        @Override
        public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
            return docIdSet;
        }

    }

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(FacetedNavigationEngine.class);

    // note some Jackrabbit Queries like ParentAxisQuery cannot be very well cached because do not have proper equals and hashCode :
    // however, for single fac nav nodes the same ParentAxisQuery instance is reused, and thus still valuable. Also note
    // that most 'free text queries' do not involve parent or child axis queries: We need to document the cost of using these in faceted
    // navigation contexts.
    private Cache<org.apache.lucene.search.Query, Filter> filterCache =
            CacheBuilder.newBuilder().softValues().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();

    private Cache<FVCKey, Map<String, Count>> facetValueCountCache  =
            CacheBuilder.newBuilder().softValues().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();

    public FacetedNavigationEngineImpl() {

    }

    public ContextImpl prepare(String userId, Subject subject, List<QueryImpl> initialQueries, Session session)
            throws RepositoryException {
        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        return new ContextImpl((SessionImpl)session, userId, subject, ntMgr);
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


    private int docIdSetCacheSize = 1000;
    private int facetValueCountMapCacheSize = 1000;

    public void setDocIdSetCacheSize(int docIdSetCacheSize) {
        this.docIdSetCacheSize = docIdSetCacheSize;
    }

    public int getDocIdSetCacheSize() {
        return docIdSetCacheSize;
    }

    /**
     * The facetedEngineCacheMngr property for the maximum number of facetValueCount's
     */
    public void setFacetValueCountMapCacheSize(int facetValueCountMapCacheSize) {
        this.facetValueCountMapCacheSize = facetValueCountMapCacheSize;
    }

    // although we do not need the getter ourselves, it is mandatory here because otherwise the setter is not called because
    // of org.apache.commons.collections.BeanMap#keyIterator
    public int getFacetValueCountMapCacheSize() {
        return facetValueCountMapCacheSize;
    }


    public Result view(String queryName, QueryImpl initialQuery, ContextImpl contextImpl,
                       List<KeyValue<String, String>> facetsQueryList, List<FacetRange> rangeQuery, QueryImpl openQuery,
                       Map<String, Map<String, Count>> resultset, Map<String, String> inheritedFilter, HitsRequested hitsRequested)
            throws UnsupportedOperationException, IllegalArgumentException {

        long start = System.currentTimeMillis();
        try {
            return doView(queryName, initialQuery, contextImpl, facetsQueryList, rangeQuery, openQuery, resultset,
                    inheritedFilter, hitsRequested);
        } finally {
            log.debug("Faceted Navigation Engine view took {} ms to complete.", (System.currentTimeMillis() - start));
        }
    }

    public Result doView(String queryName, QueryImpl initialQuery, ContextImpl contextImpl,
                         List<KeyValue<String, String>> facetsQueryList, List<FacetRange> rangeQuery, QueryImpl openQuery,
                         Map<String, Map<String, Count>> resultset, Map<String, String> inheritedFilter, HitsRequested hitsRequested)
            throws UnsupportedOperationException, IllegalArgumentException {
        NamespaceMappings nsMappings = getNamespaceMappings();

        IndexReader indexReader = null;
        try {
            indexReader = getIndexReader(false);

            IndexSearcher searcher = new IndexSearcher(indexReader);
            SetDocIdSetBuilder matchingDocsSetBuilder = new SetDocIdSetBuilder();

            BooleanQuery facetsQuery = new FacetsQuery(facetsQueryList, nsMappings).getQuery();
            matchingDocsSetBuilder.add(filterDocIdSetPlainLuceneQuery(facetsQuery, indexReader));

            BooleanQuery facetRangeQuery = new FacetRangeQuery(rangeQuery, nsMappings, this).getQuery();
            matchingDocsSetBuilder.add(filterDocIdSetPlainLuceneQuery(facetRangeQuery, indexReader));

            BooleanQuery inheritedFilterQuery = new InheritedFilterQuery(inheritedFilter, nsMappings).getQuery();
            matchingDocsSetBuilder.add(filterDocIdSetPlainLuceneQuery(inheritedFilterQuery, indexReader));

            org.apache.lucene.search.Query initialLuceneQuery = null;
            if (initialQuery != null && initialQuery.scopes != null && initialQuery.scopes.length > 0) {
                if(initialQuery.scopes.length == 1) {
                    initialLuceneQuery = new TermQuery(new Term(ServicingFieldNames.HIPPO_PATH, initialQuery.scopes[0]));
                } else {
                    initialLuceneQuery = new BooleanQuery(true);
                    for(String scope : initialQuery.scopes) {
                        ((BooleanQuery)initialLuceneQuery).add(new TermQuery(new Term(ServicingFieldNames.HIPPO_PATH, scope)), Occur.SHOULD);
                    }
                }
            }
            matchingDocsSetBuilder.add(filterDocIdSetPlainLuceneQuery(initialLuceneQuery, indexReader));

            FacetFiltersQuery facetFiltersQuery = null;
            if (initialQuery != null && initialQuery.facetFilters != null) {
                facetFiltersQuery = new FacetFiltersQuery(initialQuery.facetFilters, nsMappings, this.getTextAnalyzer(), this.getSynonymProvider()); 
            }

            final BooleanQuery authorizationQuery = contextImpl.getAuthorizationQuery();
            if (authorizationQuery != null) {
                final DocIdSet authorisationIdSet = contextImpl.getAuthorisationIdSet(indexReader);
                if (authorisationIdSet != null) {
                    matchingDocsSetBuilder.add(authorisationIdSet);
                }
            }

            if (resultset != null) {
               // If there are more than one facet in the 'resultset' we return an empty result as this is not allowed
               if(resultset.size() > 1) {
                   log.error("The resultset cannot contain multiple facets");
                   return new ResultImpl(0, null);
               }

               int cardinality = 0;
               for (String namespacedFacet : resultset.keySet()) {

                   // Not a search involving scoring, thus compute bitsets for facetFiltersQuery & freeSearchInjectedSort
                   if (facetFiltersQuery != null) {
                       if (facetFiltersQuery.isPlainLuceneQuery()) {
                           matchingDocsSetBuilder.add(filterDocIdSetPlainLuceneQuery(facetFiltersQuery.getQuery(), indexReader));
                       } else {
                           matchingDocsSetBuilder.add(filterDocIdSetJackRabbitQuery(facetFiltersQuery.getQuery(), indexReader));
                       }
                   }

                   if (openQuery != null) {
                       QueryAndSort queryAndSort = openQuery.getLuceneQueryAndSort(contextImpl);
                       // open query is always a jackrabbit query
                       matchingDocsSetBuilder.add(filterDocIdSetJackRabbitQuery(queryAndSort.query, indexReader));
                   }

                   OpenBitSet matchingDocs = matchingDocsSetBuilder.toBitSet();
                   cardinality = (int) matchingDocs.cardinality();
                    /*
                     * Nodes not having this facet, still should be counted if they are a hit
                     * in the query without this facet. Therefor, first get the count query without
                     * FacetPropExistsQuery.
                     */
                    int numHits = 0;
                    if (hitsRequested.isFixedDrillPath()) {
                        // only in the case of the fixed drillpath we use the count where the facet does not need to exist
                        numHits = (int) matchingDocs.cardinality();
                    }

                    ParsedFacet parsedFacet;
                    try {
                        parsedFacet = ParsedFacet.getInstance(namespacedFacet);
                    } catch (Exception e) {
                        log.error("Error parsing facet: ", e);
                        return new ResultImpl(0, null);
                    }

                    String propertyName = ServicingNameFormat.getInteralPropertyPathName(nsMappings, parsedFacet
                            .getNamespacedProperty());

                    /*
                     * facetPropExists: the node must have the property as facet
                     */

                    matchingDocsSetBuilder.add(filterDocIdSetPlainLuceneQuery(new FacetPropExistsQuery(propertyName).getQuery(), indexReader));

                    matchingDocs = matchingDocsSetBuilder.toBitSet();
                    cardinality = (int) matchingDocs.cardinality();
                    // this method populates the facetValueCountMap for the current facet

                   // index reader is instance of JackrabbitIndexReader : we need the wrapped multi-index reader as
                   // cache key : since during deletes only, the backing index reader can stay the same, we
                   // also need to use numDocs to be sure we get the right cached values
                    Object[] keyObjects = {matchingDocs,propertyName,parsedFacet, indexReader.getCoreCacheKey(), indexReader.numDocs()};
                    FVCKey fvcKey = new FVCKey(keyObjects);

                    Map<String, Count> facetValueCountMap = facetValueCountCache.getIfPresent(fvcKey);
                    if(facetValueCountMap == null) { 
                        facetValueCountMap =  new HashMap<String, Count>();
                        populateFacetValueCountMap(propertyName, parsedFacet, facetValueCountMap, matchingDocs, indexReader);
                        facetValueCountCache.put(fvcKey, facetValueCountMap);
                        log.debug("Caching new facet value count map");
                    } else {
                        log.debug("Reusing previously cached facet value count map");
                    }
                    
                    Map<String, Count> resultFacetValueCountMap = resultset.get(namespacedFacet);
                    resultFacetValueCountMap.putAll(facetValueCountMap);
                    
                    // set the numHits value
                    if (hitsRequested.isFixedDrillPath()) {
                        return new ResultImpl(numHits, null);
                    }
                }

                return new ResultImpl(cardinality, null);

            } else {
                // resultset is null, so search for HippoNodeType.HIPPO_RESULTSET
                if (!hitsRequested.isResultRequested()) {
                    // No search with SCORING involved, this everything can be done with BitSet's
                    if (facetFiltersQuery != null && facetFiltersQuery.getQuery().clauses().size() > 0) {
                        matchingDocsSetBuilder.add(filterDocIdSetPlainLuceneQuery(facetFiltersQuery.getQuery(), indexReader));
                    }
                    
                    if (openQuery != null) {
                        QueryAndSort queryAndSort = openQuery.getLuceneQueryAndSort(contextImpl);
                        matchingDocsSetBuilder.add(filterDocIdSetJackRabbitQuery(queryAndSort.query, indexReader));
                    }

                    int size = (int) matchingDocsSetBuilder.toBitSet().cardinality();
                    return new ResultImpl(size, null);
                    
                } else {
                    
                    BooleanQuery searchQuery = new BooleanQuery(false);
                    Sort freeSearchInjectedSort = null;
                    if (facetFiltersQuery != null && facetFiltersQuery.getQuery().clauses().size() > 0) {
                        searchQuery.add(facetFiltersQuery.getQuery(), Occur.MUST);
                    }
                    
                    if (openQuery != null) {
                        QueryAndSort queryAndSort = openQuery.getLuceneQueryAndSort(contextImpl);
                        if(queryAndSort.query != null) {
                            searchQuery.add(queryAndSort.query, Occur.MUST);
                        }
                        freeSearchInjectedSort = queryAndSort.sort;
                    }

                    Set<String> fieldNames = new HashSet<String>();
                    fieldNames.add(FieldNames.UUID);
                    FieldSelector fieldSelector = new SetBasedFieldSelector(fieldNames, new HashSet<String>());

                    int fetchTotal = hitsRequested.getOffset() + hitsRequested.getLimit();
                    Sort sort = null;
                    if(freeSearchInjectedSort != null) {
                        // we already have a sort from the xpath or sql free search. Use this one
                        sort = freeSearchInjectedSort;
                    } else  if (hitsRequested.getOrderByList().size() > 0) {
                        List<Path> orderPropertiesList = new ArrayList<Path>();
                        List<Boolean> ascSpecsList = new ArrayList<Boolean>();
                        for (OrderBy orderBy : hitsRequested.getOrderByList()) {
                            try {
                               Name orderByProp = NameFactoryImpl.getInstance().create(orderBy.getName());
                               boolean isAscending = !orderBy.isDescending();
                               orderPropertiesList.add(createPath(orderByProp));
                               ascSpecsList.add(isAscending);
                            } catch (IllegalArgumentException e) {
                               log.warn("Skip property '{}' because cannot create a Name for it: {}",orderBy.getName(), e.toString());
                            }
                        }
                        if(orderPropertiesList.size() > 0) {
                            Path[] orderProperties = orderPropertiesList.toArray(new Path[orderPropertiesList.size()]);
                            boolean[] ascSpecs = new boolean[ascSpecsList.size()];
                            int i = 0;
                            for(Boolean b : ascSpecsList) {
                                ascSpecs[i] = b;
                                i++;
                            }
                            sort = new Sort(createSortFields(orderProperties, ascSpecs, new String[orderProperties.length]));
                        }
                    }

                    boolean sortScoreAscending = false;
                    // if the sort is on score descending, we can set it to null as this is the default and more efficient                  
                    if(sort != null && sort.getSort().length == 1 && sort.getSort()[0].getType() == SortField.SCORE) {
                        
                        if(sort.getSort()[0].getReverse()) {
                            sortScoreAscending = true;
                        } else {
                            // we can skip sort as it is on score descending
                            sort = null;
                        }
                    }
                    
                    TopDocs tfDocs;
                    org.apache.lucene.search.Query query = searchQuery;
                    if(searchQuery.clauses().size() == 0) {
                       // add a match all query
                       // searchQuery.add(new MatchAllDocsQuery(), Occur.MUST);
                        query = new MatchAllDocsQuery();
                     }
                     
                    if (sort == null) {
                        // when sort == null, use this search without search as is more efficient
                        Filter filterToApply = new DocIdSetFilter(matchingDocsSetBuilder.toBitSet());
                        tfDocs = searcher.search(query, filterToApply, fetchTotal);
                    } else {
                        if(sortScoreAscending) {
                            // we need the entire searchQuery because scoring is involved
                            Filter filterToApply = new DocIdSetFilter(matchingDocsSetBuilder.toBitSet());
                            tfDocs = searcher.search(query, filterToApply, fetchTotal, sort);
                        } else {
                            // because we have at least one explicit sort, scoring can be skipped. We can use cached bitsets combined with a match all query
                            if (facetFiltersQuery != null) {
                                matchingDocsSetBuilder.add(filterDocIdSetPlainLuceneQuery(facetFiltersQuery.getQuery(), indexReader));
                            }
                            if (openQuery != null) {
                                QueryAndSort queryAndSort = openQuery.getLuceneQueryAndSort(contextImpl);
                                matchingDocsSetBuilder.add(filterDocIdSetJackRabbitQuery(queryAndSort.query, indexReader));
                            }

                            Filter filterToApply = new DocIdSetFilter(matchingDocsSetBuilder.toBitSet());
                            // set query to MatchAllDocsQuery because we have everything as filter now
                            query = new MatchAllDocsQuery();
                            tfDocs = searcher.search(query, filterToApply, fetchTotal, sort);
                        }
                        
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
            
            if (indexReader != null) {
                try {
                    // do not call indexReader.close() as ref counting is taken care of by  
                    // org.apache.jackrabbit.core.query.lucene.Util#closeOrRelease
                    Util.closeOrRelease(indexReader);
                } catch (IOException e) {
                    log.error("Exception while closing index reader", e);
                }
            }
        }
        return new ResultImpl(0, null);
    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl contextImpl,
            List<KeyValue<String, String>> facetsQueryList, QueryImpl openQuery,
            Map<String, Map<String, Count>> resultset, Map<String, String> inheritedFilter, HitsRequested hitsRequested)
            throws UnsupportedOperationException, IllegalArgumentException {

        return this.view(queryName, initialQuery, contextImpl, facetsQueryList, null, openQuery, resultset,
                inheritedFilter, hitsRequested);
    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
            List<KeyValue<String, String>> facetsQuery, QueryImpl openQuery, Map<String, String> inheritedFilter,
            HitsRequested hitsRequested) throws IllegalArgumentException{
        return view(queryName, initialQuery, authorization, facetsQuery, openQuery, null, inheritedFilter,
                hitsRequested);
    }

    public QueryImpl parse(String query) throws IllegalArgumentException {
        return this.new QueryImpl(query);
    }

    @Override
    protected void doInit() throws IOException {
        QueryHandlerContext context = getContext();
        HippoSharedItemStateManager stateMgr = (HippoSharedItemStateManager) context.getItemStateManager();
        stateMgr.repository.setFacetedNavigationEngine(this);
        super.doInit();
    }

    public Result query(String statement, ContextImpl context) throws InvalidQueryException, RepositoryException {
        QueryRootNode root = org.apache.jackrabbit.spi.commons.query.QueryParser.parse(statement, "xpath",
                context.session, getQueryNodeFactory());
        org.apache.lucene.search.Query query = LuceneQueryBuilder.createQuery(root, context.session, getContext()
                .getItemStateManager(), getNamespaceMappings(), getTextAnalyzer(), getContext()
                .getPropertyTypeRegistry(), getSynonymProvider(), getIndexFormatVersion(), null);

        Set<NodeId> nodeIdHits = new LinkedHashSet<NodeId>();
        try {
            IndexReader indexReader = getIndexReader(false);
            IndexSearcher searcher = new IndexSearcher(indexReader);
           
            TopDocs tfDocs = searcher.search(query, null, 1000);
            ScoreDoc[] hits = tfDocs.scoreDocs;
            int position = 0;

            Set<String> fieldNames = new HashSet<String>();
            fieldNames.add(FieldNames.UUID);
            FieldSelector fieldSelector = new SetBasedFieldSelector(fieldNames, new HashSet<String>());

            // LinkedHashSet because ordering should be kept!
            while (position < hits.length) {
                Document d = indexReader.document(hits[position].doc, fieldSelector);
                Field uuidField = d.getField(FieldNames.UUID);
                if (uuidField != null) {
                    nodeIdHits.add(NodeId.valueOf(uuidField.stringValue()));
                }
                position++;
            }
        } catch (IOException ex) {
            log.warn(ex.getMessage(), ex);
        }
        return new ResultImpl(nodeIdHits.size(), nodeIdHits);
    }

    private void populateFacetValueCountMap(String propertyName, ParsedFacet parsedFacet,
            Map<String, Count> facetValueCountMap, OpenBitSet matchingDocs, IndexReader indexReader) throws IOException {

        long start = 0;
        if(log.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }
        
        if (matchingDocs.cardinality() == 0) {
            return;
        }

        if (parsedFacet.getFacetRanges() != null) {
            TermDocs termDocs = indexReader.termDocs();
            try {
                for (FacetRange facetRange : parsedFacet.getFacetRanges()) {
                    long t1 = 0;
                    if(log.isDebugEnabled()) {
                        t1 = System.currentTimeMillis();
                    }
                    try {
                        String internalName = ServicingNameFormat.getInteralPropertyPathName(getNamespaceMappings(),
                                facetRange.getNamespacedProperty());

                        RangeFields rangeFields = new RangeFields(internalName, facetRange);

                        Count counter = new Count(0);
                        facetValueCountMap.put(rangeFields.facetRangeName, counter);

                        // TODO : skip here the logic for long and double range and use the matchingDocs directly and through term vectors fetch the values

                        if (rangeFields.begin == null && rangeFields.end == null) {
                            // short cut: begin and end are open, thus all hits apply. Therefor this short cut is possible
                            counter.count = (int) matchingDocs.cardinality();
                        } else {
                            TermEnum termEnum = indexReader.terms(new Term(rangeFields.internalFacetName,
                                    rangeFields.begin == null ? "" : rangeFields.begin));
                            try {
                                do {
                                    Term term = termEnum.term();
                                    if (term != null && term.field() == rangeFields.internalFacetName) { // interned comparison as rangeFields.internalFacetName is interned
                                        if (rangeFields.end != null && term.text().compareTo(rangeFields.end) >= 0) {
                                            // term text is higher than upper limit
                                            break;
                                        }
                                        termDocs.seek(term);
                                        while (termDocs.next()) {
                                            if (matchingDocs.get(termDocs.doc())) {
                                                counter.count++;
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                } while (termEnum.next());
                            } finally {
                                termEnum.close();
                            }
                        }

                    } catch (IllegalNameException e) {
                        log.error(e.toString());
                    } catch (IllegalArgumentException e) {
                        log.warn(e.getMessage());
                    }
                    if(log.isDebugEnabled()) {
                        log.debug("Populating range '{}' took '{}' ms. ", facetRange.getName(), (System.currentTimeMillis() - t1));
                    }
                }
            } finally {
                termDocs.close();
            }
        } else {
            String internalFacetName = ServicingNameFormat.getInternalFacetName(propertyName).intern(); // important to intern for the == comparison
            TermEnum termEnum = indexReader.terms(new Term(internalFacetName, ""));
            // iterate through all the values of this facet and see look at number of hits per term

            try {
                TermDocs termDocs = indexReader.termDocs();
                // open termDocs only once, and use seek: this is more efficient
                try {
                    do {
                        Term term = termEnum.term();
                        int count = 0;
                        if (term != null && term.field() == internalFacetName) { // interned comparison

                            termDocs.seek(term);
                            while (termDocs.next()) {
                                if (matchingDocs.get(termDocs.doc())) {
                                    count++;
                                }
                            }

                            // TODO in mode: show 0 valued facets, we need to return the count == 0 as well!
                            if (count > 0) {
                                // matchingDocs contains the current term
                                if (!"".equals(term.text())) {
                                    facetValueCountMap.put(term.text(), new Count(count));
                                }
                            }

                        } else {
                            break;
                        }
                    } while (termEnum.next());
                } finally {
                    termDocs.close();
                }
            } finally {
                termEnum.close();
            }
        }
        
        if(log.isDebugEnabled()) {
            log.debug("Populating the FacetValueCountMap took '{}' ms for  #'{}' facet values (in case of ranges, this is not the same as all unique facet values, but only the number of ranges) ", (System.currentTimeMillis() - start), facetValueCountMap.size());
        }
    }


    private DocIdSet filterDocIdSetPlainLuceneQuery(final org.apache.lucene.search.Query query,
                                    final IndexReader indexReader) throws IOException {
        if ((query instanceof BooleanQuery) && ((BooleanQuery)query).clauses().size() == 0) {
            // no constraints. Return null
            return null;
        }
        Filter queryFilter = filterCache.getIfPresent(query);
        if (queryFilter != null) {
            log.debug("For query '{}' getting queryFilter from cache", query);
        }
        else {
            queryFilter = new CachingMultiReaderQueryFilter(query);
            filterCache.put(query, queryFilter);
        }
        return queryFilter.getDocIdSet(indexReader);
    }

    private DocIdSet filterDocIdSetJackRabbitQuery(final org.apache.lucene.search.Query query,
                                              final IndexReader indexReader) throws IOException {
        if ((query instanceof BooleanQuery) && ((BooleanQuery)query).clauses().size() == 0) {
            // no constraints. Return null
            return null;
        }

        // TODO CACHE jackrabbit queries still need to be cached. Difficult parts are
        // 1: Jackrabbit Queries do not have hashCode or equals
        // 2: Jackrabbit Query implementations keep REFERENCES (!!) to index readers
        // 3.Jackrabbit creates a *NEW* JackrabbitIndexReader instance for *EVERY* search. Hence
        // if the reader is a JackrabbitIndexReader, the cache would be pointless.
        // Since all index readers in JR extend from FilterIndexReader, we can use
        // reader.getCoreCacheKey() : The FilterIndexReader delegates that call to the
        // wrapped index reader
        // TODO CACHE
        final OpenBitSet bits = new OpenBitSet(indexReader.maxDoc());
        long start = System.currentTimeMillis();
        new IndexSearcher(indexReader).search(query, new AbstractHitCollector() {
            @Override
            public final void collect(int doc, float score) {
                bits.set(doc);  // set bit for hit
            }
        });
        log.info("Creating doc id set for Jackrabbit Query took {} ms.", String.valueOf(System.currentTimeMillis() - start));
        return new DocIdSetFilter(bits).getDocIdSet(indexReader);

    }

    /**
     * Creates a path with a single element out of the given <code>name</code>.
     *
     * @param name the name to create the path from.
     * @return a path with a single element.
     */
    private static Path createPath(Name name) {
        try {
            PathBuilder builder = new PathBuilder();
            builder.addLast(name);
            return builder.getPath();
        } catch (MalformedPathException e) {
            // never happens, we just added an element
            throw new InternalError();
        }
    }

    private class FVCKey {
        final Object[] keyObjects;
        public FVCKey(final Object[] keyObjects) {
            this.keyObjects = keyObjects;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FVCKey fvcKey = (FVCKey) o;

            if (!Arrays.equals(keyObjects, fvcKey.keyObjects)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return keyObjects != null ? Arrays.hashCode(keyObjects) : 0;
        }
    }

}
