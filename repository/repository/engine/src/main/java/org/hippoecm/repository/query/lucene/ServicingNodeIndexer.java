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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.DoubleField;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.LongField;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.NodeIndexer;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hippoecm.repository.jackrabbit.FacetTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicingNodeIndexer extends NodeIndexer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(ServicingNodeIndexer.class);

    /**
     * Set of exclude nodescope fieldNames
     */
    private Set<String> excludeFieldNamesFromNodeScope;

    /**
     * boolean indicating all excluded names have been added
     */
    private boolean addedAllExcludeFieldNames = false;

    /**
     * Set of exclude properties for index single term
     */
    private Set<String> excludePropertiesSingleIndexTerm;

    /**
     * boolean indicating all excluded properties have been added
     */
    private boolean addedAllExcludePropertiesSingleIndexTerm = false;

    /**
     * The indexing configuration or <code>null</code> if none is available.
     */
    protected ServicingIndexingConfiguration servicingIndexingConfig;

    private QueryHandlerContext queryHandlerContext;

    public ServicingNodeIndexer(NodeState node, QueryHandlerContext queryHandlerContext, NamespaceMappings mappings, TextExtractor extractor) {
        super(node, queryHandlerContext.getItemStateManager(), mappings, extractor);
        this.queryHandlerContext = queryHandlerContext;
    }

    public void setServicingIndexingConfiguration(ServicingIndexingConfiguration config) {
        super.setIndexingConfiguration(config);
        this.servicingIndexingConfig = config;
    }

    @Override
    protected Document createDoc() throws RepositoryException {
        // index the jackrabbit way
        Document doc = super.createDoc();
        // plus index our facet specifics

        // TODO : only index facets for hippo:document + subtypes
        try {

            if (node.getParentId() != null) { // skip root node
                NodeState parent = (NodeState) stateProvider.getItemState(node.getParentId());
                ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
                if (child == null) {
                    throw new RepositoryException("Missing child node entry " +
                            "for node with id: " + node.getNodeId());
                }
                String nodename = child.getName().getLocalName();
                String prefix = null;
                if (child.getName().getNamespaceURI() != null && !"".equals(child.getName().getNamespaceURI())) {
                    prefix = queryHandlerContext.getNamespaceRegistry().getPrefix(child.getName().getNamespaceURI());
                    nodename = prefix + ":" + nodename;
                }
                // index the nodename to sort on
                doc.add(new Field(ServicingFieldNames.HIPPO_SORTABLE_NODENAME, nodename, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));

                for (Iterator childNodeIter = node.getChildNodeEntries().iterator(); childNodeIter.hasNext();) {
                    ChildNodeEntry childNode = (ChildNodeEntry) childNodeIter.next();
                    NodeState childState = (NodeState) stateProvider.getItemState(childNode.getId());
                    if (servicingIndexingConfig.isChildAggregate(childState.getNodeTypeName())) {
                        Set props = childState.getPropertyNames();
                        for (Iterator it = props.iterator(); it.hasNext();) {
                            Name propName = (Name) it.next();
                            PropertyId id = new PropertyId(childNode.getId(), propName);
                            try {
                                PropertyState propState = (PropertyState) stateProvider.getItemState(id);
                                InternalValue[] values = propState.getValues();
                                if (!isHippoPath(propName) && isFacet(propName)) {
                                    for (int i = 0; i < values.length; i++) {
                                        String s = resolver.getJCRName(propState.getName()) + "/" + resolver.getJCRName(childNode.getName());
                                        addFacetValue(doc, values[i], s, propState.getName());
                                    }
                                }
                            } catch (NoSuchItemStateException e) {
                                throwRepositoryException(e);
                            } catch (ItemStateException e) {
                                throwRepositoryException(e);
                            }
                        }
                    }
                }
                
                /**
                 * index the nodename to search on. We index this as hippo:_localname, a pseudo property which does not really exist but
                 * only meant to search on
                 */
                indexNodeName(doc, child.getName().getLocalName());
            }
        } catch (ItemStateException e) {
            throwRepositoryException(e);
        }

        Set props = node.getPropertyNames();
        for (Iterator it = props.iterator(); it.hasNext();) {
            Name propName = (Name) it.next();
            PropertyId id = new PropertyId(node.getNodeId(), propName);
            try {
                PropertyState propState = (PropertyState) stateProvider.getItemState(id);
                InternalValue[] values = propState.getValues();
                if (isHippoPath(propName)) {
                    indexPath(doc, values, propState.getName());
                } else if (isFacet(propName)) {
                    for (int i = 0; i < values.length; i++) {
                        addFacetValue(doc, values[i], resolver.getJCRName(propState.getName()), propState.getName());
                    }
                }
            } catch (NoSuchItemStateException e) {
                throwRepositoryException(e);
            } catch (ItemStateException e) {
                throwRepositoryException(e);
            }
        }
        return doc;
    }

    private void indexNodeName(Document doc, String localName) {
        // simple String
        String hippo_ns_prefix = null;
        try {
            hippo_ns_prefix = this.mappings.getPrefix(this.servicingIndexingConfig.getHippoNamespaceURI());
        } catch (NamespaceException e) {
            //log.warn("Cannot get 'hippo' lucene prefix. ", e.getMessage());
            return;
        }
        String fieldName = hippo_ns_prefix + ":" + FieldNames.FULLTEXT_PREFIX + "_localname";
        Field localNameField = new Field(fieldName, localName, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO);
        localNameField.setBoost(5);
        doc.add(localNameField);

        // also create fulltext index of this value
        Field localNameFullTextField;
        if (supportHighlighting) {
            localNameFullTextField = new Field(FieldNames.FULLTEXT, localName, Field.Store.NO, Field.Index.TOKENIZED,
                    Field.TermVector.WITH_OFFSETS);
        } else {
            localNameFullTextField = new Field(FieldNames.FULLTEXT, localName, Field.Store.NO, Field.Index.TOKENIZED,
                    Field.TermVector.NO);
        }
        doc.add(localNameFullTextField);
    }

    // below: When the QName is configured to be a facet, also index like one
    private void addFacetValue(Document doc, InternalValue value, String fieldName, Name name) {
        switch (value.getType()) {
            case PropertyType.BINARY:
                // never facet;
                break;
            case PropertyType.BOOLEAN:
                indexFacet(doc, fieldName, value.toString() + FacetTypeConstants.BOOLEAN_POSTFIX);
                break;
            case PropertyType.DATE:
                // TODO : configurable resolution for dates: currently SECONDS
                String dateToString = DateTools.timeToString(value.getDate().getTimeInMillis(), DateTools.Resolution.SECOND);
                indexFacet(doc, fieldName, dateToString + FacetTypeConstants.DATE_POSTFIX);
                break;
            case PropertyType.DOUBLE:
                indexFacet(doc, fieldName, DoubleField.doubleToString(new Double(value.getDouble()).doubleValue()) + FacetTypeConstants.DOUBLE_POSTFIX);
                break;
            case PropertyType.LONG:
                indexFacet(doc, fieldName, LongField.longToString(new Long(value.getLong())) + FacetTypeConstants.LONG_POSTFIX);
                break;
            case PropertyType.REFERENCE:
                // never facet;
                break;
            case PropertyType.PATH:
                // never facet;
                break;
            case PropertyType.STRING:
                // never index uuid as facet
                if (!name.equals(NameConstants.JCR_UUID)) {
                    String str = value.toString();
                    if (str.length() > 255) {
                        log.debug("truncating facet value because string length exceeds 255 chars. This is useless for facets");
                        str = str.substring(0, 255);
                    }
                    indexFacet(doc, fieldName, str + FacetTypeConstants.STRING_POSTFIX);
                }
                break;
            case PropertyType.NAME:
                if (name.equals(NameConstants.JCR_PRIMARYTYPE)) {
                    indexNodeTypeNameFacet(doc, ServicingFieldNames.HIPPO_PRIMARYTYPE, value.getQName());
                } else if (name.equals(NameConstants.JCR_MIXINTYPES)) {
                    indexNodeTypeNameFacet(doc, ServicingFieldNames.HIPPO_MIXINTYPE, value.getQName());
                }
                break;
            default:
                throw new IllegalArgumentException("illegal internal value type");
        }
    }

    @Override
    protected void addStringValue(Document doc, String fieldName, Object internalValue, boolean tokenized,
                                  boolean includeInNodeIndex, float boost, boolean useInExcerpt) {
        if (!addedAllExcludeFieldNames) {
            // init only when not all excluded nodenames have been resolved before
            synchronized (this) {
                int excCount = 0;
                excludeFieldNamesFromNodeScope = new HashSet<String>();
                for (Name n : servicingIndexingConfig.getExcludedFromNodeScope()) {
                    try {
                        excludeFieldNamesFromNodeScope.add(resolver.getJCRName(n));
                    } catch (NamespaceException e) {
                        excCount++;
                        log.debug("Cannot (yet) add name " + n + " to the exlude set from nodescope. Most likely the namespace still has to be registered.", e.getMessage());
                    }
                }
                if (excCount == 0) {
                    addedAllExcludeFieldNames = true;
                }
            }
        }
        if (!addedAllExcludePropertiesSingleIndexTerm) {
            // init only when not all excluded properties have been resolved before
            synchronized (this) {
                int excCount = 0;
                excludePropertiesSingleIndexTerm = new HashSet<String>();
                for (Name n : servicingIndexingConfig.getExcludePropertiesSingleIndexTerm()) {
                    try {
                        excludePropertiesSingleIndexTerm.add(resolver.getJCRName(n));
                    } catch (NamespaceException e) {
                        excCount++;
                        log.debug("Cannot (yet) add name " + n + " to the exlude set for properties not to index a single term from. Most likely the namespace still has to be registered.", e.getMessage());
                    }
                }
                if (excCount == 0) {
                    addedAllExcludePropertiesSingleIndexTerm = true;
                }
            }
        }

        if (excludeFieldNamesFromNodeScope.contains(fieldName)) {
            log.debug("Do not nodescope/tokenize fieldName : {}", fieldName);
            super.addStringValue(doc, fieldName, internalValue, false, false, boost, false);
        } else if (excludePropertiesSingleIndexTerm.contains(fieldName)) {
            super.addStringValue(doc, fieldName, internalValue, tokenized, includeInNodeIndex, boost, false);
        } else {
            super.addStringValue(doc, fieldName, internalValue, tokenized, includeInNodeIndex, boost, useInExcerpt);
        }
    }

    private void indexFacet(Document doc, String fieldName, String value) {
        doc.add(new Field(ServicingFieldNames.FACET_PROPERTIES_SET, fieldName, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
        int idx = fieldName.indexOf(':');
        fieldName = fieldName.substring(0, idx + 1) + ServicingFieldNames.HIPPO_FACET + fieldName.substring(idx + 1);
        doc.add(new Field(fieldName, value, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.YES));
    }

    protected void indexNodeTypeNameFacet(Document doc, String fieldName, Object internalValue) {
        try {
            Name qualiName = (Name) internalValue;
            String normValue = mappings.getPrefix(qualiName.getNamespaceURI()) + ":" + qualiName.getLocalName();
            doc.add(new Field(fieldName, normValue, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
        } catch (NamespaceException e) {
            // will never happen
        }
    }

    private void indexPath(Document doc, InternalValue[] values, Name name) {

        // index each level of the path for searching
        for (int i = 0; i < values.length; i++) {
            InternalValue value = values[i];
            if (value.getType() == PropertyType.STRING) {
                doc.add(new Field(ServicingFieldNames.HIPPO_PATH, value.toString(), Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
            }
        }

        // make lexical sorting on depth possible. Max depth = 999;
        String depth = String.valueOf(values.length);
        depth = "000".substring(depth.length()).concat(depth);
        doc.add(new Field(ServicingFieldNames.HIPPO_DEPTH, depth, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
    }

    /**
     * Returns <code>true</code> if the property with the given name should be
     * indexed.
     *
     * @param propertyName name of a property.
     * @return <code>true</code> if the property is a facet;
     *         <code>false</code> otherwise.
     */
    protected boolean isFacet(Name propertyName) {
        if (servicingIndexingConfig == null) {
            return false;
        } else {
            return servicingIndexingConfig.isFacet(propertyName);
        }
    }

    /**
     * Returns <code>true</code> if the property with the given name should be
     * indexed as hippo path.
     *
     * @param propertyName name of a property.
     * @return <code>true</code> if the property is a hippo path;
     *         <code>false</code> otherwise.
     */
    protected boolean isHippoPath(Name propertyName) {
        if (servicingIndexingConfig == null) {
            return false;
        } else {
            return servicingIndexingConfig.isHippoPath(propertyName);
        }
    }

    /**
     * Wraps the exception <code>e</code> into a <code>RepositoryException</code>
     * and throws the created exception.
     *
     * @param e the base exception.
     */
    private void throwRepositoryException(Exception e)
            throws RepositoryException {
        String msg = "Error while indexing node: " + node.getNodeId() + " of " + "type: " + node.getNodeTypeName();
        throw new RepositoryException(msg, e);
    }
}
