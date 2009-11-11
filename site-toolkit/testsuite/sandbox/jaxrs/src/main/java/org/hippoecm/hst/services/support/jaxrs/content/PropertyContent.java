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
package org.hippoecm.hst.services.support.jaxrs.content;

import java.util.Calendar;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.jackrabbit.util.ISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * PropertyContent
 * 
 * @version $Id$
 */
@XmlRootElement(name = "property")
public class PropertyContent extends ItemContent {
    
    static Logger logger = LoggerFactory.getLogger(PropertyContent.class);
    
    private int type;
    private String typeName;
    private String multiple;
    private Object [] values;
    
    public PropertyContent() {
        super();
    }
    
    public PropertyContent(String name) {
        super(name);
    }
    
    public PropertyContent(String name, String path) {
        super(name, path);
    }
    
    public PropertyContent(Property property) throws RepositoryException {
        super(property);
        this.type = property.getType();
        this.typeName = PropertyType.nameFromValue(type);
        this.multiple = Boolean.toString(property.getDefinition().isMultiple());
        loadPropertyValues(property);
    }
    
    @XmlAttribute
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.typeName = PropertyType.nameFromValue(type);
        this.type = type;
    }
    
    @XmlAttribute
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(String typeName) {
        this.type = PropertyType.valueFromName(typeName);
        this.typeName = typeName;
    }
    
    @XmlAttribute
    public String getMultiple() {
        return multiple;
    }
    
    public void setMultiple(String multiple) {
        this.multiple = multiple;
    }
    
    @XmlElements(@XmlElement(name="value"))
    public Object [] getValues() {
        return values;
    }
    
    public void setValues(Object [] values) {
        this.values = values;
        
        if (this.values != null) {
            for (int i = 0; i < this.values.length; i++) {
                if (this.values[i] instanceof Element) {
                    String textContent = ((Element) this.values[i]).getTextContent();
                    
                    if (textContent != null) {
                        this.values[i] = convertPropertyValue(textContent, type);
                    }
                }
            }
        }
    }
    
    public Object getValue() {
        if (values != null && values.length > 0) {
            return values[0];
        }
        
        return null;
    }
    
    public String [] getValuesAsString() {
        if (values == null) {
            return null;
        }
        
        String [] stringValues = new String[values.length];
        
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Calendar) {
                stringValues[i] = ISO8601.format((Calendar) values[i]);
            } else {
                stringValues[i] = values[i].toString();
            }
        }
        
        return stringValues;
    }
    
    public String getValueAsString() {
        Object value = getValue();
        
        if (value == null) {
            return null;
        }
        
        if (value instanceof Calendar) {
            return ISO8601.format((Calendar) value);
        } else {
            return value.toString();
        }
    }
    
    private Object convertPropertyValue(String textContent, int type) {
        Object value = null;
        
        switch (type) {
        case PropertyType.BOOLEAN: 
            value = Boolean.valueOf(textContent);
            break;
        case PropertyType.NAME:
        case PropertyType.REFERENCE:
        case PropertyType.STRING:
            value = textContent;
            break;
        case PropertyType.LONG :
            value = Long.valueOf(textContent);
            break;
        case PropertyType.DOUBLE :
            value = Double.valueOf(textContent);
            break;
        case PropertyType.DATE :
            value = ISO8601.parse(textContent);
            break;
        }
        
        return value;
    }
    
    private void loadPropertyValues(Property p) {
        try {
            boolean isMultiple = p.getDefinition().isMultiple();
            
            switch (p.getType()) {
            case PropertyType.BOOLEAN: 
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.values = new Boolean[values.length];
                    int i = 0;
                    for (Value val : values) {
                        this.values[i] = val.getBoolean();
                        i++;
                    }
                } else {
                    this.values = new Boolean [] { p.getBoolean() };
                }
                break;
            case PropertyType.NAME:
            case PropertyType.REFERENCE:
            case PropertyType.STRING:
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.values = new String[values.length];
                    int i = 0;
                    for (Value val : values) {
                        this.values[i] = val.getString();
                        i++;
                    }
                } else {
                    this.values = new String [] { p.getString() };
                }
                break;
            case PropertyType.LONG :
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.values = new Long[values.length];
                    int i = 0;
                    for (Value val : values) {
                        this.values[i] = val.getLong();
                        i++;
                    }
                } else {
                    this.values = new Long [] { p.getLong() };
                }
                break;
            case PropertyType.DOUBLE :
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.values = new Double[values.length];
                    int i = 0;
                    for (Value val : values) {
                        this.values[i] = val.getDouble();
                        i++;
                    }
                } else {
                    this.values = new Double [] { p.getDouble() };
                }
                break;
            case PropertyType.DATE :
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.values = new Calendar[values.length];
                    int i = 0;
                    for(Value val : values) {
                        this.values[i] = val.getDate();
                        i++;
                    }
                } else {
                    this.values = new Calendar [] { p.getDate() };
                }
                break;
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve property value: {}", e.toString());
        }
        
        return ;
    }

}
