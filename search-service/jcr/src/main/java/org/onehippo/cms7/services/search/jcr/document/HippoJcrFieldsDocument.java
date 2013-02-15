/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.jcr.document;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.onehippo.cms7.services.search.content.ContentId;
import org.onehippo.cms7.services.search.document.FieldsDocument;
import org.onehippo.cms7.services.search.jcr.content.JcrContentId;

public class HippoJcrFieldsDocument implements FieldsDocument {

    private Node node;
    private Set<String> fieldNames;
    private ContentId contentId;

    public HippoJcrFieldsDocument() {
        this(null);
    }

    public HippoJcrFieldsDocument(Node node) {
        this(node, null);
    }

    public HippoJcrFieldsDocument(Node node, Set<String> fieldNames) {
        this.node = node;

        setFieldNames(fieldNames);

        try {
            contentId = new JcrContentId(node.getIdentifier());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    
    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public Collection<String> getFieldNames() {
        return Collections.unmodifiableCollection(fieldNames);
    }

    public void setFieldNames(Set<String> fieldNames) {
        this.fieldNames = new HashSet<String>();

        if (fieldNames != null) {
            this.fieldNames.addAll(fieldNames);
        }
    }

    @Override
    public String getPrimaryTypeName() {
        try {
            return node.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ContentId getContentId() {
        return contentId;
    }

    public void setContentId(ContentId contentId) {
        this.contentId = contentId;
    }

    @Override
    public boolean hasField(String name) {
        try {
            return fieldNames.contains(name) && node.hasProperty(name);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getFieldValue(String name) {
        if (!hasField(name)) {
            return null;
        }

        Object value = null;

        try {
            Property prop = node.getProperty(name);
            int type = prop.getType();
            boolean multiple = prop.isMultiple();

            if (multiple) {
                Value [] values = prop.getValues();
                int valuesLen = (values != null ? values.length : 0);

                switch (type) {
                case PropertyType.STRING:
                    String [] stringArray = new String[valuesLen];
                    for (int i = 0; i < valuesLen; i++) {
                        stringArray[i] = values[i].getString();
                    }
                    value = stringArray;
                    break;
                case PropertyType.BOOLEAN:
                    Boolean [] booleanArray = new Boolean[valuesLen];
                    for (int i = 0; i < valuesLen; i++) {
                        booleanArray[i] = (values[i].getBoolean() ? Boolean.TRUE : Boolean.FALSE);
                    }
                    value = booleanArray;
                    break;
                case PropertyType.DATE:
                    Calendar [] calendarArray = new Calendar[valuesLen];
                    for (int i = 0; i < valuesLen; i++) {
                        calendarArray[i] = values[i].getDate();
                    }
                    value = calendarArray;
                    break;
                case PropertyType.DOUBLE:
                    Double [] doubleArray = new Double[valuesLen];
                    for (int i = 0; i < valuesLen; i++) {
                        doubleArray[i] = new Double(values[i].getDouble());
                    }
                    value = doubleArray;
                    break;
                case PropertyType.LONG:
                    Long [] longArray = new Long[valuesLen];
                    for (int i = 0; i < valuesLen; i++) {
                        longArray[i] = new Long(values[i].getLong());
                    }
                    value = longArray;
                    break;
                case PropertyType.DECIMAL:
                    BigDecimal [] decimalArray = new BigDecimal[valuesLen];
                    for (int i = 0; i < valuesLen; i++) {
                        decimalArray[i] = values[i].getDecimal();
                    }
                    value = decimalArray;
                    break;
                }
            } else {
                switch (type) {
                case PropertyType.STRING:
                    value = prop.getString();
                    break;
                case PropertyType.BOOLEAN:
                    value = (prop.getBoolean() ? Boolean.TRUE : Boolean.FALSE);
                    break;
                case PropertyType.DATE:
                    value = prop.getDate();
                    break;
                case PropertyType.DOUBLE:
                    value = new Double(prop.getDouble());
                    break;
                case PropertyType.LONG:
                    value = new Long(prop.getLong());
                    break;
                case PropertyType.DECIMAL:
                    value = prop.getDecimal();
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return value;
    }

    @Override
    public Collection<Object> getFieldValues(String name) {
        Object value = getFieldValue(name);

        if (value == null) {
            return Collections.emptyList();
        }

        List<Object> values = new ArrayList<Object>();

        if (value.getClass().isArray()) {
            int length = ArrayUtils.getLength(value);

            for (int i = 0; i < length; i++) {
                values.add(Array.get(value, i));
            }
        } else {
            values.add(value);
        }

        return values;
    }

    @Override
    public Object getFirstFieldValue(String name) {
        Object value = getFieldValue(name);

        if (value == null) {
            return null;
        }

        if (value.getClass().isArray()) {
            if (ArrayUtils.getLength(value) > 0) {
                return Array.get(value, 0);
            } else {
                return null;
            }
        } else {
            return value;
        }
    }

}
