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

import java.util.ArrayList;
import java.util.List;

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
    private List<ValueContent> valueContents;
    
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
            this.valueContents = new ArrayList<ValueContent>();
            
            if (property.getDefinition().isMultiple()) {
                Value [] valueObjects = property.getValues();
                for (int i = 0; i < valueObjects.length; i++) {
                    this.valueContents.add(new ValueContent(valueObjects[i]));
                }
            } else {
                this.valueContents.add(new ValueContent(property.getValue()));
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve property value: {}", e.toString());
        }
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.typeName = PropertyType.nameFromValue(type);
        this.type = type;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(String typeName) {
        this.type = PropertyType.valueFromName(typeName);
        this.typeName = typeName;
    }
    
    public String getMultiple() {
        return multiple;
    }
    
    public void setMultiple(String multiple) {
        this.multiple = multiple;
    }
    
    @XmlElementWrapper(name="values")
    @XmlElements(@XmlElement(name="value"))
    public List<ValueContent> getValueContents() {
        return valueContents;
    }
    
    public void setValueContents(List<ValueContent> valueContents) {
        this.valueContents = valueContents;
    }
    
}
