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
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.jackrabbit.FacetTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicingNodeIndexer extends NodeIndexer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(ServicingNodeIndexer.class);

    /**
     * Hardcoded nodescope indexing exluded properties. Currently hippo:paths
     */
    private Name excludeName;
    
    /**
     * The hippo namespace, derived from the prefix 'hippo', hence still valid after bumbing the hippo namespace to a next version
     */
    private String hippo_ns;
    
    /**
     * The indexing configuration or <code>null</code> if none is available.
     */
    protected ServicingIndexingConfiguration servicingIndexingConfig;

    private QueryHandlerContext queryHandlerContext;

    public ServicingNodeIndexer(NodeState node, QueryHandlerContext queryHandlerContext, NamespaceMappings mappings, TextExtractor extractor) {
        super(node, queryHandlerContext.getItemStateManager(), mappings, extractor);
        this.queryHandlerContext = queryHandlerContext;
        try {
            String name = HippoNodeType.HIPPO_PATHS.substring(HippoNodeType.HIPPO_PATHS.indexOf(":")+1);
            String prefix = HippoNodeType.HIPPO_PATHS.substring(0,HippoNodeType.HIPPO_PATHS.indexOf(":"));
            hippo_ns = this.queryHandlerContext.getNamespaceRegistry().getURI(prefix);
            excludeName = NameFactoryImpl.getInstance().create(hippo_ns,name);
        } catch (NamespaceException e1) {
            log.warn("Error creating exclude name for hippo:paths ", e1.getMessage());
        }
        
    }


    public void setServicingIndexingConfiguration(ServicingIndexingConfiguration config) {
        super.setIndexingConfiguration(config);
        this.servicingIndexingConfig = config;
    }


    @Override
    protected boolean isIncludedInNodeIndex(Name propertyName) {
        if(excludeName.equals(propertyName)){
            return false;
        }
        return super.isIncludedInNodeIndex(propertyName);
    }


    @Override
    protected Document createDoc() throws RepositoryException {
        // index the jackrabbit way
        Document doc = super.createDoc();
        // plus index our facet specifics
        
        // TODO : only index facets for hippo:document + subtypes
        try{
          
        if (node.getParentId() == null) {
            // root node
        } else {
            NodeState parent = (NodeState) stateProvider.getItemState(node.getParentId());
            ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
            if (child == null) {
                throw new RepositoryException("Missing child node entry " +
                        "for node with id: " + node.getNodeId());
            }
            String nodename = child.getName().getLocalName();
            String prefix = null;
            if(child.getName().getNamespaceURI() != null && !"".equals(child.getName().getNamespaceURI())) {
                prefix = queryHandlerContext.getNamespaceRegistry().getPrefix(child.getName().getNamespaceURI());
                nodename = prefix+":"+nodename;
            }
            // index the nodename to sort on
            doc.add(new Field(ServicingFieldNames.HIPPO_SORTABLE_NODENAME, nodename , Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
            
            /**
             * index the nodename to search on. We index this as hippo:_localname, a pseudo property which does not really exist but
             * only meant to search on
             */
            
            indexNodeName(doc, prefix ,child.getName().getLocalName());
            
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
                if(isHippoPath(propName)){
                    indexPath(doc,values,propState.getName());
                }
                if(isFacet(propName)){
                    for (int i = 0; i < values.length; i++) {
                        addValue(doc, values[i], propState.getName());
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

    
    private void indexNodeName(Document doc, String prefix, String localName) {
     // simple String
        String hippo_ns_prefix = null;
        try {
            hippo_ns_prefix = this.mappings.getPrefix(hippo_ns);
        } catch (NamespaceException e) {
           log.warn("Cannot get 'hippo' lucene prefix. ", e.getMessage());
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
    private void addValue(Document doc, InternalValue value, Name name) {
        String fieldName = name.getLocalName();
        try {
            fieldName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }
        switch (value.getType()) {
            case PropertyType.BINARY:
                // never facet;
                break;
            case PropertyType.BOOLEAN:
                indexFacet(doc,fieldName,value.toString()+FacetTypeConstants.BOOLEAN_POSTFIX);
                break;
            case PropertyType.DATE:
               // TODO : configurable resolution for dates: currently SECONDS
               String dateToString = DateTools.timeToString(value.getDate().getTimeInMillis(), DateTools.Resolution.SECOND);
               indexFacet(doc,fieldName,dateToString+FacetTypeConstants.DATE_POSTFIX);
               break;
            case PropertyType.DOUBLE:
                indexFacet(doc,fieldName,DoubleField.doubleToString(new Double(value.getDouble()).doubleValue())+FacetTypeConstants.DOUBLE_POSTFIX);
                break;
            case PropertyType.LONG:
                indexFacet(doc,fieldName,LongField.longToString(new Long(value.getLong()))+FacetTypeConstants.LONG_POSTFIX);
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
                    indexFacet(doc,fieldName,value.toString()+FacetTypeConstants.STRING_POSTFIX);
                }
                break;
            case PropertyType.NAME:
                if (name.equals(NameConstants.JCR_PRIMARYTYPE)){
                    indexNodeTypeNameFacet(doc,ServicingFieldNames.HIPPO_PRIMARYTYPE ,value.getQName());
                }
                else if(name.equals(NameConstants.JCR_MIXINTYPES)) {
                    indexNodeTypeNameFacet(doc, ServicingFieldNames.HIPPO_MIXINTYPE ,value.getQName());
                }
                break;
            default:
                throw new IllegalArgumentException("illegal internal value type");
        }
    }

    private void indexFacet(Document doc, String fieldName, String value) {
        doc.add(new Field(ServicingFieldNames.FACET_PROPERTIES_SET,fieldName,Field.Store.NO,Field.Index.NO_NORMS, Field.TermVector.NO));
        int idx = fieldName.indexOf(':');
        fieldName = fieldName.substring(0, idx + 1)
                + ServicingFieldNames.HIPPO_FACET + fieldName.substring(idx + 1);
        doc.add(new Field(fieldName,
                value,
                Field.Store.NO,
                Field.Index.NO_NORMS,
                Field.TermVector.YES));
    }

    protected void indexNodeTypeNameFacet(Document doc, String fieldName, Object internalValue) {
        try {
            Name qualiName = (Name) internalValue;
            String normValue = mappings.getPrefix(qualiName.getNamespaceURI())
                    + ":" + qualiName.getLocalName();
            doc.add(new Field(fieldName,normValue,Field.Store.NO,Field.Index.NO_NORMS, Field.TermVector.NO));
        } catch (NamespaceException e) {
            // will never happen
        }
    }

    private void indexPath(Document doc, InternalValue[] values, Name name) {

        // index each level of the path for searching
        for (int i = 0; i < values.length; i++) {
            InternalValue value = values[i];
            if(value.getType() == PropertyType.STRING){
                doc.add(new Field(ServicingFieldNames.HIPPO_PATH,
                        value.toString(),
                        Field.Store.NO,
                        Field.Index.NO_NORMS,
                        Field.TermVector.NO));
            }
        }

        // make lexical sorting on depth possible. Max depth = 999;
        String depth = String.valueOf(values.length);
        depth="000".substring(depth.length()).concat(depth);
        doc.add(new Field(ServicingFieldNames.HIPPO_DEPTH,depth,Field.Store.NO, Field.Index.NO_NORMS,Field.TermVector.NO));
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
        String msg = "Error while indexing node: " + node.getNodeId() + " of "
            + "type: " + node.getNodeTypeName();
        throw new RepositoryException(msg, e);
    }


}
