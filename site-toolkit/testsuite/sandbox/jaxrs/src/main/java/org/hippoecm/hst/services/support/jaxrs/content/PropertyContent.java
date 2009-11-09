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
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public String getType() {
        return typeName;
    }
    
    public void setType(String typeName) {
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
    
    public Object [] getValues() {
        return values;
    }
    
    public void setValues(Object [] values) {
        this.values = values;
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
