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

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private ValueContent [] valueContents;
    
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
        
        try {
            if (property.getDefinition().isMultiple()) {
                Value [] valueObjects = property.getValues();
                this.valueContents = new ValueContent[valueObjects.length];
                for (int i = 0; i < valueObjects.length; i++) {
                    this.valueContents[i] = new ValueContent(valueObjects[i]);
                }
            } else {
                this.valueContents = new ValueContent [] { new ValueContent(property.getValue()) };
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve property value: {}", e.toString());
        }
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
    public ValueContent [] getValueContents() {
        return valueContents;
    }
    
    public void setValueContents(ValueContent [] valueContents) {
        this.valueContents = valueContents;
    }
    
    public ValueContent getFirstValueContent() {
        if (valueContents != null && valueContents.length > 0) {
            return valueContents[0];
        }
        
        return null;
    }
    
}
