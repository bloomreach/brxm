/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.query.lucene;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.lucene.DateField;
import org.apache.jackrabbit.core.query.lucene.DoubleField;
import org.apache.jackrabbit.core.query.lucene.LongField;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.NodeIndexer;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicingNodeIndexer extends NodeIndexer {

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(ServicingNodeIndexer.class);

    /**
     * The indexing configuration or <code>null</code> if none is available.
     */
    protected ServicingIndexingConfiguration servicingIndexingConfig;


    public ServicingNodeIndexer(NodeState node, ItemStateManager stateProvider, NamespaceMappings mappings, TextExtractor extractor) {
        super(node, stateProvider, mappings, extractor);
    }


    public void setServicingIndexingConfiguration(ServicingIndexingConfiguration config) {
        super.setIndexingConfiguration(config);
        this.servicingIndexingConfig = config;
    }


    @Override
    protected boolean isIncludedInNodeIndex(Name propertyName) {
        Name excludeName = NameFactoryImpl.getInstance().create("http://www.hippoecm.org/nt/1.0","paths");
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
                indexFacet(doc,fieldName,value.toString());
                break;
            case PropertyType.DATE:
               indexFacet(doc,fieldName,DateField.timeToString(value.getDate().getTimeInMillis()));
               break;
            case PropertyType.DOUBLE:
                indexFacet(doc,fieldName,DoubleField.doubleToString(new Double(value.getDouble()).doubleValue()));
                break;
            case PropertyType.LONG:
                indexFacet(doc,fieldName,LongField.longToString(new Long(value.getLong())));
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
                    indexFacet(doc,fieldName,value.toString());
                }
                break;
            case PropertyType.NAME:
                // never facet;
                break;
            default:
                throw new IllegalArgumentException("illegal internal value type");
        }
    }

    private void indexFacet(Document doc, String fieldName, String value) {
        doc.add(new Field(ServicingFieldNames.FACET_PROPERTIES_SET,fieldName,Field.Store.NO,Field.Index.NO_NORMS));
        int idx = fieldName.indexOf(':');
        fieldName = fieldName.substring(0, idx + 1)
                + ServicingFieldNames.HIPPO_FACET + fieldName.substring(idx + 1);
        doc.add(new Field(fieldName,
                value,
                Field.Store.NO,
                Field.Index.NO_NORMS,
                Field.TermVector.YES));
    }

    private void indexPath(Document doc, InternalValue[] values, Name name) {
        // index root node path
        
        doc.add(new Field(ServicingFieldNames.HIPPO_PATH,
                "/",
                Field.Store.NO,
                Field.Index.NO_NORMS,
                Field.TermVector.NO));
        
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
