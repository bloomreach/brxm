/*
 *  Copyright 2008-2012 Hippo.
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.SessionImpl;
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
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.hippoecm.repository.util.RepoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ServicingSearchIndex extends SearchIndex implements HippoQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(ServicingSearchIndex.class);

    /**
     * The DOM with the indexing configuration or <code>null</code> if there
     * is no such configuration.
     */
    private Element indexingConfiguration;

    private final ConcurrentHashMap<String, AuthorizationBitSet> authorizationBitSets = new ConcurrentHashMap<String, AuthorizationBitSet>();

    /**
     * Simple zero argument constructor.
     */
    public ServicingSearchIndex() {
        super();
    }
    
    /**
     * @return the authorization bitset and <code>null</code> when every bit is allowed to be read
     * @throws IOException
     */
    private BitSet getAuthorizationBitSet(final InternalHippoSession session, final IndexReader reader) throws IOException {
        if (session.getUserID().equals("system")) {
            return null;
        }
        BitSet bits;
        final AuthorizationBitSet authorizationBitSet = authorizationBitSets.get(session.getUserID());
        if (authorizationBitSet == null || !authorizationBitSet.isValid(reader)) {
            Filter filter = new QueryWrapperFilter(session.getAuthorizationQuery().getQuery());
            bits = filter.bits(reader);
            authorizationBitSets.put(session.getUserID(), new AuthorizationBitSet(reader, bits));
        } else {
            bits = authorizationBitSet.bits;
        }
        if (bits.cardinality() == bits.length()) {
            // every bit is 1 , we can return null
            return null;
        }
        return bits;
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
                                             long resultFetchHint)
            throws IOException {
        checkOpen();

        Sort sort = new Sort(createSortFields(orderProps, orderSpecs));
        final IndexReader reader = getIndex().getIndexReader();
        // an authorizationBitSet that is equal to null means: no filter for bitset
        BitSet authorizationBitSet = null;
        if (session instanceof InternalHippoSession) {
            authorizationBitSet = getAuthorizationBitSet((InternalHippoSession) session, reader);
        }
        final HippoIndexSearcher searcher = new HippoIndexSearcher(session, reader, getContext().getItemStateManager(), authorizationBitSet);
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

        final IndexReader reader = getIndex().getIndexReader();
        BitSet authorizationBitSet = null;
        if (session instanceof InternalHippoSession) {
            authorizationBitSet = getAuthorizationBitSet((InternalHippoSession) session, reader);
        }
        final HippoIndexSearcher searcher = new HippoIndexSearcher(session, reader, getContext().getItemStateManager(), authorizationBitSet);
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
                for (int i = 0; i < orderSpecs.length; i++) {
                    orderProperties[i] = orderSpecs[i].getPropertyPath();
                    ascSpecs[i] = orderSpecs[i].isAscending();
                }


                return new HippoQueryResult(index, sessionContext,
                        this, query,
                        getColumns(), orderProperties, ascSpecs,
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
        InputStream configInputStream = null;
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
        } catch (IOException e) {
            log.warn("Exception parsing " + this.getIndexingConfiguration(), e);
        } catch (SAXException e) {
            log.warn("Exception parsing " + this.getIndexingConfiguration(), e);
        }
        return indexingConfiguration;
    }

    @Override
    public void updateNodes(Iterator<NodeId> remove, Iterator<NodeState> add) throws RepositoryException, IOException {
        final NodeStateIteratorImpl addedIt = new NodeStateIteratorImpl(add);
        final NodeIdIteratorImpl removedIt = new NodeIdIteratorImpl(remove);
        super.updateNodes(removedIt, addedIt);
        updateContainingDocumentNodes(addedIt.processedStates, addedIt.processedIds, removedIt.processedIds);
    }

    /*
     * If node states have been updated that are descendants of hippo:document nodes, then those hippo:document
     * nodes need to be re-indexed.
     */
    private void updateContainingDocumentNodes(final List<NodeState> addedStates,
                                               final List<NodeId> addedIds,
                                               final List<NodeId> removedIds) throws RepositoryException, IOException {
        final List<NodeId> checkedIds = new ArrayList<NodeId>();
        final List<NodeState> updateDocumentStates = new ArrayList<NodeState>();
        final List<NodeId> updateDocumentIds = new ArrayList<NodeId>();
        for (NodeState addedState : addedStates) {
            try {
                NodeState document = getContainingDocument(addedState, checkedIds);
                if (document != null && !addedIds.contains(document.getNodeId())
                        && !updateDocumentIds.contains(document.getNodeId())) {
                    if (!removedIds.contains(document.getNodeId())) {
                        updateDocumentStates.add(document);
                        updateDocumentIds.add(document.getNodeId());
                    }
                }
            } catch (ItemStateException e) {
                log.debug("Unable to get state '{}'", e.getMessage());
            }
        }

        if (updateDocumentStates.size() > 0) {
            super.updateNodes(updateDocumentIds.iterator(), updateDocumentStates.iterator());
        }
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
            return new Document();
        }

        ServicingNodeIndexer indexer = new ServicingNodeIndexer(node, getContext(), nsMappings, getParser());

        indexer.setSupportHighlighting(getSupportHighlighting());
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
            }
        } catch (ItemStateException e) {
            log.debug("Unable to get state '{}'", e.getMessage());
        }

        return doc;
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

            ItemStateManager ism = getContext().getItemStateManager();

            NodeState parent = (NodeState) ism.getItemState(node.getParentId());
            if (parent != null && isHandle(parent) && isHippoDocument(node)) {
                return true;
            }

        }
        return false;
    }

    private boolean isHandle(NodeState node) {
        return node.getNodeTypeName().equals(getIndexingConfig().getHippoHandleName());
    }

    private boolean isHippoDocument(NodeState node) {
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
    private NodeState getContainingDocument(NodeState state, List<NodeId> checkedIds) throws ItemStateException {
        if (checkedIds.contains(state.getNodeId())) {
            // already checked these ancestors: no need to do it again
            return null;
        }
        checkedIds.add(state.getNodeId());
        if (isDocumentVariant(state)) {
            return state;
        }
        ItemStateManager ism = getContext().getItemStateManager();
        if (state.getParentId() == null) {
            return null;
        }
        NodeState parent = (NodeState) ism.getItemState(state.getParentId());
        if (parent == null) {
            return null;

        }
        return getContainingDocument(parent, checkedIds);
    }

    /**
     * Adds the fulltext index field of the child states to Document doc
     * @param aggregateChildTypes When <code>true</code>, properties of child nodes will also be indexed as explicit
     *                            fields on <code>doc</code> if configured as aggregate/childType in indexing_configuration.xml
     */
    private void aggregateDescendants(NodeState state, Document doc, IndexFormatVersion indexFormatVersion, ServicingNodeIndexer indexer,  boolean aggregateChildTypes) {
        for (ChildNodeEntry childNodeEntry : state.getChildNodeEntries()) {
            if (childNodeEntry.getId() instanceof HippoNodeId) {
                // do not index virtual child nodes, ever
                continue;
            }
            Document aDoc;
            try {
                NodeState childState = (NodeState) getContext().getItemStateManager().getItemState(
                        childNodeEntry.getId());
                if (aggregateChildTypes) {
                    if (getIndexingConfig().isChildAggregate(childState.getNodeTypeName())) {
                        for (Name propName : childState.getPropertyNames()) {
                            PropertyId id = new PropertyId(childNodeEntry.getId(), propName);
                            PropertyState propState = (PropertyState) getContext().getItemStateManager().getItemState(id);
                            InternalValue[] values = propState.getValues();
                            if (!indexer.isHippoPath(propName) && indexer.isFacet(propName)) {
                                for (final InternalValue value : values) {
                                    String s = indexer.getResolver().getJCRName(propState.getName()) + "/"
                                            + indexer.getResolver().getJCRName(childNodeEntry.getName());
                                    indexer.addFacetValue(doc, value, s, propState.getName());
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
                    for (int k = 0; k < fulltextFields.length; k++) {
                        doc.add(fulltextFields[k]);
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

    // TODO remove when jackrabbit supports sorting on nodename
    @Override
    protected SortField[] createSortFields(Path[] orderProps, boolean[] orderSpecs) {
        SortField[] sortFields = super.createSortFields(orderProps, orderSpecs);
        for (int i = 0; i < orderProps.length; i++) {
            // replace the one created by the one from Jackrabbit, because the jackrabbit cannot sort on jcr:name (core < 1.4.6)
            if (orderProps[i].getString().equals(NameConstants.JCR_NAME.toString())) {
                sortFields[i] = new SortField(ServicingFieldNames.HIPPO_SORTABLE_NODENAME, /*getSortComparatorSource(),*/ !orderSpecs[i]);
            } else if(orderProps[i].getString().endsWith("/" + NameConstants.JCR_NAME.toString())) {
                try {
                    sortFields[i] = new SortField(orderProps[i].getAncestor(1).getString()+"/"+ServicingFieldNames.HIPPO_SORTABLE_NODENAME, /*getSortComparatorSource()*/ !orderSpecs[i]);
                } catch(PathNotFoundException ex) {
                    // ignore
                } catch(RepositoryException ex) {
                    // ignore
                }
            }
        }
        return sortFields;
    }

    private class NodeIdIteratorImpl implements Iterator<NodeId> {

        private final Iterator iter;
        List<NodeId> processedIds = new ArrayList<NodeId>();

        public NodeIdIteratorImpl(Iterator iterator) {
            this.iter = iterator;
        }

        public NodeId nextNodeId() throws NoSuchElementException {
            NodeId id = (NodeId) iter.next();
            processedIds.add(id);
            return id;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public NodeId next() {
            return nextNodeId();
        }

    }

    private class NodeStateIteratorImpl implements Iterator<NodeState> {
        Iterator process;
        List<NodeState> processedStates = new ArrayList<NodeState>();
        List<NodeId> processedIds = new ArrayList<NodeId>();

        NodeStateIteratorImpl(Iterator process) {
            this.process = process;
        }

        public NodeState nextNodeState() throws NoSuchElementException {
            NodeState state = (NodeState) process.next();
            if (state != null) {
                processedStates.add(state);
                processedIds.add(state.getNodeId());
            }
            return state;
        }

        public boolean hasNext() {
            return process.hasNext();
        }

        public NodeState next() {
            return nextNodeState();
        }

        public void remove() {
            process.remove();
        }

    }

    private static class AuthorizationBitSet {

        private final WeakReference<IndexReader> reader;
        private final BitSet bits;

        private AuthorizationBitSet(final IndexReader reader, final BitSet bits) {
            this.reader = new WeakReference<IndexReader>(reader);
            this.bits = bits;
        }

        private boolean isValid(IndexReader reader) {
            if (this.reader.get() != reader) {
                return false;
            }
            return true;
        }
    }

}
