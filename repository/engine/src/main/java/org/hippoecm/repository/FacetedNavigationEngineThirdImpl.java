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
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.InvalidQueryException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.JackrabbitQueryParser;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryBuilder;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
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
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.jackrabbit.HippoSharedItemStateManager;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.query.lucene.FacetFiltersQuery;
import org.hippoecm.repository.query.lucene.FacetPropExistsQuery;
import org.hippoecm.repository.query.lucene.FacetRangeQuery;
import org.hippoecm.repository.query.lucene.FacetsQuery;
import org.hippoecm.repository.query.lucene.FixedScoreSimilarity;
import org.hippoecm.repository.query.lucene.InheritedFilterQuery;
import org.hippoecm.repository.query.lucene.RangeFields;
import org.hippoecm.repository.query.lucene.ServicingFieldNames;
import org.hippoecm.repository.query.lucene.ServicingIndexingConfiguration;
import org.hippoecm.repository.query.lucene.ServicingNameFormat;
import org.hippoecm.repository.query.lucene.ServicingSearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedNavigationEngineThirdImpl extends ServicingSearchIndex
        implements
        FacetedNavigationEngine<FacetedNavigationEngineThirdImpl.QueryImpl, FacetedNavigationEngineThirdImpl.ContextImpl> {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    class QueryAndSort {
        org.apache.lucene.search.Query query;
        org.apache.lucene.search.Sort sort;
        
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
                if(parameter.indexOf(FacetedNavigationEngine.Query.DOCBASE_FILTER_DELIMITER) > -1) {
                    // parse the filters
                    docbases = docbases.substring(0,parameter.indexOf(FacetedNavigationEngine.Query.DOCBASE_FILTER_DELIMITER));
                    String filters = parameter.substring(parameter.indexOf(FacetedNavigationEngine.Query.DOCBASE_FILTER_DELIMITER) + 1);
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
                    ex.printStackTrace();
                    throw new IllegalArgumentException("Unable to parse query", ex);
                }
            }
        }

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
                                sort = new Sort(createSortFields(orderProperties, ascSpecs));
                            }
                        }
                        queryAndSort = new QueryAndSort(query, sort);
                        
                    } catch (InvalidQueryException ex) {
                        throw new IllegalArgumentException("Unable to parse query", ex);
                    } catch (RepositoryException ex) {
                        throw new IllegalArgumentException("error while building query", ex);
                    }
                } else if (javax.jcr.query.Query.SQL.equals(language)) {
//                    try {
//                        QueryHandler queryHandler = ((RepositoryImpl)context.session.getRepository()).getSearchManager(context.session.getWorkspace().getName()).getQueryHandler();
//                        queryHandler.createExecutableQuery((SessionImpl)context.session, (SessionImpl)context.session.getItemManager, statement, language);
//                        FacetedNavigationEngineThirdImpl.this.getContext
//                    } catch (NoSuchWorkspaceException ex) {
//                        throw new IllegalArgumentException("error while building query", ex);
//                    } catch (RepositoryException ex) {
//                        throw new IllegalArgumentException("error while building query", ex);
//                    }
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
        AuthorizationQuery authorizationQuery;
        SessionImpl session;

        ContextImpl(SessionImpl session, String userId, Subject subject, NodeTypeManager ntMgr) throws RepositoryException {
            this.authorizationQuery = new AuthorizationQuery(subject, getNamespaceMappings(),
                    (ServicingIndexingConfiguration) getIndexingConfig(), ntMgr, session);
            this.session = session;
        }
    }

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(FacetedNavigationEngine.class);

    public FacetedNavigationEngineThirdImpl() {
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

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl contextImpl,
            List<KeyValue<String, String>> facetsQueryList, List<FacetRange> rangeQuery, QueryImpl openQuery,
            Map<String, Map<String, Count>> resultset, Map<String, String> inheritedFilter, HitsRequested hitsRequested)
            throws UnsupportedOperationException, IllegalArgumentException {
        NamespaceMappings nsMappings = getNamespaceMappings();

        /*
         * facetsQuery: get the query for the facets that are asked for
         */
        FacetsQuery facetsQuery = new FacetsQuery(facetsQueryList, nsMappings);

        /*
         * facetRangeQuery : get the query for the ranges of facet values
         */
        FacetRangeQuery facetRangeQuery = new FacetRangeQuery(rangeQuery, nsMappings, this);

        /*
         * inheritedFilter: get the query representation of the interited filters (for example from facetselect)
         */

        InheritedFilterQuery inheritedFilterQuery = new InheritedFilterQuery(inheritedFilter, nsMappings);

        /*
         * initialQuery: get the query for initialQuery. This is the hippo:docbase value. 
         */
        org.apache.lucene.search.Query initialLuceneQuery = null;
        if (initialQuery != null && !(initialQuery.scopes == null) && initialQuery.scopes.length > 0) {
            if(initialQuery.scopes.length == 1) {
                initialLuceneQuery = new TermQuery(new Term(ServicingFieldNames.HIPPO_PATH, initialQuery.scopes[0]));
            } else {
                initialLuceneQuery = new BooleanQuery(true);
                for(String scope : initialQuery.scopes) {
                    ((BooleanQuery)initialLuceneQuery).add(new TermQuery(new Term(ServicingFieldNames.HIPPO_PATH, scope)), Occur.SHOULD);
                }
            }
        }
        
        FacetFiltersQuery facetFiltersQuery = null;
        if (initialQuery != null && initialQuery.facetFilters != null) {
            facetFiltersQuery = new FacetFiltersQuery(initialQuery.facetFilters, nsMappings, this.getTextAnalyzer(), this.getSynonymProvider()); 
        }

        /*
         * authorizationQuery: get the query for the facets the person is allowed to see (which
         * is again a facetsQuery)
         */

        BooleanQuery searchQuery = new BooleanQuery(false);
        Sort freeSearchInjectedSort = null;

        if (facetsQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(facetsQuery.getQuery(), Occur.MUST);
        }

        if (facetRangeQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(facetRangeQuery.getQuery(), Occur.MUST);
        }

        if (inheritedFilterQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(inheritedFilterQuery.getQuery(), Occur.MUST);
        }

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

        // TODO perhaps create cached user specific filter for authorisation to gain speed
        if (contextImpl.authorizationQuery.getQuery().clauses().size() > 0) {
            // TODO enable again after HREPTWO-3959 is fixed
            //searchQuery.add(contextImpl.authorizationQuery.getQuery(), Occur.MUST);
        }

        if (initialLuceneQuery != null) {
            searchQuery.add(initialLuceneQuery, Occur.MUST);
        }

        //FacetResultCollector collector = null;
        BitSet matchingDocs = null;
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
                for (String namespacedFacet : resultset.keySet()) {
                    /*
                     * Nodes not having this facet, still should be counted if they are a hit
                     * in the query without this facet. Therefor, first get the count query without
                     * FacetPropExistsQuery.
                     */
                    int numHits = 0;
                    if (hitsRequested.isFixedDrillPath()) {
                        // only in the case of the fixed drillpath we use the count where the facet does not need to exist
                        numHits = searcher.search(searchQuery).length();
                    }

                    ParsedFacet parsedFacet = null;
                    try {
                        parsedFacet = new ParsedFacet(namespacedFacet);
                    } catch (Exception e) {
                        log.error("Error parsing facet: ", e);
                        return new ResultImpl(0, null);
                    }

                    String propertyName = ServicingNameFormat.getInteralPropertyPathName(nsMappings, parsedFacet
                            .getNamespacedProperty());

                    /*
                     * facetPropExists: the node must have the property as facet
                     */

                    FacetPropExistsQuery facetPropExists = new FacetPropExistsQuery(propertyName);

                    searchQuery.add(facetPropExists.getQuery(), Occur.MUST);

                    /*
                     * TODO when there are MANY facet values and few hits, a collector is more efficient then populateFacetValueCountMap. 
                     * When needed for performance, we need to find (empirical) the optimal numbers when to switch to a collector
                     */
                    long start = 0;
                    if(log.isDebugEnabled()) {
                        start = System.currentTimeMillis();
                    }
                    Filter filter = new QueryWrapperFilter(searchQuery);
                    matchingDocs = filter.bits(indexReader);
                    if(log.isDebugEnabled()) {
                        log.debug("Took '{}' ms to create the bitset filter (#hits = '"+matchingDocs.cardinality()+"' ) for the query '{}'", (System.currentTimeMillis() - start), searchQuery.toString());
                    }
                    
                    Map<String, Count> facetValueCountMap = resultset.get(namespacedFacet);
                    // this method populates the resultset for the current facet
                    populateFacetValueCountMap(propertyName, parsedFacet, facetValueCountMap, matchingDocs, indexReader);

                    // set the numHits value
                    if (hitsRequested.isFixedDrillPath()) {
                        return new ResultImpl(numHits, null);
                    }
                }

                return new ResultImpl(matchingDocs.cardinality(), null);

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
                    if(freeSearchInjectedSort != null) {
                        // we already have a sort from the xpath or sql free search. Use this one
                        sort = freeSearchInjectedSort;
                    } else  if (hitsRequested.getOrderByList().size() > 0) {
                        List<SortField> sortFields = new ArrayList<SortField>();
                        for (OrderBy orderBy : hitsRequested.getOrderByList()) {
                            try {
                                String propertyName = ServicingNameFormat.getInteralPropertyPathName(nsMappings,
                                        orderBy.getName());
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

                                TermEnum termEnum = indexReader.terms(new Term(internalFacetName, ""));
                                Term term = termEnum.term();
                                if (term == null) {
                                    log.warn(
                                            "Cannot sort on non-indexed property '{}'. Skip sorting on this property.",
                                            orderBy.getName());
                                }
                                if (term.field().equals(internalFacetName)) {
                                    // found a field with internalFacetName: we can sort on it! 
                                    sortFields.add(new SortField(internalFacetName, reverse));
                                } else {
                                    log.warn(
                                            "Cannot sort on non-indexed property '{}'. Skip sorting on this property.",
                                            orderBy.getName());
                                }
                                termEnum.close();
                            } catch (IllegalNameException e) {
                                log.error("Cannot order by illegal name: '{}' : '{}'. Skip name ", orderBy.getName(), e
                                        .getMessage());
                            }
                        }
                        if (sortFields.size() > 0) {
                            sort = new Sort(sortFields.toArray(new SortField[sortFields.size()]));
                        }
                    }

                    TopDocs tfDocs;
                    if (sort == null) {
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

    private void populateFacetValueCountMap(String propertyName, ParsedFacet parsedFacet,
            Map<String, Count> facetValueCountMap, BitSet matchingDocs, IndexReader indexReader) throws IOException {

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
                            counter.count = matchingDocs.cardinality();
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
            searcher.setSimilarity(new FixedScoreSimilarity());

            TopDocs tfDocs = searcher.search(query, (Filter) null, 1000);
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

}
