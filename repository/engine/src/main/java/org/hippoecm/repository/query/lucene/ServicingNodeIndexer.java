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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.jackrabbit.core.id.PropertyId;
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
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicingNodeIndexer extends NodeIndexer {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

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
    
    /**
     * List where the binaries are stored before being actually indexed: when there is a hippo:text binary in the documentBinaries, we 
     * then skip the other binaries indexing: the hippo:text is there as the extracted version of the real binary 
     * 
     */
    private List<BinaryValue> documentBinaries = null;

    private QueryHandlerContext queryHandlerContext;

    public ServicingNodeIndexer(NodeState node, QueryHandlerContext context, NamespaceMappings mappings, Parser parser) {
        super(node, context.getItemStateManager(), mappings, context.getExecutor(), parser);
        this.queryHandlerContext = context;
    }

    public void setServicingIndexingConfiguration(ServicingIndexingConfiguration config) {
        super.setIndexingConfiguration(config);
        this.servicingIndexingConfig = config;
    }

    
    @Override
    protected void addBinaryValue(Document doc, String fieldName, InternalValue internalValue) {
        if(documentBinaries == null) {
            documentBinaries = new ArrayList<BinaryValue>();
        }
        documentBinaries.add(new BinaryValue(fieldName, internalValue));
    }

    private class BinaryValue {
        private Object internalValue;
        private String fieldName;
        public BinaryValue(String fieldName, Object internalValue) {
            this.internalValue = internalValue;
            this.fieldName = fieldName;
        }
    }
    
    @Override
    public Document createDoc() throws RepositoryException {
        // index the jackrabbit way
        Document doc = super.createDoc();
        
        /*
         * check the documentBinaries list: if it is not null, we still need to index the binaries, as we temporarily store them.
         * If the documentBinaries list size is bigger than 1, we need to inspect whether there is a hippo:text binary: in that case,
         * we index only the hippo:text binary, as this is the 'extracted' version of the real binary
         */  
        
        if(this.documentBinaries != null && !documentBinaries.isEmpty()) {
            try {
                String hippoTextName = resolver.getJCRName(servicingIndexingConfig.getHippoTextPropertyName());
                if (documentBinaries.size() > 1) {
                    boolean hippoTextFieldPresent = false;
                    // inspect whether there is a hippo:text version
                    for (BinaryValue binVal : documentBinaries) {
                        if (hippoTextName.equals(binVal.fieldName)) {
                            hippoTextFieldPresent = true;
                        }
                    }
                    if (hippoTextFieldPresent) {
                        log.debug("The '{}' property is present and thus will be used to index this binary.",
                                HippoNodeType.HIPPO_TEXT);
                        for (BinaryValue binval : documentBinaries) {
                            if (hippoTextName.equals(binval.fieldName)) {
                                try {
                                    InternalValue type = getValue(NameConstants.JCR_MIMETYPE);
                                    if (type != null) {
                                        Metadata metadata = new Metadata();
                                        metadata.set(Metadata.CONTENT_TYPE, type.getString());
                                        InternalValue encoding = getValue(NameConstants.JCR_ENCODING);
                                        if (encoding != null) {
                                            metadata.set(Metadata.CONTENT_ENCODING, encoding.getString());
                                        }
                                        doc.add(createFulltextField((InternalValue)binval.internalValue, metadata));
                                    }
                                } catch (ItemStateException e) {
                                    log.warn("Exception during indexing hippo:text binary property", e);
                                }
                            }
                        }
                    } else {
                        for (BinaryValue val : documentBinaries) {
                            super.addBinaryValue(doc, val.fieldName, (InternalValue) val.internalValue);
                        }
                    }

                } else {
                    BinaryValue binVal = documentBinaries.get(0);
                    if (hippoTextName.equals(binVal.fieldName)) {
                        log.debug("The '{}' property is present and thus will be used to index this binary.",
                                HippoNodeType.HIPPO_TEXT);
                        try {
                            InternalValue type = getValue(NameConstants.JCR_MIMETYPE);
                            if (type != null) {
                                Metadata metadata = new Metadata();
                                metadata.set(Metadata.CONTENT_TYPE, type.getString());
                                InternalValue encoding = getValue(NameConstants.JCR_ENCODING);
                                if (encoding != null) {
                                    metadata.set(Metadata.CONTENT_ENCODING, encoding.getString());
                                }
                                doc.add(createFulltextField((InternalValue)binVal.internalValue, metadata));
                            }
                        } catch (ItemStateException e) {
                            log.warn("Exception during indexing hippo:text binary property", e);
                        }
                    } else {
                        // fallback to original Jackrabbit binary indexing
                        super.addBinaryValue(doc, binVal.fieldName, (InternalValue) binVal.internalValue);
                    }
                }
            } catch (NamespaceException e) {
                // will never happen
                log.error("Error trying to create lucene internal field for " + HippoNodeType.HIPPO_TEXT + "", e);
            }
        }
        
        
        // plus index our facet specifics & hippo extra's 
        try {
            if (node.getParentId() != null) { // skip root node
                NodeState parent = (NodeState) stateProvider.getItemState(node.getParentId());
                ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
                if (child == null) {
                    throw new RepositoryException("Missing child node entry " + "for node with id: " + node.getNodeId());
                }
                String nodename = child.getName().getLocalName();
                String prefix = null;
                if (child.getName().getNamespaceURI() != null && !"".equals(child.getName().getNamespaceURI())) {
                    prefix = queryHandlerContext.getNamespaceRegistry().getPrefix(child.getName().getNamespaceURI());
                    nodename = prefix + ":" + nodename;
                }
                // index the nodename to sort on
                doc.add(new Field(ServicingFieldNames.HIPPO_SORTABLE_NODENAME, nodename, Field.Store.NO,
                        Field.Index.NO_NORMS, Field.TermVector.NO));

                /**
                 * index the nodename to search on. We index this as hippo:_localname, a pseudo property which does not really exist but
                 * only meant to search on
                 */
                if(this.servicingIndexingConfig.isNodeNameIndexingEnabled()) {
                    indexNodeName(doc, child.getName().getLocalName());
                }
                
                // TODO ARD: imo this code does not belong in the node indexer: if aggregation is needed, it should be in the servicing search index arranged 
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
                                        String s = resolver.getJCRName(propState.getName()) + "/"
                                                + resolver.getJCRName(childNode.getName());
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
        Field localNameField = new Field(fieldName, localName, Field.Store.NO, Field.Index.TOKENIZED,
                Field.TermVector.NO);
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
    private void addFacetValue(Document doc, InternalValue value, String fieldName, Name name) throws RepositoryException {

        switch (value.getType()) {
        case PropertyType.BINARY:
            // never facet;
            break;
        case PropertyType.BOOLEAN:
            indexFacet(doc, fieldName, value.toString());
            break;
        case PropertyType.DATE:
            indexDateFacet(doc, fieldName, value.getDate());
            break;
        case PropertyType.DOUBLE:
            indexDoubleFacet(doc, fieldName, value.getDouble());
            break;
        case PropertyType.LONG:
            indexLongFacet(doc, fieldName, value.getLong());
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
                    log
                            .debug("truncating facet value because string length exceeds 255 chars. This is useless for facets");
                    str = str.substring(0, 255);
                }
                indexStringFacet(doc, fieldName, str);
            }
            break;
        case PropertyType.NAME:
            if (name.equals(NameConstants.JCR_PRIMARYTYPE)) {
                indexNodeTypeNameFacet(doc, ServicingFieldNames.HIPPO_PRIMARYTYPE, value.getName());
            } else if (name.equals(NameConstants.JCR_MIXINTYPES)) {
                indexNodeTypeNameFacet(doc, ServicingFieldNames.HIPPO_MIXINTYPE, value.getName());
            }
            try {
                // nodename in format: nsprefix:localname
                String prefix = queryHandlerContext.getNamespaceRegistry().getPrefix(value.getName().getNamespaceURI());
                indexFacet(doc, fieldName, prefix + ":" + value.getName().getLocalName());
            } catch (NamespaceException e) {
                log.error("Could not get primaryNodeName in format nsprefix:localname for '{}'", value.getName());
            }
            break;
        default:
            throw new IllegalArgumentException("illegal internal value type");
        }
    }

    @Override
    protected void addStringValue(Document doc, String fieldName, String internalValue, boolean tokenized,
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
                        log.debug("Cannot (yet) add name " + n + " to the exlude set from nodescope. Most likely the namespace still has to be registered.",
                                   e.getMessage());
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
                        log.debug("Cannot (yet) add name "  + n  + " to the exlude set for properties not to index a single term from. Most likely the namespace still has to be registered.",
                                   e.getMessage());
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

    private void indexFacet(Document doc, String fieldName, String value, Field.TermVector termVector) {
        doc.add(new Field(ServicingFieldNames.FACET_PROPERTIES_SET, fieldName, Field.Store.NO, Field.Index.NO_NORMS,
                Field.TermVector.NO));
        String internalFacetName = ServicingNameFormat.getInternalFacetName(fieldName);
        doc.add(new Field(internalFacetName, value, Field.Store.NO, Field.Index.NO_NORMS, termVector));
    }

    private void indexFacet(Document doc, String fieldName, String value) {
        indexFacet(doc, fieldName, value, Field.TermVector.NO);
    }

    private void indexDateFacet(Document doc, String fieldName, Calendar calendar) {
        doc.add(new Field(ServicingFieldNames.FACET_PROPERTIES_SET, fieldName, Field.Store.NO, Field.Index.NO_NORMS,
                Field.TermVector.NO));
        
        Map<String, String> resolutions = new HashMap<String, String>();
        resolutions.put("year", HippoDateTools.timeToString(calendar.getTimeInMillis(),
                HippoDateTools.Resolution.YEAR));
        resolutions.put("month", HippoDateTools.timeToString(calendar.getTimeInMillis(),
                HippoDateTools.Resolution.MONTH));
        resolutions.put("week", HippoDateTools.timeToString(calendar.getTimeInMillis(),
                HippoDateTools.Resolution.WEEK));
        resolutions.put("day", HippoDateTools.timeToString(calendar.getTimeInMillis(),
                HippoDateTools.Resolution.DAY));
        resolutions.put("hour", HippoDateTools.timeToString(calendar.getTimeInMillis(),
                HippoDateTools.Resolution.HOUR));
        resolutions.put("minute", HippoDateTools.timeToString(calendar.getTimeInMillis(),
                HippoDateTools.Resolution.MINUTE));
        resolutions.put("second", HippoDateTools.timeToString(calendar.getTimeInMillis(),
                HippoDateTools.Resolution.SECOND));

        Map<String, Integer> byDateNumbers = new HashMap<String, Integer>();
        byDateNumbers.put("year", calendar.get(Calendar.YEAR));
        byDateNumbers.put("month", calendar.get(Calendar.MONTH));
        byDateNumbers.put("week", calendar.get(Calendar.WEEK_OF_YEAR));
        byDateNumbers.put("dayofyear", calendar.get(Calendar.DAY_OF_YEAR));
        byDateNumbers.put("dayofweek", calendar.get(Calendar.DAY_OF_WEEK));
        byDateNumbers.put("day", calendar.get(Calendar.DAY_OF_MONTH));
        byDateNumbers.put("hour", calendar.get(Calendar.HOUR_OF_DAY));
        byDateNumbers.put("minute", calendar.get(Calendar.MINUTE));
        byDateNumbers.put("second", calendar.get(Calendar.SECOND));
        String internalFacetName = ServicingNameFormat.getInternalFacetName(fieldName);
        
        String dateToString = String.valueOf(calendar.getTimeInMillis());
        doc.add(new Field(internalFacetName, dateToString, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));

        for (Entry<String, String> keyValue : resolutions.entrySet()) {
            String compoundFieldName = fieldName + ServicingFieldNames.DATE_RESOLUTION_DELIMITER + keyValue.getKey();
            indexFacet(doc, compoundFieldName, keyValue.getValue());
        }

        for (Entry<String, Integer> keyValue : byDateNumbers.entrySet()) {
            String compoundFieldName = fieldName + ServicingFieldNames.DATE_NUMBER_DELIMITER + keyValue.getKey();
            indexFacet(doc, compoundFieldName, String.valueOf(keyValue.getValue()));
        }
    }

    private void indexLongFacet(Document doc, String fieldName, long value) {
        indexFacet(doc, fieldName, String.valueOf(value));
        String compoundFieldName = fieldName + ServicingFieldNames.LONG_POSTFIX;
        // for efficient range queries on long fields, we also index a legical format and store term vector
        indexFacet(doc, compoundFieldName, LongField.longToString(value), Field.TermVector.YES);
    }

    private void indexStringFacet(Document doc, String fieldName, String value) {
        indexFacet(doc, fieldName, value);

        // lowercase index the the first, first 2 and first 3 chars in seperate fields
        for (int i = 1; i <= 3; i++) {
            String ngram = fieldName + ServicingFieldNames.STRING_DELIMITER + i
                    + ServicingFieldNames.STRING_CHAR_POSTFIX;
            ;
            if (value.length() > i) {
                indexFacet(doc, ngram, value.substring(0, i).toLowerCase());
            } else {
                indexFacet(doc, ngram, value.toLowerCase());
            }
        }

    }

    private void indexDoubleFacet(Document doc, String fieldName, double value) {
        indexFacet(doc, fieldName, String.valueOf(value));
        String compoundFieldName = fieldName + ServicingFieldNames.DOUBLE_POSTFIX;
        // for efficient range queries on long fields, we also index a legical format and store term vector
        indexFacet(doc, compoundFieldName, DoubleField.doubleToString(value), Field.TermVector.YES);
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
                doc.add(new Field(ServicingFieldNames.HIPPO_PATH, value.toString(), Field.Store.NO,
                        Field.Index.NO_NORMS, Field.TermVector.NO));
            }
        }

        // make lexical sorting on depth possible. Max depth = 999;
        String depth = String.valueOf(values.length);
        depth = "000".substring(depth.length()).concat(depth);
        doc.add(new Field(ServicingFieldNames.HIPPO_DEPTH, depth, Field.Store.NO, Field.Index.NO_NORMS,
                Field.TermVector.NO));
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
}
