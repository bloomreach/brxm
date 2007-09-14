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
package org.hippoecm.repository.lucene;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

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
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

public class ServicingNodeIndexer extends NodeIndexer{

    /**
     * The indexing configuration or <code>null</code> if none is available.
     */
    protected ServicingIndexingConfiguration indexingConfig;
    
    
    public ServicingNodeIndexer(NodeState node, ItemStateManager stateProvider, NamespaceMappings mappings, TextExtractor extractor) {
        super(node, stateProvider, mappings, extractor);
    }
    
    
    public void setServicingIndexingConfiguration(ServicingIndexingConfiguration config) {
        this.indexingConfig = config;
    }
    

    @Override
    protected Document createDoc() throws RepositoryException {
        // index the jackrabbit way
        Document doc = super.createDoc();
        
        // plus index our facet specifics 
        Set props = node.getPropertyNames();
        for (Iterator it = props.iterator(); it.hasNext();) {
            QName propName = (QName) it.next();
            PropertyId id = new PropertyId(node.getNodeId(), propName);
            try {
                PropertyState propState = (PropertyState) stateProvider.getItemState(id);
                InternalValue[] values = propState.getValues();
                for (int i = 0; i < values.length; i++) {
                    addValue(doc, values[i], propState.getName());
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
    private void addValue(Document doc, InternalValue value, QName name) {
        String fieldName = name.getLocalName();
        try {
            fieldName = NameFormat.format(name, mappings);
        } catch (NoPrefixDeclaredException e) {
            // will never happen
        }
        switch (value.getType()) {
            case PropertyType.BINARY:
                // never facet;
                break;
            case PropertyType.BOOLEAN:
                if (isFacet(name)) {
                    indexFacet(doc,fieldName,value.toString());
                }
                break;
            case PropertyType.DATE:
                if (isFacet(name)) {
                    indexFacet(doc,fieldName,DateField.timeToString(((Calendar)((Object)value)).getTimeInMillis()));
                }
                break;
            case PropertyType.DOUBLE:
                if (isFacet(name)) {
                    indexFacet(doc,fieldName,DoubleField.doubleToString(new Double(value.getDouble()).doubleValue()));
                }
                break;
            case PropertyType.LONG:
                if (isFacet(name)) {
                    indexFacet(doc,fieldName,LongField.longToString(new Long(value.getLong())));
                }
                break;
            case PropertyType.REFERENCE:
                // never facet;
                break;
            case PropertyType.PATH:
                // never facet;
                break;
            case PropertyType.STRING:
                if (isFacet(name)) {
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
        doc.add(new Field(fieldName,
                value,
                Field.Store.YES,
                Field.Index.NO_NORMS,
                Field.TermVector.YES));
    }


    /**
     * Returns <code>true</code> if the property with the given name should be
     * indexed.
     *
     * @param propertyName name of a property.
     * @return <code>true</code> if the property should be fulltext indexed;
     *         <code>false</code> otherwise.
     */
    protected boolean isFacet(QName propertyName) {
        if (indexingConfig == null) {
            return false;
        } else {
            return indexingConfig.isFacet(node, propertyName);
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
