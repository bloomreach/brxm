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

import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.IndexFormatVersion;
import org.apache.jackrabbit.core.query.lucene.IndexingConfigurationEntityResolver;
import org.apache.jackrabbit.core.query.lucene.MultiIndex;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
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

        if (node.getId() instanceof HippoNodeId) {
            log.warn("Indexing a virtual node should never happen, and not be possible. Return an empty lucene doc");
            Document doc = new Document();
            return doc;
        }

        ServicingNodeIndexer indexer = new ServicingNodeIndexer(node,
                getContext(), nsMappings, super.getTextExtractor());

        indexer.setSupportHighlighting(super.getSupportHighlighting());
        // indexer.setIndexingConfiguration(indexingConfig);
        indexer.setServicingIndexingConfiguration((ServicingIndexingConfiguration) super.getIndexingConfig());
        indexer.setIndexFormatVersion(indexFormatVersion);
        Document doc = indexer.createDoc();
        mergeAggregatedNodeIndexes(node, doc);
        mergeHippoStandardAggregates(node, doc, indexFormatVersion);
        return doc;
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
                        Name nodeName = nodeState.getNodeTypeName();
                        Name[] aggr = servicingIndexingConfiguration.getHippoAggregates();
                        for (int i = 0; i < aggr.length; i++) {
                            if (nodeName.equals(aggr[i])) {
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
}
