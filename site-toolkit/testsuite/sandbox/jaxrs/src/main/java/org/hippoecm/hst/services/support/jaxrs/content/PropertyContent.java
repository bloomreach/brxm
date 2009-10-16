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
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name = "property")
public class PropertyContent extends ItemContent {
    
    static Logger logger = LoggerFactory.getLogger(PropertyContent.class);
    
    private int type;
    private String typeName;
    
    private boolean [] booleanValue;
    private String [] stringValue;
    private long [] longValue;
    private double [] doubleValue;
    private Calendar [] dateValue;
    
    public PropertyContent() {
        super();
    }
    
    public PropertyContent(Property property) throws RepositoryException {
        super(property);
        this.type = property.getType();
        this.typeName = PropertyType.nameFromValue(type);
        loadPropertyValues(property);
    }
    
    public String getType() {
        return typeName;
    }
    
    public void setType(String typeName) {
        this.type = PropertyType.valueFromName(typeName);
        this.typeName = typeName;
    }
    
    public boolean [] getBooleanValue() {
        return booleanValue;
    }
    
    public void setBooleanValue(boolean [] booleanValue) {
        this.booleanValue = booleanValue;
    }
    
    public String [] getStringValue() {
        return stringValue;
    }
    
    public void setStringValue(String [] stringValue) {
        this.stringValue = stringValue;
    }
    
    public long [] getLongValue() {
        return longValue;
    }
    
    public void setLongValue(long [] longValue) {
        this.longValue = longValue;
    }
    
    public double [] getDoubleValue() {
        return doubleValue;
    }
    
    public void setDoubleValue(double [] doubleValue) {
        this.doubleValue = doubleValue;
    }
    
    public Calendar [] getDateValue() {
        return dateValue;
    }
    
    public void setDateValue(Calendar [] dateValue) {
        this.dateValue = dateValue;
    }
    
    private void loadPropertyValues(Property p) {
        try {
            boolean multiple = p.getDefinition().isMultiple();
            
            switch (p.getType()) {
            case PropertyType.BOOLEAN: 
                if (multiple) {
                    Value [] values = p.getValues();
                    booleanValue = new boolean[values.length];
                    int i = 0;
                    for (Value val : values) {
                        booleanValue[i] = val.getBoolean();
                        i++;
                    }
                } else {
                    booleanValue = new boolean [] { p.getBoolean() };
                }
                break;
            case PropertyType.STRING:
                if (multiple) {
                    Value [] values = p.getValues();
                    stringValue = new String[values.length];
                    int i = 0;
                    for (Value val : values) {
                        stringValue[i] = val.getString();
                        i++;
                    }
                } else {
                    stringValue = new String [] { p.getString() };
                }
                break;
            case PropertyType.LONG :
                if (multiple) {
                    Value [] values = p.getValues();
                    longValue = new long[values.length];
                    int i = 0;
                    for (Value val : values) {
                        longValue[i] = val.getLong();
                        i++;
                    }
                } else {
                    longValue = new long [] { p.getLong() };
                }
                break;
            case PropertyType.DOUBLE :
                if (multiple) {
                    Value [] values = p.getValues();
                    doubleValue = new double[values.length];
                    int i = 0;
                    for (Value val : values) {
                        doubleValue[i] = val.getDouble();
                        i++;
                    }
                } else {
                    doubleValue = new double [] { p.getDouble() };
                }
                break;
            case PropertyType.DATE :
                if (multiple) {
                    Value [] values = p.getValues();
                    dateValue = new Calendar[values.length];
                    int i = 0;
                    for(Value val : values) {
                        dateValue[i] = val.getDate();
                        i++;
                    }
                } else {
                    dateValue = new Calendar [] { p.getDate() };
                }
                break;
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve property value: {}", e.toString());
        }
        
        return ;
    }

}
