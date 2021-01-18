/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.query.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.lucene.AbstractQueryImpl;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.FilterMultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.IndexFormatVersion;
import org.apache.jackrabbit.core.query.lucene.IndexingConfigurationEntityResolver;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryBuilder;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQuery;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.Ordering;
import org.apache.jackrabbit.core.query.lucene.QueryImpl;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.query.lucene.Util;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.hippoecm.repository.query.lucene.util.CachingMultiReaderQueryFilter;
import org.hippoecm.repository.util.RepoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.hippoecm.repository.query.lucene.QueryHelper.isMatchAllDocsQuery;

public class ServicingSearchIndex extends SearchIndex implements HippoQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(ServicingSearchIndex.class);

    /**
     * The DOM with the indexing configuration or <code>null</code> if there
     * is no such configuration.
     */
    private Element indexingConfiguration;

    private boolean servicingConsistencyCheckEnabled;


    private boolean supportSimilarityOnStrings = true;
    private boolean supportSimilarityOnBinaries = false;

    private boolean slowAlwaysExactSizedQueryResult = false;

    private boolean useSimpleFSDirectory = true;

    /**
     * Whether similarity searches on String properties are supported.Supporting similarity on
     * Strings increases the Lucene index. If no similarity searches are needed, it is better
     * to not support similarity.
     */
    public void setSupportSimilarityOnStrings(boolean supportSimilarityOnStrings) {
        this.supportSimilarityOnStrings = supportSimilarityOnStrings;
    }

    // although we do not need the getter ourselves, it is mandatory here because otherwise the setter is not called because
    // of org.apache.commons.collections.BeanMap#keyIterator
    public boolean getSupportSimilarityOnStrings() {
        return supportSimilarityOnStrings;
    }

    /**
     * Whether similarity searches on Binary properties are supported. Supporting similarity on
     * binaries increases the Lucene index. If no similarity searches are needed, it is better
     * to not support similarity.
     */
    public void setSupportSimilarityOnBinaries(boolean supportSimilarityOnBinaries) {
        this.supportSimilarityOnBinaries = supportSimilarityOnBinaries;
    }

    // although we do not need the getter ourselves, it is mandatory here because otherwise the setter is not called because
    // of org.apache.commons.collections.BeanMap#keyIterator
    public boolean getSupportSimilarityOnBinaries() {
        return supportSimilarityOnBinaries;
    }

    public boolean getSlowAlwaysExactSizedQueryResult() {
        return slowAlwaysExactSizedQueryResult;
    }

    public void setSlowAlwaysExactSizedQueryResult(final boolean slowAlwaysExactSizedQueryResult) {
        this.slowAlwaysExactSizedQueryResult = slowAlwaysExactSizedQueryResult;
    }

    private final Cache<String, CachingMultiReaderQueryFilter> cache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();

    /**
     * @return the authorization bitset and <code>null</code> when every bit is allowed to be read
     * @throws IOException
     */
    protected CachingMultiReaderQueryFilter getAuthorizationFilter(final Session session) throws IOException {
        if (!(session instanceof InternalHippoSession)) {
            return null;
        }
        if (session.getUserID().equals("system")) {
            return null;
        }

        String userId = session.getUserID();
        CachingMultiReaderQueryFilter filter = cache.getIfPresent(userId);
        InternalHippoSession internalHippoSession = (InternalHippoSession) session;
        BooleanQuery query = internalHippoSession.getAuthorizationQuery().getQuery();
        if (query.getClauses().length == 1 && isMatchAllDocsQuery(query.getClauses()[0].getQuery())) {
            // optimization : return a null filter in case it matches all docs
            return null;
        }
        if (filter != null && !filter.getQuery().equals(query)) {
            cache.invalidate(userId);
            filter = null;
        }
        if (filter == null) {
            // since this method can be invoked concurrently for the same userID it might be that we store
            // the same filter twice or more: This only happens for the first unique userID or after a change in
            // authorization. Any way, storing it needlessly twice or more only for the same userID under concurrency is
            // much preferable over introducing synchronization
            filter = new CachingMultiReaderQueryFilter(query, session.getUserID());
            cache.put(session.getUserID(), filter);
        }
        return filter;
    }

    /**
     * Executes the query on the search index.
     *
     * @param session         the session that executes the query.
     * @param queryImpl       the query impl.
     * @param query           the lucene query.
     * @param orderProps      name of the properties for sort order.
     * @param orderSpecs      the order specs for the sort order properties.
     *                        <code>true</code> indicates ascending order,
     *                        <code>false</code> indicates descending.
     * @param resultFetchHint a hint on how many results should be fetched.
     * @return the query hits.
     * @throws IOException if an error occurs while searching the index.
     */
    public MultiColumnQueryHits executeQuery(SessionImpl session,
                                             AbstractQueryImpl queryImpl,
                                             Query query,
                                             Path[] orderProps,
                                             boolean[] orderSpecs,
                                             String[] orderFuncs,
                                             long resultFetchHint)
            throws IOException {
        checkOpen();
        Sort sort = new Sort(createSortFields(orderProps, orderSpecs, orderFuncs));
        final IndexReader reader = getIndexReader();
        // an authorizationFilter that is equal to null means: no filter for bitset
        CachingMultiReaderQueryFilter authorizationFilter = getAuthorizationFilter(session);
        final HippoIndexSearcher searcher = new HippoIndexSearcher(session, reader, getItemStateManager(), authorizationFilter);
        searcher.setSimilarity(getSimilarity());
        return new FilterMultiColumnQueryHits(
                searcher.execute(query, sort, resultFetchHint,
                        QueryImpl.DEFAULT_SELECTOR_NAME)) {
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Util.closeOrRelease(reader);
                }
            }
        };
    }

    /**
     * Executes the query on the search index.
     *
     * @param session         the session that executes the query.
     * @param query           the query.
     * @param orderings       the order specs for the sort order.
     * @param resultFetchHint a hint on how many results should be fetched.
     * @return the query hits.
     * @throws IOException if an error occurs while searching the index.
     */
    public MultiColumnQueryHits executeQuery(SessionImpl session,
                                             MultiColumnQuery query,
                                             Ordering[] orderings,
                                             long resultFetchHint)
            throws IOException {
        checkOpen();

        final IndexReader reader = getIndexReader();
        CachingMultiReaderQueryFilter authorizationFilter = getAuthorizationFilter(session);
        final HippoIndexSearcher searcher = new HippoIndexSearcher(session, reader, getItemStateManager(), authorizationFilter);
        searcher.setSimilarity(getSimilarity());
        return new FilterMultiColumnQueryHits(
                query.execute(searcher, orderings, resultFetchHint)) {
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Util.closeOrRelease(reader);
                }
            }
        };
    }

    @Override
    public ServicingIndexingConfiguration getIndexingConfig() {
        return (ServicingIndexingConfiguration) super.getIndexingConfig();
    }

    @Override
    public ExecutableQuery createExecutableQuery(
            SessionContext sessionContext, String statement, String language)
            throws InvalidQueryException {

        QueryImpl query = new QueryImpl(sessionContext, this,
                getContext().getPropertyTypeRegistry(), statement, language, getQueryNodeFactory()) {
            @Override
            public QueryResult execute(long offset, long limit) throws RepositoryException {
                if (log.isDebugEnabled()) {
                    log.debug("Executing query: \n" + root.dump());
                }

                // build lucene query
                final Query query = LuceneQueryBuilder.createQuery(root, sessionContext.getSessionImpl(),
                        index.getContext().getItemStateManager(),
                        index.getNamespaceMappings(), index.getTextAnalyzer(),
                        propReg, index.getSynonymProvider(),
                        index.getIndexFormatVersion(), cache);

                OrderQueryNode orderNode = root.getOrderNode();

                OrderQueryNode.OrderSpec[] orderSpecs;
                if (orderNode != null) {
                    orderSpecs = orderNode.getOrderSpecs();
                } else {
                    orderSpecs = new OrderQueryNode.OrderSpec[0];
                }
                Path[] orderProperties = new Path[orderSpecs.length];
                boolean[] ascSpecs = new boolean[orderSpecs.length];
                String[] orderFuncs = new String[orderSpecs.length];
                for (int i = 0; i < orderSpecs.length; i++) {
                    orderProperties[i] = orderSpecs[i].getPropertyPath();
                    ascSpecs[i] = orderSpecs[i].isAscending();
                    orderFuncs[i] = orderSpecs[i].getFunction();
                }


                return new HippoQueryResult(index, sessionContext,
                        this, query,
                        getColumns(), orderProperties, ascSpecs, orderFuncs,
                        orderProperties.length == 0 && getRespectDocumentOrder(),
                        offset, limit);
            }
        };
        query.setRespectDocumentOrder(getRespectDocumentOrder());
        return query;
    }


    /**
     * Returns the document element of the indexing configuration or
     * <code>null</code> if there is no indexing configuration.
     *
     * @return the indexing configuration or <code>null</code> if there is
     *         none.
     */
    @Override
    protected Element getIndexingConfigurationDOM() {
        if (indexingConfiguration != null) {
            return indexingConfiguration;
        }
        String configName = getIndexingConfiguration();
        if (configName == null) {
            return null;
        }
        InputStream configInputStream;
        if (configName.startsWith("file:/")) {
            configName = RepoUtils.stripFileProtocol(configName);
            File config = new File(configName);
            log.info("Using indexing configuration: " + configName);
            if (!config.exists()) {
                log.warn("File does not exist: " + this.getIndexingConfiguration());
                return null;
            } else if (!config.canRead()) {
                log.warn("Cannot read file: " + this.getIndexingConfiguration());
                return null;
            }
            try {
                configInputStream = new FileInputStream(config);
            } catch (FileNotFoundException ex) {
                log.warn("indexing configuration not found: " + configName);
                return null;
            }
        } else {
            log.info("Using resource repository indexing_configuration: " + configName);
            configInputStream = ServicingSearchIndex.class.getResourceAsStream(configName);
            if (configInputStream == null) {
                log.warn("indexing configuration not found: " + getClass().getName() + "/" + configName);
                return null;
            }
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new IndexingConfigurationEntityResolver());
            InputSource configurationInputSource = new InputSource(configInputStream);
            indexingConfiguration = builder.parse(configurationInputSource).getDocumentElement();
        } catch (ParserConfigurationException e) {
            log.warn("Unable to create XML parser", e);
        } catch (IOException | SAXException e) {
            log.warn("Exception parsing " + this.getIndexingConfiguration(), e);
        }
        return indexingConfiguration;
    }

    @Override
    public void setForceConsistencyCheck(final boolean b) {
        super.setForceConsistencyCheck(false);
    }

    public boolean getServicingConsistencyCheckEnabled() {
        return servicingConsistencyCheckEnabled;
    }

    @Override
    public void setEnableConsistencyCheck(final boolean b) {
        super.setEnableConsistencyCheck(false);
        servicingConsistencyCheckEnabled = b;
    }

    @Override
    public void setUseSimpleFSDirectory(final boolean useSimpleFSDirectory) {
        this.useSimpleFSDirectory = useSimpleFSDirectory;
    }

    @Override
    public boolean isUseSimpleFSDirectory() {
        return useSimpleFSDirectory;
    }

    @Override
    public void updateNodes(Iterator<NodeId> remove, Iterator<NodeState> add) throws RepositoryException, IOException {

        final Set<NodeId> augmentedRemove = new HashSet<>();
        while (remove.hasNext()) {
            final NodeId nodeId = remove.next();
            if (nodeId != null) {
                augmentedRemove.add(nodeId);
            }
        }

        // NodeState does not implement equals hence we need NodeId
        final Map<NodeId, NodeState> augmentedAdd = new HashMap<>();
        while (add.hasNext()) {
            final NodeState nodeState = add.next();
            if (nodeState != null) {
                // since NodeState does not have hashcode/equals impls, we need to use NodeId for equals in Map
                augmentedAdd.put(nodeState.getNodeId(), nodeState);
            }
        }

        appendDocumentsThatHaveChangedChildNodesOrChangedHandles(augmentedRemove, augmentedAdd);

        //  now filter out documents that have a 'skip index' marker
        Map<NodeId, NodeState> includedNodeStates = getSkipIndexFilteredNodeStates(augmentedAdd);

        super.updateNodes(augmentedRemove.iterator(),
                includedNodeStates.values().iterator());

    }

    private Map<NodeId, NodeState> getSkipIndexFilteredNodeStates(final Map<NodeId, NodeState> augmentedAdd) throws RepositoryException {
        Map<NodeId, NodeState> includedNodeStates = new HashMap<>();
        // since NodeState does not have hashcode/equals impls, we need to use NodeId for caches
        Set<NodeId> excludedIdsCache = new HashSet<>();
        Set<NodeId> includedIdsCache = new HashSet<>();

        for (NodeState nodeState : augmentedAdd.values()) {
            if (nodeState != null) {
                if (!skipIndexing(nodeState, excludedIdsCache, includedIdsCache)) {
                    includedNodeStates.put(nodeState.getNodeId(), nodeState);
                } else {
                    log.debug("Nodestate '{}' is marked to be skipped for indexing.", nodeState.getId());
                }
            }
        }
        return includedNodeStates;
    }

    /*
     * If node states (below documents) have been newly ADDED (can not yet be found via retrieveAggregateRoot
     * since no org.apache.jackrabbit.core.query.lucene.FieldNames.AGGREGATED_NODE_UUID indexed on document level )
     * that are descendants of hippo:document nodes, then those hippo:document nodes need to be re-indexed.
     *
     * Also re-index documents below changed handles
     */
    private void appendDocumentsThatHaveChangedChildNodesOrChangedHandles(final Set<NodeId> augmentedRemove,
                                                                          final Map<NodeId, NodeState> augmentedAdd) throws RepositoryException, IOException {
        final Set<NodeId> checkedIds = new HashSet<>();
        List<NodeState> reindexNodeStates = new ArrayList<>();
        final ItemStateManager itemStateManager = getItemStateManager();
        for (Map.Entry<NodeId, NodeState> addEntry : augmentedAdd.entrySet()) {
            try {
                final NodeState state = addEntry.getValue();
                if (isHandle(state)) {
                    // changed handle (hippo:name translation perhaps, hence re-index the variants)
                    for (ChildNodeEntry childNodeEntry : state.getChildNodeEntries()) {
                        if (childNodeEntry.getId() instanceof HippoNodeId) {
                            // do not index virtual child nodes, ever
                            continue;
                        }

                        final NodeState childState = (NodeState)itemStateManager.getItemState(childNodeEntry.getId());
                        if (isHippoDocument(childState)) {
                            // found document below changed handle. Add document to be reindexed
                            addStateIfNeeded(augmentedRemove, augmentedAdd, reindexNodeStates, childState);
                        }
                    }
                    continue;
                }

                NodeState document = getContainingDocument(state, checkedIds, itemStateManager);
                if (document != null) {
                    addStateIfNeeded(augmentedRemove, augmentedAdd, reindexNodeStates, document);
                }
            } catch (ItemStateException e) {
                log.debug("Unable to retrieve state: {}", e.getMessage());
            }
        }
        for (NodeState nodeState : reindexNodeStates) {
            augmentedAdd.put(nodeState.getNodeId(), nodeState);
            augmentedRemove.add(nodeState.getNodeId());
        }
    }

    private void addStateIfNeeded(final Set<NodeId> augmentedRemove,
                                  final Map<NodeId, NodeState> augmentedAdd,
                                  final List<NodeState> reindexNodeStates, final NodeState state) {
        final NodeId nodeId = state.getNodeId();
        if (nodeId instanceof HippoNodeId) {
            // do not index virtual child nodes, ever
            return;
        }
        if (augmentedAdd.containsKey(nodeId) &&
                augmentedRemove.contains(nodeId)) {
            return;
        }
        reindexNodeStates.add(state);
    }

    @Override
    protected Document createDocument(NodeState node, NamespaceMappings nsMappings,
                                      IndexFormatVersion indexFormatVersion) throws RepositoryException {

        return createDocument(node, nsMappings, indexFormatVersion, false);
    }

    protected Document createDocument(NodeState node, NamespaceMappings nsMappings,
                                      IndexFormatVersion indexFormatVersion, boolean aggregateDescendants) throws RepositoryException {

        if (node.getId() instanceof HippoNodeId) {
            log.warn("Indexing a virtual node should never happen, and not be possible. Return an empty lucene doc");
            return null;
        }

        ServicingNodeIndexer indexer = new ServicingNodeIndexer(node, getContext(), nsMappings, getParser());

        indexer.setSupportHighlighting(getSupportHighlighting());
        indexer.setSupportSimilarityOnStrings(getSupportSimilarityOnStrings());
        indexer.setSupportSimilarityOnBinaries(getSupportSimilarityOnBinaries());
        indexer.setServicingIndexingConfiguration(getIndexingConfig());
        indexer.setIndexFormatVersion(indexFormatVersion);
        Document doc = indexer.createDoc();
        mergeAggregatedNodeIndexes(node, doc, indexFormatVersion);

        try {
            if (aggregateDescendants) {
                aggregateDescendants(node, doc, indexFormatVersion, indexer, false);
            } else if (isDocumentVariant(node)) {
                // we have a Hippo Document state, let's aggregate child text
                // for free text searching. We use aggregateChildTypes = true only for child
                // nodes directly below a hippo document.
                aggregateDescendants(node, doc, indexFormatVersion, indexer, true);
                aggregateHandle(node, doc, indexer);
            }
        } catch (ItemStateException e) {
            log.debug("Unable to get state '{}'", e.getMessage());
        }

        return doc;
    }


    private boolean skipIndexing(final NodeState node,
                                 final Set<NodeId> excludedIdsCache,
                                 final Set<NodeId> includedIdsCache) throws RepositoryException {
        return skipIndexing(node, excludedIdsCache, includedIdsCache, new ArrayList<NodeId>());
    }

    /*
     * checks recursively all ancestor states whether non of them is marked to be excluded for indexing
     * once a state is found to be excluded for indexing, all the states that were checked are added to the
     * 'excludedIdsCache' to avoid pointless double checking.
     * The nodeIdHierarchy keeps track of checked nodes
     */
    private boolean skipIndexing(final NodeState node,
                                 final Set<NodeId> excludedIdsCache,
                                 final Set<NodeId> includedIdsCache,
                                 final List<NodeId> nodeIdHierarchy) throws RepositoryException {

        final NodeId parentId = node.getParentId();
        if (parentId == null) {
            // no 'skip index' node was found, add this nodeIdHierarchy list to the include set
            includedIdsCache.addAll(nodeIdHierarchy);
            return false;
        }

        final NodeId nodeId = node.getNodeId();
        nodeIdHierarchy.add(nodeId);


        if (includedIdsCache.contains(nodeId)) {
            includedIdsCache.addAll(nodeIdHierarchy);
            return false;
        } else if (excludedIdsCache.contains(nodeId)) {
            // an ancestor was already found to be excluded for indexing
            excludedIdsCache.addAll(nodeIdHierarchy);
            return true;
        }

        if (node.getMixinTypeNames().contains(getIndexingConfig().getSkipIndexName())) {
            excludedIdsCache.addAll(nodeIdHierarchy);
            return true;
        }

        try {
            final NodeState parent = getNodeState(parentId);
            return skipIndexing(parent, excludedIdsCache, includedIdsCache, nodeIdHierarchy);
        } catch (ItemNotFoundException e) {
            log.debug("Node with id '{}' not found, not skipping indexing", parentId);
            return false;
        } catch (ItemStateException e) {
            String msg = "Error while indexing node: " + nodeId + " of "
                    + "type: " + node.getNodeTypeName();
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @return <code>true</code> when the NodeState belongs to a
     *         hippo:document below a hippo:handle
     */
    private boolean isDocumentVariant(NodeState node) throws ItemStateException {
        if (getIndexingConfig() != null) {

            if (node.getParentId() == null) {
                return false;
            }

            ItemStateManager ism = getItemStateManager();

            NodeState parent = (NodeState) ism.getItemState(node.getParentId());
            if (parent != null && isHandle(parent) && isHippoDocument(node)) {
                return true;
            }

        }
        return false;
    }

    private boolean isHandle(final NodeState node) {
        return node.getNodeTypeName().equals(getIndexingConfig().getHippoHandleName());
    }

    private boolean isHippoNamed(final NodeState state) {
        return state.getMixinTypeNames().contains(getIndexingConfig().getHippoNamedName());
    }

    private boolean isHippoDocument(final NodeState node) {
        try {
            final EffectiveNodeType nodeType = getContext().getNodeTypeRegistry().getEffectiveNodeType(node.getNodeTypeName());
            return nodeType.includesNodeType(getIndexingConfig().getHippoDocumentName());
        } catch (NoSuchNodeTypeException e) {
            log.error("Unexpected exception while checking node type", e);
        }
        return false;
    }

    /**
     * @return the <code>NodeState</code> of the Document variant which is an ancestor of the state
     * or <code>null</code> if this state was not a child of a document variant
     */
    private NodeState getContainingDocument(final NodeState state, final Set<NodeId> checkedIds,
                                            final ItemStateManager itemStateManager) throws ItemStateException {
        if (checkedIds.contains(state.getNodeId())) {
            // already checked these ancestors: no need to do it again
            return null;
        }
        checkedIds.add(state.getNodeId());
        if (isDocumentVariant(state)) {
            return state;
        }
        if (state.getParentId() == null) {
            return null;
        }
        NodeState parent = (NodeState) itemStateManager.getItemState(state.getParentId());
        if (parent == null) {
            return null;

        }
        return getContainingDocument(parent, checkedIds, itemStateManager);
    }

    /**
     * Adds the fulltext index field of the child states to Document doc
     *
     * @param aggregateChildTypes When <code>true</code>, properties of child nodes will also be indexed as explicit
     *                            fields on <code>doc</code> if configured as aggregate/childType in
     *                            indexing_configuration.xml
     */
    private void aggregateDescendants(final NodeState state, final Document doc,
                                      final IndexFormatVersion indexFormatVersion,
                                      final ServicingNodeIndexer indexer, final boolean aggregateChildTypes) {
        for (ChildNodeEntry childNodeEntry : state.getChildNodeEntries()) {
            if (childNodeEntry.getId() instanceof HippoNodeId) {
                // do not index virtual child nodes, ever
                continue;
            }
            Document aDoc;
            try {
                final ItemStateManager itemStateManager = getItemStateManager();
                final NodeState childState = (NodeState) itemStateManager.getItemState(childNodeEntry.getId());
                if (aggregateChildTypes) {
                    if (getIndexingConfig().isChildAggregate(childState.getNodeTypeName())) {
                        for (Name propName : childState.getPropertyNames()) {
                            PropertyId id = new PropertyId(childNodeEntry.getId(), propName);
                            PropertyState propState = (PropertyState) itemStateManager.getItemState(id);
                            InternalValue[] values = propState.getValues();
                            if (!indexer.isHippoPath(propName) && indexer.isFacet(propName)) {
                                final NamePathResolver resolver = indexer.getResolver();
                                String fieldName = resolver.getJCRName(propState.getName()) + "/"
                                        + resolver.getJCRName(childNodeEntry.getName());
                                indexer.indexFacetProperty(doc, fieldName);

                                for (final InternalValue value : values) {
                                    indexer.addFacetValue(doc, value, fieldName, propState.getName());
                                }
                            }
                        }
                    }
                }

                doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, childState.getNodeId().toString(),
                        Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));

                aDoc = createDocument(childState, getNamespaceMappings(), indexFormatVersion, true);

                // transfer fields to doc if there are any
                Fieldable[] fulltextFields = aDoc.getFieldables(FieldNames.FULLTEXT);
                if (fulltextFields != null) {
                    for (final Fieldable fulltextField : fulltextFields) {
                        doc.add(fulltextField);
                    }
                }

                // Really important to keep here for updating the aggregate (document variant) when a child is removed
                Fieldable[] aggrNodeUUID = aDoc.getFieldables(FieldNames.AGGREGATED_NODE_UUID);
                if (aggrNodeUUID != null) {
                    for (Fieldable f : aggrNodeUUID) {
                        doc.add(f);
                    }
                }
            } catch (NoSuchItemStateException e) {
                final String message = "Unable to add index fields for child states of " + state.getId() + " because an item could not be found. " +
                        "Probably because it was removed again.";
                if (log.isDebugEnabled()) {
                    log.warn(message, e);
                } else {
                    log.warn(message + " (full stack trace on debug level)");
                }
            } catch (ItemStateException e) {
                log.warn("ItemStateException while indexing descendants of a hippo:document for "
                        + "node with UUID: " + state.getNodeId().toString(), e);
            } catch (ItemNotFoundException e) {
                final String message = "Unable to add index fields for child states of " + state.getId() + " because an item could not be found. " +
                        "Probably because it was removed again.";
                if (log.isDebugEnabled()) {
                    log.warn(message, e);
                } else {
                    log.warn(message + " (full stack trace on debug level)");
                }
            } catch (RepositoryException e) {
                log.warn("RepositoryException while indexing descendants of a hippo:document for "
                        + "node with UUID: " + state.getNodeId().toString(), e);
            }
        }
    }

    private void aggregateHandle(final NodeState state, final Document doc, final ServicingNodeIndexer indexer) {
        try {
            if (!isHippoDocument(state)) {
                return;
            }
            final NodeId parentId = state.getParentId();
            if (parentId == null) {
                return;
            }
            final NodeState parentState = getNodeState(parentId);
            if (!isHandle(parentState)) {
                return;
            }
            if (isHippoNamed(parentState)) {
                final PropertyId namePropertyId = new PropertyId(parentState.getNodeId(), getIndexingConfig().getHippoNameName());
                final PropertyState namePropertyState = getPropertyState(namePropertyId);
                indexer.addStringValue(doc, getIndexingConfig().getHippoNameFieldName(), namePropertyState.getValues()[0].getString(), true, true, 2.0f, true, false);

            }
            // make sure that if the handle gets the mixin 'hippo:named' or gets its 'hippo:name' changed, it triggers a re-index on the documents (variants)
            doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, parentId.toString(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));

        } catch (ItemStateException | RepositoryException e) {
            final String message = "Unable to add index translations of document " + state.getId();
            if (log.isDebugEnabled()) {
                log.warn(message, e);
            } else {
                log.warn(message + ": " + e + " (full stack on debug level)");
            }
        }
    }

    private PropertyState getPropertyState(ItemId propertyId) throws ItemStateException {
        return (PropertyState) getItemStateManager().getItemState(propertyId);
    }

    private NodeState getNodeState(ItemId nodeId) throws ItemNotFoundException, ItemStateException {
        return (NodeState) getItemStateManager().getItemState(nodeId);
    }

    private ItemStateManager getItemStateManager() {
        return getContext().getItemStateManager();
    }

}
