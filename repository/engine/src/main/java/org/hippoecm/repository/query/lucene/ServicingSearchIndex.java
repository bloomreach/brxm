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
package org.hippoecm.repository.query.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry.TypeMapping;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.IndexFormatVersion;
import org.apache.jackrabbit.core.query.lucene.IndexingConfigurationEntityResolver;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryBuilder;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.MultiIndex;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.QueryImpl;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.query.lucene.SingleColumnQueryResult;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ServicingSearchIndex extends SearchIndex {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(ServicingSearchIndex.class);

    /**
     * The DOM with the indexing configuration or <code>null</code> if there
     * is no such configuration.
     */
    private Element indexingConfiguration;

    /**
     * Simple zero argument constructor.
     */
    public ServicingSearchIndex() {
        super();
    }

    /**
     * Returns the multi index.
     *
     * @return the multi index
     */
    public MultiIndex getIndex() {
        return super.getIndex();
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
                Query query = LuceneQueryBuilder.createQuery(root, sessionContext.getSessionImpl(),
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
            File config = new File(configName.substring(5));
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
        NodeStateIteratorImpl addedIt = new NodeStateIteratorImpl(add);
        NodeIdIteratorImpl removedIt = new NodeIdIteratorImpl(remove);
        super.updateNodes(removedIt, addedIt);

        /*
         * now see if there are added nodestate that are childs of a hippo:document in a hippo:handle:
         * When there are, and the hippo:document was not part of the NodeStateIterator add or remove, it means
         * the hippo:document must be reindexed
         */

        List<NodeState> addedStates = addedIt.processedStates;
        List<NodeId> addedIds = addedIt.processedIds;
        List<NodeId> removedIds = removedIt.processedIds;
        List<NodeId> checkedIds = new ArrayList<NodeId>();
        List<NodeState> reIndexVariantsStates = new ArrayList<NodeState>();
        List<NodeId> reIndexVariantsIds = new ArrayList<NodeId>();
        for (NodeState addedState : addedStates) {
            try {
                NodeState documentVariant = getVariantDocumentIfAncestor(addedState, checkedIds);
                if (documentVariant != null && !addedIds.contains(documentVariant.getNodeId())
                        && !reIndexVariantsIds.contains(documentVariant.getNodeId())) {
                    if (!removedIds.contains(documentVariant.getNodeId())) {
                        reIndexVariantsStates.add(documentVariant);
                        reIndexVariantsIds.add(documentVariant.getNodeId());
                    }
                }
            } catch (ItemStateException e) {
                log.debug("Unable to get state '{}'", e.getMessage());
            }
        }

        if (reIndexVariantsStates.size() > 0) {
            super.updateNodes(new NodeIdIteratorImpl(reIndexVariantsIds.iterator()), new NodeStateIteratorImpl(
                    reIndexVariantsStates.iterator()));
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
            Document doc = new Document();
            return doc;
        }

        ServicingNodeIndexer indexer = new ServicingNodeIndexer(node, getContext(), nsMappings, getParser());

        indexer.setSupportHighlighting(super.getSupportHighlighting());
        indexer.setServicingIndexingConfiguration((ServicingIndexingConfiguration) super.getIndexingConfig());
        indexer.setIndexFormatVersion(indexFormatVersion);
        Document doc = indexer.createDoc();
        mergeAggregatedNodeIndexes(node, doc, indexFormatVersion);

        try {
            if (aggregateDescendants) {
                aggregateDescendants(node, doc, indexFormatVersion);
            } else if (isDocumentVariant(node)) {
                // we have a Hippo Document state, let's aggregate child text
                // for free text searching.
                aggregateDescendants(node, doc, indexFormatVersion);
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
        if (this.getIndexingConfig() instanceof ServicingIndexingConfiguration) {
            ServicingIndexingConfiguration sIndexConfig = (ServicingIndexingConfiguration) this.getIndexingConfig();

            ItemStateManager ism = getContext().getItemStateManager();

            if (node.getParentId() == null) {
                return false;
            }
            NodeState parent = (NodeState) ism.getItemState(node.getParentId());
            if (parent != null && parent.getNodeTypeName().equals(sIndexConfig.getHippoHandleName())
                    && !node.getNodeTypeName().equals(sIndexConfig.getHippoRequestName())) {
                return true;
            }

        }
        return false;
    }

    /**
     * 
     * @param state
     * @param checkedStates
     * @return the <code>NodeState</code> of the Document variant which is an ancestor of the state or <code>null</code> if this state was not a child of a document variant
     */

    private NodeState getVariantDocumentIfAncestor(NodeState state, List<NodeId> checkedIds) throws ItemStateException {

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
        return getVariantDocumentIfAncestor(parent, checkedIds);

    }

    /**
     * Adds the fulltext index field of the child states to Document doc
     * @param state
     * @param doc
     * @param indexFormatVersion
     */
    private void aggregateDescendants(NodeState state, Document doc, IndexFormatVersion indexFormatVersion) {
        List childNodeEntries = state.getChildNodeEntries();
        List<NodeState> nodeStates = new ArrayList<NodeState>();
        for (Iterator it = childNodeEntries.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof ChildNodeEntry) {
                ChildNodeEntry childNodeEntry = (ChildNodeEntry) o;
                if (childNodeEntry.getId() instanceof HippoNodeId) {
                    // do not index virtual child nodes, ever
                    continue;
                }
                Document aDoc;
                try {
                    NodeState nodeState = (NodeState) getContext().getItemStateManager().getItemState(
                            childNodeEntry.getId());
                    doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, nodeState.getNodeId().toString(),
                            Field.Store.NO, Field.Index.NO_NORMS));

                    aDoc = createDocument(nodeState, getNamespaceMappings(), indexFormatVersion, true);

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
                } catch (ItemStateException e) {
                    log.warn("ItemStateException while indexing descendants of a hippo:document for "
                            + "node with UUID: " + state.getNodeId().toString(), e);
                } catch (RepositoryException e) {
                    log.warn("RepositoryException while indexing descendants of a hippo:document for "
                            + "node with UUID: " + state.getNodeId().toString(), e);
                }
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

}
