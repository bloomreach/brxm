/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang.time.DateFormatUtils;

/**
 * @version $Id$
 *
 */
@XmlRootElement(name = "value")
public class PropertyValue {
    private String value;
    
    public PropertyValue() {
    }
    
    public PropertyValue(Object valueObject) {
        if (valueObject != null) {
            if (valueObject instanceof Calendar) {
                this.value = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format((Calendar) valueObject);
            } 
            else {
                this.value = valueObject.toString();
            }
        }
    }
    
    public PropertyValue(Value valueObject) throws ValueFormatException, IllegalStateException, RepositoryException {
        int type = valueObject.getType();
        
        switch (type) {
        case PropertyType.BOOLEAN: 
            value = Boolean.toString(valueObject.getBoolean());
            break;
        case PropertyType.NAME:
        case PropertyType.REFERENCE:
        case PropertyType.STRING:
            value = valueObject.getString();
            break;
        case PropertyType.LONG :
            value = Long.toString(valueObject.getLong());
            break;
        case PropertyType.DOUBLE :
            value = Double.toString(valueObject.getDouble());
            break;
        case PropertyType.DATE :
            value = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(valueObject.getDate());
            break;
        }
    }
    
    @XmlValue
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
}
