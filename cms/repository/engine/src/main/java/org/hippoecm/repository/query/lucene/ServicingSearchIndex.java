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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeIdIterator;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry.TypeMapping;
import org.apache.jackrabbit.core.query.lucene.DateField;
import org.apache.jackrabbit.core.query.lucene.DoubleField;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.IndexFormatVersion;
import org.apache.jackrabbit.core.query.lucene.IndexingConfigurationEntityResolver;
import org.apache.jackrabbit.core.query.lucene.LongField;
import org.apache.jackrabbit.core.query.lucene.MultiIndex;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.NodeStateIterator;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.SortField;
import org.hippoecm.repository.jackrabbit.HippoNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ServicingSearchIndex extends SearchIndex {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
    protected Document createDocument(NodeState node, NamespaceMappings nsMappings,
            IndexFormatVersion indexFormatVersion) throws RepositoryException {

        return createDocument(node, nsMappings, indexFormatVersion, false);
    }
    
    @Override 
    public void updateNodes(NodeIdIterator remove, NodeStateIterator add) throws RepositoryException, IOException {
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
        for(NodeState addedState : addedStates) {
            try {
                NodeState documentVariant = getVariantDocumentIfAncestor(addedState, checkedIds);
                if(documentVariant != null && !addedIds.contains(documentVariant.getNodeId()) && !reIndexVariantsIds.contains(documentVariant.getNodeId())) {
                    if(!removedIds.contains(documentVariant.getNodeId())) {
                        reIndexVariantsStates.add(documentVariant);
                        reIndexVariantsIds.add(documentVariant.getNodeId());
                    }
                } 
            } catch (ItemStateException e) {
                log.debug("Unable to get state '{}'", e.getMessage());
            }
        }
        
        if(reIndexVariantsStates.size() > 0) {
            super.updateNodes(new NodeIdIteratorImpl(reIndexVariantsIds.iterator()), new NodeStateIteratorImpl(reIndexVariantsStates.iterator()));
        }
    }
    
    
    protected Document createDocument(NodeState node, NamespaceMappings nsMappings,
            IndexFormatVersion indexFormatVersion, boolean aggregateDescendants) throws RepositoryException {
       
        if (node.getId() instanceof HippoNodeId) {
            log.warn("Indexing a virtual node should never happen, and not be possible. Return an empty lucene doc");
            Document doc = new Document();
            return doc;
        }
      
        ServicingNodeIndexer indexer = new ServicingNodeIndexer(node,
                getContext(), nsMappings, super.getTextExtractor());

        indexer.setSupportHighlighting(super.getSupportHighlighting());
        indexer.setServicingIndexingConfiguration((ServicingIndexingConfiguration) super.getIndexingConfig());
        indexer.setIndexFormatVersion(indexFormatVersion);
        Document doc = indexer.createDoc();
        mergeAggregatedNodeIndexes(node, doc);
        
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
    private boolean isDocumentVariant(NodeState node) throws ItemStateException{
        if (this.getIndexingConfig() instanceof ServicingIndexingConfiguration) {
            ServicingIndexingConfiguration sIndexConfig = (ServicingIndexingConfiguration) this.getIndexingConfig();

            ItemStateManager ism = getContext().getItemStateManager();
            
            if(node.getParentId() == null) {
                return false;
            }
            NodeState parent = (NodeState) ism.getItemState(node.getParentId());
            if (parent != null && parent.getNodeTypeName().equals(sIndexConfig.getHippoHandleName()) && !node.getNodeTypeName().equals(sIndexConfig.getHippoRequestName())) {
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
        
        if(checkedIds.contains(state.getNodeId())) {
            // already checked these ancestors: no need to do it again
            return null;
        } 
        checkedIds.add(state.getNodeId());
        if(isDocumentVariant(state)) {
            return state;
        } 
        ItemStateManager ism = getContext().getItemStateManager();
        if(state.getParentId() == null) {
            return null;
        }
        NodeState parent = (NodeState) ism.getItemState(state.getParentId());
        if(parent == null) {
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
                    NodeState nodeState = (NodeState) getContext().getItemStateManager().getItemState(childNodeEntry.getId());
                    aDoc = createDocument(nodeState,
                            getNamespaceMappings(),
                            indexFormatVersion, true);
                    // transfer fields to doc if there are any
                    Fieldable[] fulltextFields = aDoc.getFieldables(FieldNames.FULLTEXT);
                    if (fulltextFields != null) {
                        for (int k = 0; k < fulltextFields.length; k++) {
                            doc.add(fulltextFields[k]);
                        }
                    }
                    
                    // Really important to keep here for updating the aggregate (document variant) when a child is removed
                    Fieldable[] aggrNodeUUID = aDoc.getFieldables(FieldNames.AGGREGATED_NODE_UUID);
                    if(aggrNodeUUID != null) {
                        for(Fieldable f : aggrNodeUUID) {
                            doc.add(f);
                        }
                    }
                    doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID,
                            nodeState.getNodeId().getUUID().toString(),
                            Field.Store.NO,
                            Field.Index.NO_NORMS));
                    //////////////////////////////////
                    
                }catch (ItemStateException e) {
                    log.warn("ItemStateException while indexing descendants of a hippo:document for " +
                            "node with UUID: " + state.getNodeId().getUUID(), e);
                } catch (RepositoryException e) {
                    log.warn("RepositoryException while indexing descendants of a hippo:document for " +
                            "node with UUID: " + state.getNodeId().getUUID(), e);
                }
            }
        }
    }

    // TODO remove when jackrabbit supports sorting on nodename
    @Override
    protected SortField[] createSortFields(Name[] orderProps, boolean[] orderSpecs) {
        SortField[] sortFields = super.createSortFields(orderProps, orderSpecs);
        for (int i = 0; i < orderProps.length; i++) {
            if (orderProps[i].equals(NameConstants.JCR_NAME)) {
                // replace the one created by the one from Jackrabbit, because the jackrabbit cannot sort on jcr:name (core < 1.4.6) 
                sortFields[i] = new SortField(ServicingFieldNames.HIPPO_SORTABLE_NODENAME, !orderSpecs[i]);
            }
        }
        return sortFields;
    }

    @Deprecated 
    private void mergeHippoStandardAggregates(NodeState state, Document doc, IndexFormatVersion indexFormatVersion) {
        if (this.getIndexingConfig() instanceof ServicingIndexingConfiguration) {
            ServicingIndexingConfiguration servicingIndexingConfiguration = (ServicingIndexingConfiguration) this.getIndexingConfig();
  
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
                    try {
                        NodeState nodeState = (NodeState) getContext().getItemStateManager().getItemState(childNodeEntry.getId());
                        Name nodeTypeName = nodeState.getNodeTypeName();
                        Name[] aggr = servicingIndexingConfiguration.getHippoAggregates();
                        for (int i = 0; i < aggr.length; i++) {
                            if (nodeTypeName.equals(aggr[i])) {
                                nodeStates.add(nodeState);
                                // leave after first aggr match
                                break;
                            }
                        }
                    } catch (NoSuchItemStateException e) {
                        log.warn("NoSuchItemStateException while building indexing  hippo standard aggregates for " +
                                "node with UUID: " + state.getNodeId().getUUID(), e);
                    } catch (ItemStateException e) {
                        log.warn("ItemStateException while building indexing  hippo standard aggregates for " +
                                "node with UUID: " + state.getNodeId().getUUID(), e);
                    }
                }

                NodeState[] aggregates = (NodeState[]) nodeStates.toArray(new NodeState[nodeStates.size()]);
                for (int j = 0; j < aggregates.length; j++) {
                    Document aDoc;
                    try {
                        aDoc = createDocument(aggregates[j],
                                getNamespaceMappings(),
                                indexFormatVersion);
                        // transfer fields to doc if there are any
                        Fieldable[] fulltextFields = aDoc.getFieldables(FieldNames.FULLTEXT);
                        if (fulltextFields != null) {
                            for (int k = 0; k < fulltextFields.length; k++) {
                                doc.add(fulltextFields[k]);
                            }
                            // Really important to keep here for updating the aggregate when a child is removed or updated
                            doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID,
                                    aggregates[j].getNodeId().getUUID().toString(),
                                    Field.Store.NO,
                                    Field.Index.NO_NORMS));
                        }
                    } catch (RepositoryException e) {
                        log.warn("RepositoryException while building indexing  hippo standard aggregates for " +
                                "node with UUID: " + state.getNodeId().getUUID(), e);
                    }
                }
            }
        }
    }
    
    private class NodeIdIteratorImpl implements NodeIdIterator {

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

        public Object next() {
            return nextNodeId();
        }
        
    }
   
    
    private class NodeStateIteratorImpl implements NodeStateIterator {
        Iterator process;
        List<NodeState> processedStates = new ArrayList<NodeState>();
        List<NodeId> processedIds = new ArrayList<NodeId>();
       
        NodeStateIteratorImpl(Iterator process){
            this.process = process;
        }
        
        public NodeState nextNodeState() throws NoSuchElementException {
            NodeState state = (NodeState)process.next();
            if(state != null) {
                processedStates.add(state);
                processedIds.add(state.getNodeId());
            }
            return state;
        }

        public boolean hasNext() {
            return process.hasNext();
        }

        public Object next() {
            return nextNodeState();
        }

        public void remove() {
             process.remove();
        }
        
    }
 
    // TODO for some reason, jackrabbit's PropertyTypeRegistry sometimes seem to incorrectly return that it does not know the type of some property. Hence, for now, better not
    // use this method
    // jcrPropertyName is of format : {namespace}:localname
    public int getPropertyType(String namespacedProperty){
        try {
            // try to get the Name for jcrPropertyName
            Name facetName = NameFactoryImpl.getInstance().create(namespacedProperty);
            TypeMapping[] typeMappings = getContext().getPropertyTypeRegistry().getPropertyTypes(facetName);
            if(typeMappings.length == 0) {
                log.debug("Property name '{}' not mapped in cnd: do not know how to convert from lucene term. Return lucene term", namespacedProperty);
            }
            else if(typeMappings.length > 1) {
                log.debug("Same property name '{}' mapped to multiple types: do not know how to convert from lucene term. Return lucene term", namespacedProperty);
            }  else {
               return typeMappings[0].type;
            }
        } catch (IllegalArgumentException e) {
           log.debug("Cannot get Name for '{}'. Return the luceneTerm as is.", namespacedProperty); 
        }
        return PropertyType.UNDEFINED;
    }
    
}
