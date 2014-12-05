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
package org.hippoecm.repository.query.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
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
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.tika.parser.Parser;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicingNodeIndexer extends NodeIndexer {

    private static final Logger log = LoggerFactory.getLogger(ServicingNodeIndexer.class);

    /**
     * List where the binaries are stored before being actually indexed: when there is a hippo:text binary in the documentBinaries, we
     * then skip the other binaries indexing: the hippo:text is there as the extracted version of the real binary
     */
    private List<BinaryValue> binaryValues = null;

    private String hippoTextPropertyName;

    private BinaryValue hippoTextValue;

    private final QueryHandlerContext queryHandlerContext;

    protected ServicingIndexingConfiguration servicingIndexingConfig;
    private boolean supportSimilarityOnStrings = true;
    private boolean supportSimilarityOnBinaries;

    public ServicingNodeIndexer(NodeState node, QueryHandlerContext context, NamespaceMappings mappings, Parser parser) {
        super(node, context.getItemStateManager(), mappings, context.getExecutor(), parser);
        this.queryHandlerContext = context;
    }

    public void setServicingIndexingConfiguration(ServicingIndexingConfiguration config) {
        super.setIndexingConfiguration(config);
        this.servicingIndexingConfig = config;
    }

    @Override
    public Document createDoc() throws RepositoryException {
        Document doc = super.createDoc();

        // index ancestor uuids + self
        indexUuidsHierarchy(doc);

        addBinaries(doc);

        indexNodeName(doc);

        for (final Name propName : node.getPropertyNames()) {
            PropertyId id = new PropertyId(node.getNodeId(), propName);
            if (isHippoPath(propName)) {
                addHippoPath(doc, id);
            }
            if (isFacet(propName)) {
                addFacetValues(doc, id, propName);
            }
        }
        return doc;
    }

    private void indexUuidsHierarchy(final Document doc) throws RepositoryException {
        try {
            NodeState current = node;
            for (;;) {
                doc.add(new Field(ServicingFieldNames.HIPPO_UUIDS, current.getId().toString(), Field.Store.NO,
                        Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
                if (current.getParentId() == null) {
                    // root node or free floating
                    break;
                }
                current = (NodeState) stateProvider.getItemState(current.getParentId());
            }

        } catch (ItemStateException e) {
            throwRepositoryException(e);
        }
    }

    private void addFacetValues(final Document doc, final PropertyId id, final Name propName) throws RepositoryException {
        final String fieldName = resolver.getJCRName(propName);
        indexFacetProperty(doc, fieldName);

        try {
            PropertyState propState = (PropertyState) stateProvider.getItemState(id);
            InternalValue[] values = propState.getValues();
            for (final InternalValue value : values) {
                addFacetValue(doc, value, fieldName, propName);
            }
        } catch (ItemStateException e) {
            throwRepositoryException(e);
        }
    }

    @Override
    protected void addBinaryValue(Document doc, String fieldName, InternalValue internalValue) {
        // we override the way binaries are handled in {@link #createDoc}
        if (hippoTextValue == null) {
            final BinaryValue binaryValue = new BinaryValue(fieldName, internalValue);
            if (binaryValue.isHippoTextValue()) {
                hippoTextValue = binaryValue;
                binaryValues = null;
            } else {
                if(binaryValues == null) {
                    binaryValues = new ArrayList<BinaryValue>();
                }
                binaryValues.add(binaryValue);
            }
        }
    }

    @Override
    protected void addCalendarValue(Document doc, String fieldName, Calendar internalValue) {
        super.addCalendarValue(doc, fieldName, internalValue);

        final long timeInMillis = internalValue.getTimeInMillis();
        for (DateTools.Resolution resolution : DateTools.getSupportedResolutions()) {
            String propertyNameForResolution = DateTools.getPropertyForResolution(fieldName, resolution);
            Calendar roundedForResolution = DateTools.roundDate(timeInMillis, resolution);
            super.addCalendarValue(doc, propertyNameForResolution, roundedForResolution);
        }
    }

    @Override
    protected void addStringValue(Document doc, String fieldName, String internalValue, boolean tokenized,
                                  boolean includeInNodeIndex, float boost, boolean useInExcerpt) {
        boolean includeSingleIndexTerm = true;
        if (isExcludedFromNodeScope(fieldName)) {
            includeInNodeIndex = false;
        } else if (isExcludedSingleIndexTerm(fieldName)) {
            includeSingleIndexTerm = false;
        }
        addStringValue(doc, fieldName, internalValue, tokenized, includeInNodeIndex, boost, useInExcerpt, includeSingleIndexTerm);
    }

    /**
     * The method below is same as  NodeIndexer#addStringValue only one extra check <code>includeSingleIndexTerm</code> to
     * include or exclude the property as a single term in the index. For example, for our html fields, we do not need to support
     * in queries \@hippostd:content = 'some long text', and obviously, we do not need to support sorting on these fields either.
     * @see {@link NodeIndexer#addStringValue(org.apache.lucene.document.Document, String, String, boolean, boolean, float, boolean)}
     */
    protected void addStringValue(Document doc, String fieldName,
                                  String internalValue, boolean tokenized,
                                  boolean includeInNodeIndex, float boost,
                                  boolean useInExcerpt,
                                  boolean includeSingleIndexTerm) {
        if (includeSingleIndexTerm) {
            // simple String
            doc.add(createFieldWithoutNorms(fieldName, internalValue,
                    PropertyType.STRING));
        }
        if (tokenized) {
            if (internalValue.length() == 0) {
                return;
            }
            // create fulltext index on property
            int idx = fieldName.indexOf(':');
            fieldName = fieldName.substring(0, idx + 1)
                    + FieldNames.FULLTEXT_PREFIX + fieldName.substring(idx + 1);
            boolean hasNorms = boost != DEFAULT_BOOST;
            Field.Index indexType = hasNorms ? Field.Index.ANALYZED
                    : Field.Index.ANALYZED_NO_NORMS;
            Field f = new Field(fieldName, true, internalValue, Field.Store.NO,
                    indexType, Field.TermVector.NO);
            f.setBoost(boost);
            doc.add(f);

            if (includeInNodeIndex) {
                f = createFulltextField(internalValue, false, supportSimilarityOnStrings, false);
                if (useInExcerpt) {
                    doc.add(f);
                } else {
                    doNotUseInExcerpt.add(f);
                }
            }
        }
    }

    /**
     * Creates a fulltext field for the string <code>value</code>. Overridden in order to reduce size of the index.
     * The {@code store} field is ignored, fields are never stored. The {@code withNorms} is also ignored. We always
     * analyse the field as it hardly takes any space or memory.
     *
     * @param value the string value.
     * @param store We ignore <code>store</code> as this increases lucene size while at the same time
     *              excerpts are of poor quality
     * @param termVectors if a term vector with offsets should be stored.
     * @param withNorms : We ignore 'withNorms' : It takes hardly space or memory for the full text field. We always
     *                  use Field.Index.ANALYZED
     *
     * @return a lucene field.
     */
    protected Field createFulltextField(String value,
                                        boolean store,
                                        boolean termVectors,
                                        boolean withNorms) {
        Field.TermVector tv;
        if (termVectors) {
            tv = Field.TermVector.YES;
        } else {
            tv = Field.TermVector.NO;
        }
        return new Field(FieldNames.FULLTEXT, false, value,Field.Store.NO, Field.Index.ANALYZED, tv);
    }

    @Override
    protected void throwRepositoryException(final Exception e) throws RepositoryException {
        if (e instanceof NoSuchItemStateException) {
            throw new ItemNotFoundException(e);
        }
        super.throwRepositoryException(e);
    }

    // below: When the QName is configured to be a facet, also index like one
    protected void addFacetValue(Document doc, InternalValue value, String fieldName, Name name) throws RepositoryException {
        switch (value.getType()) {
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
        case PropertyType.STRING:
            // never index uuid as facet
            if (!name.equals(NameConstants.JCR_UUID)) {
                String str = value.toString();
                if (str.length() > 255) {
                    log.debug("truncating facet value because string length exceeds 255 chars. This is useless for facets");
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
            // type cannot be a facet
            log.debug("Can't create facet for type '{}'", PropertyType.nameFromValue(value.getType()));
            break;
        }
    }

    private void indexNodeTypeNameFacet(Document doc, String fieldName, Name internalValue) {
        try {
            String value = mappings.getPrefix(internalValue.getNamespaceURI()) + ":" + internalValue.getLocalName();
            doc.add(new Field(fieldName, value, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
        } catch (NamespaceException ignore) {}
    }

    protected boolean isFacet(Name propertyName) {
        return servicingIndexingConfig != null && servicingIndexingConfig.isFacet(propertyName);
    }

    protected boolean isHippoPath(Name propertyName) {
        return servicingIndexingConfig != null && servicingIndexingConfig.isHippoPath(propertyName);
    }

    protected NamePathResolver getResolver() {
        return resolver;
    }

    private void addBinaries(final Document doc) throws RepositoryException {
        if (hippoTextValue != null) {
            addHippoTextValue(doc, hippoTextValue);
        } else if (binaryValues != null) {
            for (BinaryValue binaryValue : binaryValues) {
                super.addBinaryValue(doc, binaryValue.fieldName, binaryValue.internalValue);
            }
        }
    }

    private void addHippoTextValue(final Document doc, final BinaryValue hippoTextBinaryValue) throws RepositoryException {
        log.debug("The '{}' property is present and thus will be used to index this binary", HippoNodeType.HIPPO_TEXT);
        try {
            final String hippoText = IOUtils.toString(hippoTextBinaryValue.internalValue.getStream(), "UTF-8");
            // never store for binaries!
            doc.add(createFulltextField(hippoText, false, supportSimilarityOnBinaries, true));
        } catch (IOException e) {
            log.warn("Exception during indexing hippo:text binary property", e);
        }
    }

    private String getHippoTextPropertyName() {
        if (hippoTextPropertyName == null) {
            if (servicingIndexingConfig == null) {
                hippoTextPropertyName = HippoNodeType.HIPPO_TEXT;
            } else {
                try {
                    hippoTextPropertyName = resolver.getJCRName(servicingIndexingConfig.getHippoTextPropertyName());
                } catch (NamespaceException e) {
                    log.error("Error resolving hippo text property name", e);
                    hippoTextPropertyName = HippoNodeType.HIPPO_TEXT;
                }
            }
        }
        return hippoTextPropertyName;
    }

    private boolean isExcludedFromNodeScope(String fieldName) {
        return servicingIndexingConfig != null && servicingIndexingConfig.isExcludedFromNodeScope(fieldName, resolver);
    }

    private boolean isExcludedSingleIndexTerm(String fieldName) {
        return servicingIndexingConfig != null && servicingIndexingConfig.isExcludedSingleIndexTerm(fieldName, resolver);
    }

    private void indexNodeName(Document doc) throws RepositoryException {
        if (!isRootNode()) {
            try {
                NodeState parent = (NodeState) stateProvider.getItemState(node.getParentId());
                ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
                if (child == null) {
                    throw new RepositoryException("Missing child node entry for node with id: " + node.getNodeId());
                }

                String nodeName = child.getName().getLocalName();
                String prefix = queryHandlerContext.getNamespaceRegistry().getPrefix(child.getName().getNamespaceURI());
                if (prefix != null && !prefix.isEmpty()) {
                    nodeName = prefix + ":" + nodeName;
                }

                final String jcrName = resolver.getJCRName(NameConstants.JCR_NAME);

                // index the full node name for sorting
                final Field field = createFieldWithoutNorms(jcrName, nodeName, PropertyType.STRING);
                doc.add(field);

                indexFacetProperty(doc, jcrName);
                indexFacet(doc, jcrName, nodeName);

                // index the local name for full text search
                indexNodeLocalName(doc, child.getName().getLocalName());

            } catch (ItemStateException e) {
                throwRepositoryException(e);
            }
        }
    }

    private boolean isRootNode() {
        return node.getParentId() == null;
    }

    private void indexNodeLocalName(Document doc, final String localName) throws NamespaceException {
        String hippoNsPrefix = this.mappings.getPrefix(this.servicingIndexingConfig.getHippoNamespaceURI());
        String fieldName = hippoNsPrefix + ":" + FieldNames.FULLTEXT_PREFIX + "_localname";

        Field localNameField;
        Field localNameFullTextField;
        if (supportHighlighting) {
            localNameField = new Field(fieldName, localName, Field.Store.YES, Field.Index.ANALYZED,
                    Field.TermVector.WITH_OFFSETS);
            localNameFullTextField = new Field(FieldNames.FULLTEXT, localName, Field.Store.YES, Field.Index.ANALYZED,
                    Field.TermVector.WITH_OFFSETS);
        } else {
            localNameField = new Field(fieldName, localName, Field.Store.NO, Field.Index.ANALYZED,
                    Field.TermVector.NO);
            localNameFullTextField = new Field(FieldNames.FULLTEXT, localName, Field.Store.NO, Field.Index.ANALYZED,
                    Field.TermVector.NO);
        }
        localNameField.setBoost(5);
        doc.add(localNameField);
        doc.add(localNameFullTextField);
    }

    protected void indexFacetProperty(final Document doc, final String fieldName) {
        doc.add(new Field(ServicingFieldNames.FACET_PROPERTIES_SET, fieldName, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS,
                Field.TermVector.NO));
    }

    private void indexFacet(Document doc, String fieldName, String value, Field.TermVector termVector) {
        String internalFacetName = ServicingNameFormat.getInternalFacetName(fieldName);
        doc.add(new Field(internalFacetName, value, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS, termVector));
    }

    private void indexFacet(Document doc, String fieldName, String value) {
        indexFacet(doc, fieldName, value, Field.TermVector.NO);
    }

    private void indexDateFacet(Document doc, String fieldName, Calendar calendar) {
        Map<String, String> resolutions = new HashMap<String, String>();
        resolutions.put("year", DateTools.timeToString(calendar.getTimeInMillis(),
                DateTools.Resolution.YEAR));
        resolutions.put("month", DateTools.timeToString(calendar.getTimeInMillis(),
                DateTools.Resolution.MONTH));
        resolutions.put("week", DateTools.timeToString(calendar.getTimeInMillis(),
                DateTools.Resolution.WEEK));
        resolutions.put("day", DateTools.timeToString(calendar.getTimeInMillis(),
                DateTools.Resolution.DAY));
        resolutions.put("hour", DateTools.timeToString(calendar.getTimeInMillis(),
                DateTools.Resolution.HOUR));
        resolutions.put("minute", DateTools.timeToString(calendar.getTimeInMillis(),
                DateTools.Resolution.MINUTE));
        resolutions.put("second", DateTools.timeToString(calendar.getTimeInMillis(),
                DateTools.Resolution.SECOND));

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
        doc.add(new Field(internalFacetName, dateToString, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));

        for (Entry<String, String> keyValue : resolutions.entrySet()) {
            String compoundFieldName = fieldName + ServicingFieldNames.DATE_RESOLUTION_DELIMITER + keyValue.getKey();
            indexFacetProperty(doc, compoundFieldName);
            indexFacet(doc, compoundFieldName, keyValue.getValue());
        }

        for (Entry<String, Integer> keyValue : byDateNumbers.entrySet()) {
            String compoundFieldName = fieldName + ServicingFieldNames.DATE_NUMBER_DELIMITER + keyValue.getKey();
            indexFacetProperty(doc, compoundFieldName);
            indexFacet(doc, compoundFieldName, String.valueOf(keyValue.getValue()));
        }
    }

    private void indexLongFacet(Document doc, String fieldName, long value) {
        indexFacet(doc, fieldName, String.valueOf(value));

        // for efficient range queries on long fields, we also index a lexical format and store term vector
        String compoundFieldName = fieldName + ServicingFieldNames.LONG_POSTFIX;
        indexFacetProperty(doc, compoundFieldName);
        indexFacet(doc, compoundFieldName, LongField.longToString(value), Field.TermVector.YES);
    }

    private void indexStringFacet(Document doc, String fieldName, String value) {
        indexFacet(doc, fieldName, value);

        // lowercase index the the first, first 2 and first 3 chars in separate fields
        for (int i = 1; i <= 3; i++) {
            String ngram = fieldName + ServicingFieldNames.STRING_DELIMITER + i
                    + ServicingFieldNames.STRING_CHAR_POSTFIX;
            indexFacetProperty(doc, ngram);
            if (value.length() > i) {
                indexFacet(doc, ngram, value.substring(0, i).toLowerCase());
            } else {
                indexFacet(doc, ngram, value.toLowerCase());
            }
        }

    }

    private void indexDoubleFacet(Document doc, String fieldName, double value) {
        indexFacet(doc, fieldName, String.valueOf(value));

        // for efficient range queries on long fields, we also index a lexical format and store term vector
        String compoundFieldName = fieldName + ServicingFieldNames.DOUBLE_POSTFIX;
        indexFacetProperty(doc, compoundFieldName);
        indexFacet(doc, compoundFieldName, DoubleField.doubleToString(value), Field.TermVector.YES);
    }

    private void addHippoPath(Document doc, PropertyId id) throws RepositoryException {
        try {
            PropertyState propState = (PropertyState) stateProvider.getItemState(id);
            InternalValue[] values = propState.getValues();

            // index each level of the path for searching
            for (InternalValue value : values) {
                doc.add(new Field(ServicingFieldNames.HIPPO_PATH, value.getString(), Field.Store.NO,
                        Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
            }

            // make lexical sorting on depth possible. Max depth = 999;
            String depth = String.format("%03d", values.length);
            doc.add(new Field(ServicingFieldNames.HIPPO_DEPTH, depth, Field.Store.NO,
                    Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
        } catch (ItemStateException e) {
            throwRepositoryException(e);
        }
    }

    public void setSupportSimilarityOnStrings(final boolean supportSimilarityOnStrings) {
        this.supportSimilarityOnStrings = supportSimilarityOnStrings;
    }

    public void setSupportSimilarityOnBinaries(final boolean supportSimilarityOnBinaries) {
        this.supportSimilarityOnBinaries = supportSimilarityOnBinaries;
    }

    private class BinaryValue {

        private InternalValue internalValue;
        private String fieldName;

        private BinaryValue(String fieldName, InternalValue internalValue) {
            this.internalValue = internalValue;
            this.fieldName = fieldName;
        }

        private boolean isHippoTextValue() {
            return getHippoTextPropertyName().equals(fieldName);
        }
    }

}
