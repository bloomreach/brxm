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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Calendar;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name = "property")
public class PropertyContent extends ItemContent {
    
    static Logger logger = LoggerFactory.getLogger(PropertyContent.class);
    
    private int type;
    private String typeName;
    private String multiple;
    private Object [] value;
    
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
    
    public Object [] getValue() {
        return value;
    }
    
    public void setValue(Object [] value) {
        this.value = value;
    }
    
    private void loadPropertyValues(Property p) {
        try {
            boolean isMultiple = p.getDefinition().isMultiple();
            
            switch (p.getType()) {
            case PropertyType.BOOLEAN: 
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.value = new Boolean[values.length];
                    int i = 0;
                    for (Value val : values) {
                        this.value[i] = val.getBoolean();
                        i++;
                    }
                } else {
                    this.value = new Boolean [] { p.getBoolean() };
                }
                break;
            case PropertyType.STRING:
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.value = new String[values.length];
                    int i = 0;
                    for (Value val : values) {
                        this.value[i] = val.getString();
                        i++;
                    }
                } else {
                    this.value = new String [] { p.getString() };
                }
                break;
            case PropertyType.LONG :
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.value = new Long[values.length];
                    int i = 0;
                    for (Value val : values) {
                        this.value[i] = val.getLong();
                        i++;
                    }
                } else {
                    this.value = new Long [] { p.getLong() };
                }
                break;
            case PropertyType.DOUBLE :
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.value = new Double[values.length];
                    int i = 0;
                    for (Value val : values) {
                        this.value[i] = val.getDouble();
                        i++;
                    }
                } else {
                    this.value = new Double [] { p.getDouble() };
                }
                break;
            case PropertyType.DATE :
                if (isMultiple) {
                    Value [] values = p.getValues();
                    this.value = new Calendar[values.length];
                    int i = 0;
                    for(Value val : values) {
                        this.value[i] = val.getDate();
                        i++;
                    }
                } else {
                    this.value = new Calendar [] { p.getDate() };
                }
                break;
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve property value: {}", e.toString());
        }
        
        return ;
    }

    public void buildUrl(String urlBase, String siteContentPath, String encoding) throws UnsupportedEncodingException {
        if (encoding == null) {
            encoding = "ISO-8859-1";
        }
        
        String relativeContentPath = "";
        
        String path = getPath();
        
        if (path != null && path.startsWith(siteContentPath)) {
            relativeContentPath = path.substring(siteContentPath.length());
        }
        
        if (relativeContentPath != null) {
            StringBuilder relativeContentPathBuilder = new StringBuilder(relativeContentPath.length());
            String [] pathParts = StringUtils.splitPreserveAllTokens(StringUtils.removeStart(relativeContentPath, "/"), '/');
            
            for (int i = 0; i < pathParts.length - 1; i++) {
                String pathPart = pathParts[i];
                relativeContentPathBuilder.append('/').append(URLEncoder.encode(pathPart, encoding));
            }
            
            relativeContentPathBuilder.append('/').append(pathParts[pathParts.length - 1]);
            
            relativeContentPath = relativeContentPathBuilder.toString();
        }
        
        setUrl(URI.create(urlBase + "/property" + relativeContentPath));
    }

}
