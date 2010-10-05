/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.jaxrs.model.content;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
@XmlRootElement(name = "property")
public class NodeProperty {

    static Logger logger = LoggerFactory.getLogger(NodeProperty.class);
    
	private String name;
	private int type;
	private boolean multiple;
	private PropertyValue[] values;
	
    public NodeProperty() {
    }
    
    public NodeProperty(String name) {
        this.name = name;
    }
    
    public NodeProperty(Property property) throws RepositoryException {
    	this.name = property.getName();
        this.type = property.getType();
        this.multiple = property.getDefinition().isMultiple();
        
        try {
            if (property.getDefinition().isMultiple()) {
                Value [] valueObjects = property.getValues();
                this.values = new PropertyValue[valueObjects.length];
                for (int i = 0; i < valueObjects.length; i++) {
                    this.values[i] = new PropertyValue(valueObjects[i]);
                }
            } 
            else {
                this.values = new PropertyValue [] { new PropertyValue(property.getValue()) };
            }
        } catch (Exception e) {
        	// TODO: exception?
            logger.warn("Failed to retrieve property value: {}", e.toString());
        }
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getTypeName() {
        return PropertyType.nameFromValue(type);
    }
    
    public void setTypeName(String typeName) {
        this.type = PropertyType.valueFromName(typeName);
    }
    
    public boolean getMultiple() {
        return multiple;
    }
    
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }
    
    @XmlElementWrapper(name="values")
    @XmlElements(@XmlElement(name="value"))
    public PropertyValue [] getValues() {
        return values;
    }
    
    public void setValues(PropertyValue [] values) {
        this.values = values;
    }
}
