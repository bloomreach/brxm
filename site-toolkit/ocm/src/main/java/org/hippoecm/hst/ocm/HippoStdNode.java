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
package org.hippoecm.hst.ocm;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoStdNode implements SessionAware, NodeAware, SimpleObjectConverterAware {

    private static Logger log = LoggerFactory.getLogger(HippoStdNode.class);

    protected transient Session session;
    protected transient javax.jcr.Node node;
    protected transient SimpleObjectConverter simpleObjectConverter;
    protected String path;
    protected Map<String, Object> properties;

    public Session getSession() {
        return this.session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public javax.jcr.Node getNode() {
        return this.node;
    }

    public void setNode(javax.jcr.Node node) {
        this.node = node;
    }

    public SimpleObjectConverter getSimpleObjectConverter() {
        return this.simpleObjectConverter;
    }

    public void setSimpleObjectConverter(SimpleObjectConverter simpleObjectConverter) {
        this.simpleObjectConverter = simpleObjectConverter;
    }

    @Field(path = true)
    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        String name = "";

        if (this.node != null) {
            try {
                name = this.node.getName();
            } catch (Exception e) {
            }
        }

        return name;
    }

    public Map<String, Object> getProperties() {
        if (this.properties == null) {
            if (this.node == null) {
                this.properties = Collections.emptyMap();
            } else {
                this.properties = new HashMap<String, Object>();
                
                try {
                    Property property = null;
                    PropertyDefinition propertyDefinition = null;
                    String name = null;
                    Object value = null;
    
                    for (PropertyIterator it = this.node.getProperties(); it.hasNext();) {
                        property = it.nextProperty();
                        propertyDefinition = property.getDefinition();
                        name = property.getName();
                        value = getPropertyValue(property, propertyDefinition);
                        this.properties.put(name, value);
                    }
                } catch (RepositoryException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("getProperties() failed because of a repository exception: {}", e.getMessage());
                    }
                }
            }
        }
        
        return this.properties;
    }

    private Object getPropertyValue(Property property, PropertyDefinition propertyDefinition) {
        Object value = null;

        try {
            switch (property.getType()) {
            case PropertyType.BOOLEAN:
                if (propertyDefinition.isMultiple()) {
                    Value[] values = property.getValues();
                    Boolean[] bools = new Boolean[values.length];
                    int i = 0;
                    for (Value val : values) {
                        bools[i] = val.getBoolean();
                        i++;
                    }
                    value = bools;
                } else {
                    value = property.getBoolean();
                }
                break;
            case PropertyType.STRING:
                if (propertyDefinition.isMultiple()) {
                    Value[] values = property.getValues();
                    String[] strings = new String[values.length];
                    int i = 0;
                    for (Value val : values) {
                        strings[i] = val.getString();
                        i++;
                    }
                    value = strings;
                } else {
                    value = property.getString();
                }
                break;
            case PropertyType.LONG:
                if (propertyDefinition.isMultiple()) {
                    Value[] values = property.getValues();
                    Long[] longs = new Long[values.length];
                    int i = 0;
                    for (Value val : values) {
                        longs[i] = val.getLong();
                        i++;
                    }
                    value = longs;
                } else {
                    value = property.getLong();
                }
                break;
            case PropertyType.DOUBLE:
                if (propertyDefinition.isMultiple()) {
                    Value[] values = property.getValues();
                    Double[] doubles = new Double[values.length];
                    int i = 0;
                    for (Value val : values) {
                        doubles[i] = val.getDouble();
                        i++;
                    }
                    value = doubles;
                } else {
                    value = property.getDouble();
                }
                break;
            case PropertyType.DATE:
                if (propertyDefinition.isMultiple()) {
                    Value[] values = property.getValues();
                    Calendar[] dates = new Calendar[values.length];
                    int i = 0;
                    for (Value val : values) {
                        dates[i] = val.getDate();
                        i++;
                    }
                    value = dates;
                } else {
                    value = property.getDate();
                }
                break;
            }
        } catch (ValueFormatException e) {
            if (log.isWarnEnabled())
                log.warn("ValueFormatException: Exception for fetching property from '{}'", this.path);
        } catch (IllegalStateException e) {
            if (log.isWarnEnabled())
                log.warn("IllegalStateException: Exception for fetching property from '{}'", this.path);
        } catch (RepositoryException e) {
            if (log.isWarnEnabled())
                log.warn("RepositoryException: Exception for fetching property from '{}'", this.path);
        }

        return value;
    }
}
