/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.model.properties;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model of a multiple property with a Collection as object. Can be used for 
 * Wicket components that edit multiple property values directy, i.e. without a 
 * surrounding repeater, for instance a multiselect list.  
 * <p>
 * The contents of the list must be saved explicitly; i.e. modifying a returned list
 * is not sufficient, the client must also invoke {@link #setObject(List)}.
 */
public class JcrMultiPropertyValueModel<T extends Serializable> implements IModel<List<T>> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrMultiPropertyValueModel.class);

    private static final int NO_TYPE = -1;

    // dynamically reload value
    private transient boolean loaded = false;
    private transient List<T> object = null;
    private transient PropertyDefinition propertyDefinition;

    private final JcrPropertyModel<T> propertyModel;
    private int type = NO_TYPE;

    // Constructor
    public JcrMultiPropertyValueModel(JcrItemModel<Property> itemModel) {
        this(new JcrPropertyModel<T>(itemModel));
    }

    public JcrMultiPropertyValueModel(JcrPropertyModel<T> propertyModel) {
        this.propertyModel = propertyModel;
        Property property = propertyModel.getProperty();
        if (property != null) {
            try {
                type = property.getType();
            } catch (RepositoryException e) {
                log.error("Unable to get property type", e);
            }
        } else {
            PropertyDefinition def = getPropertyDefinition();
            // try to determine real value
            if (def != null) {
                type = def.getRequiredType();
            }
        }
        if (type == NO_TYPE) {
            type = PropertyType.UNDEFINED;
        }
    }

    public Property getProperty() {
        return propertyModel.getObject();
    }

    @SuppressWarnings("unchecked")
    public JcrItemModel<Property> getItemModel() {
        return propertyModel.getItemModel();
    }

    public List<T> getObject() {
        if (!loaded) {
            object = load();
            loaded = true;
        }
        return object;
    }

    public void detach() {
        loaded = false;
        object = null;
        propertyDefinition = null;
        propertyModel.detach();
    }

    public void setObject(final List<T> objects) {
        // make sure type is set
        getObject();

        if (objects == null) {
            setValues(new ArrayList<Value>(0));
        } else {
            List<Value> values = new ArrayList<Value>(objects.size());
            try {
                ValueFactory factory = ((UserSession) Session.get()).getJcrSession().getValueFactory();
                for (int i = 0; i < objects.size(); i++) {
                    switch (type) {
                    case PropertyType.BOOLEAN:
                        values.add(factory.createValue((Boolean) objects.get(i)));
                        break;
                    case PropertyType.DATE: {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime((Date) objects.get(i));
                        values.add(factory.createValue(calendar));
                        break;
                    }
                    case PropertyType.DOUBLE:
                        values.add(factory.createValue((Double) objects.get(i)));
                        break;
                    case PropertyType.LONG:
                        values.add(factory.createValue((Long) objects.get(i)));
                        break;
                    case PropertyType.UNDEFINED: {
                        Object object = objects.get(i);
                        if (object instanceof Boolean) {
                            values.add(factory.createValue((Boolean) object));
                        } else if (object instanceof Long) {
                            values.add(factory.createValue((Long) object));
                        } else if (object instanceof Double) {
                            values.add(factory.createValue((Double) object));
                        } else if (object instanceof Date) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime((Date) objects.get(i));
                            values.add(factory.createValue(calendar));
                        } else if (object instanceof BigDecimal) {
                            values.add(factory.createValue((BigDecimal) object));
                        } else if (object instanceof String) {
                            values.add(factory.createValue((String) object));
                        } else {
                            throw new RuntimeException("Could not determine value type of " + object);
                        }
                        break;
                    }
                    default:
                        // skip empty string as it cannot be an id in a list UI
                        if (!objects.get(i).toString().isEmpty()) {
                            values.add(factory.createValue(objects.get(i).toString(),
                                    (type == PropertyType.UNDEFINED ? PropertyType.STRING : type)));
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage(), ex);
                return;
            }
            setValues(values);
        }
    }

    @SuppressWarnings("unchecked")
    protected List<T> load() {
        try {
            List<Value> values = getValues();

            switch (type) {
            case PropertyType.BOOLEAN:
                List<Boolean> booleans = new ArrayList<Boolean>(values.size());
                for (int i = 0; i < values.size(); i++) {
                    booleans.add(values.get(i).getBoolean());
                }
                return (List<T>) booleans;
            case PropertyType.DATE:
                List<Date> dates = new ArrayList<Date>(values.size());
                for (int i = 0; i < values.size(); i++) {
                    dates.add(values.get(i).getDate().getTime());
                }
                return (List<T>) dates;
            case PropertyType.DOUBLE:
                List<Double> doubles = new ArrayList<Double>(values.size());
                for (int i = 0; i < values.size(); i++) {
                    doubles.add(values.get(i).getDouble());
                }
                return (List<T>) doubles;
            case PropertyType.LONG:
                List<Long> longs = new ArrayList<Long>(values.size());
                for (int i = 0; i < values.size(); i++) {
                    longs.add(values.get(i).getLong());
                }
                return (List<T>) longs;
            default:
                List<String> strings = new ArrayList<String>(values.size());
                for (int i = 0; i < values.size(); i++) {
                    // skip empty string as it cannot be an id in a list UI
                    if (!values.get(i).getString().equals("")) {
                        strings.add(values.get(i).getString());
                    }
                }
                return (List<T>) strings;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private PropertyDefinition getPropertyDefinition() {
        if (propertyDefinition == null) {
            // property doesn't exist, try to find pdef in the node definition
            propertyDefinition = propertyModel.getDefinition(type, true);
            if (propertyDefinition == null) {
                propertyDefinition = propertyModel.getDefinition(type, false);

                if (propertyDefinition == null && propertyModel.getItemModel().exists()) {
                    try {
                        propertyDefinition = propertyModel.getProperty().getDefinition();
                    } catch (RepositoryException e) {
                        throw new RuntimeException("Unable to determine property definition for " + propertyModel, e);
                    }
                }
            }
        }
        return propertyDefinition;
    }

    private void setValues(List<Value> values) {
        try {
            Value[] jcrValues = values.toArray(new Value[values.size()]);
            JcrItemModel<Property> itemModel = getItemModel();
            if (itemModel.exists()) {
                Property prop = getProperty();

                JcrPropertyModel propModel = new JcrPropertyModel(prop);
                PropertyDefinition definition = propModel.getDefinition(prop.getType(), true);
                if (definition == null) {
                    throw new IllegalStateException("no multi-valued definition found for property " + prop.getName());
                }

                // set new values
                if (!prop.isMultiple()) {
                    log.debug("Replacing property " + prop.getName() + " as it is not multi-valued");
                    Node node = prop.getParent();
                    String name = prop.getName();
                    prop.remove();
                    propertyModel.detach();
                    node.setProperty(name, jcrValues);
                } else {
                    prop.setValue(jcrValues);
                }
            } else {
                // create new property and set new values
                Node node = itemModel.getParentModel().getObject();
                String path = itemModel.getPath();
                String name = path.substring(path.lastIndexOf('/') + 1);
                node.setProperty(name, jcrValues);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    private List<Value> getValues() {
        if (propertyModel.getItemModel().exists()) {
            try {
                Property prop = getProperty();
                if (!prop.getDefinition().isMultiple()) {
                    return Arrays.asList(new Value[] { prop.getValue() });
                }

                return Arrays.asList(prop.getValues());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return Collections.emptyList();
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("property",
                propertyModel.getItemModel().getPath()).append("values", object).toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrMultiPropertyValueModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrMultiPropertyValueModel valueModel = (JcrMultiPropertyValueModel) object;
        return new EqualsBuilder().append(propertyModel, valueModel.propertyModel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(33, 114).append(propertyModel).toHashCode();
    }
}
