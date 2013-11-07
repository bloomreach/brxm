/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IObjectClassAwareModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model of a property value.  Retrieves the value of a single valued property, or an indexed
 * value of a multi-valued property.  The type of the value returned depends on the JCR type
 * of the property.  I.e. JCR type string maps to {@link String}, date maps to {@link Date}.
 * <p>
 * One can also set and retrieve the underlying {@link Value}.
 */
public class JcrPropertyValueModel<T extends Serializable> implements IModel<T>, IObjectClassAwareModel<T> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrPropertyValueModel.class);

    public static final int NO_INDEX = -1;

    public static final int NO_TYPE = -1;

    // dynamically reload value
    private transient boolean loaded = false;
    private transient Value value = null;
    private transient PropertyDefinition propertyDefinition;

    private JcrPropertyModel propertyModel;
    private int index = NO_INDEX;
    private int type = NO_TYPE;

    /**
     * single-valued constructor.
     * 
     * @param propertyModel 
     */
    public JcrPropertyValueModel(JcrPropertyModel propertyModel) {
        this(NO_INDEX, null, propertyModel);
    }

    /**
     * Indexed value of a multi-valued property.
     * 
     * @param index
     * @param propertyModel
     */
    public JcrPropertyValueModel(int index, JcrPropertyModel propertyModel) {
        this(index, null, propertyModel);
    }

    /**
     * Multi-valued property constructor.
     * Can be used for single-valued properties by setting index to NO_INDEX
     * 
     * @param index
     * @param value
     * @param propertyModel 
     */
    public JcrPropertyValueModel(int index, Value value, JcrPropertyModel propertyModel) {
        this.propertyModel = propertyModel;
        this.value = value;
        if (value != null) {
            this.type = value.getType();
        } else {
            if (propertyModel.getItemModel().exists()) {
                Property property = propertyModel.getProperty();
                try {
                    this.type = property.getType();
                } catch (RepositoryException e) {
                    throw new RuntimeException("Could not determine type of property", e);
                }
            }
        }
        if (index != NO_INDEX) {
            this.index = index;
        }
    }

    /**
     * Returns the {@link javax.jcr.nodetype.PropertyDefinition} for the current property.
     * @return the property definition.
     */
    private PropertyDefinition getPropertyDefinition() {
        if (propertyDefinition == null) {
            // property doesn't exist, try to find pdef in the node definition
            propertyDefinition = propertyModel.getDefinition(type, index != NO_INDEX);
            if (propertyDefinition == null && propertyModel.getItemModel().exists()) {
                try {
                    propertyDefinition = propertyModel.getProperty().getDefinition();
                } catch (RepositoryException e) {
                    log.warn("Unable to determine property definition for " + propertyModel, e);
                }
            }
        }
        return propertyDefinition;
    }

    /**
     * Determines the type of the property. If the type is not yet defined, the type will be resolved based on the value.
     * If the value is <code>null</code> the type of property is determined based on the {@link javax.jcr.nodetype.PropertyDefinition}.
     * @see {@link javax.jcr.PropertyType} for the resulting values.
     * @return an integer representing the type of property.
     */
    public int getType() {
        if (type == NO_TYPE) {
            PropertyDefinition def = getPropertyDefinition();
            // try to determine real value
            if (def != null) {
                type = def.getRequiredType();
                if (type == PropertyType.UNDEFINED && value != null) {
                    type = value.getType();
                }
            } else if (value != null) {
                type = value.getType();
            } else {
                type = PropertyType.UNDEFINED;
            }
        }
        return type;
    }

    public void setType(int type) {
        if (this.type != NO_TYPE && this.type != type) {
            throw new IllegalStateException("Attempting to set the type after is was already determined");
        }
        this.type = type;
    }
    
    /**
     * The index of a value in a multi-valued property.  NO_INDEX (-1) is returned
     * for a single-valued property.
     */
    public int getIndex() {
        return index;
    }

    public JcrPropertyModel getJcrPropertymodel() {
        return propertyModel;
    }

    public Value getValue() {
        load();
        return value;
    }

    public void setValue(Value value) {
        load();

        try {
            if (this.value != null && value != null) {
                String oldValue = this.value.getString();
                String newValue = value.getString();
                if (this.value.getType() == value.getType() && oldValue.equals(newValue)) {
                    return;
                }
            }
            this.value = value;
            this.type = getType();

            PropertyDefinition propDef = getPropertyDefinition();
            if (propertyModel.getItemModel().exists()) {
                Property prop = propertyModel.getProperty();
                if (index != NO_INDEX) {
                    Value[] oldValues = prop.getValues();
                    Value[] newValues = new Value[oldValues.length];
                    for (int i = 0; i < oldValues.length; i++) {
                        if (i == index) {
                            newValues[i] = (value == null ? createNullValue() : value);
                        } else {
                            newValues[i] = oldValues[i];
                        }
                    }
                    if (prop.isMultiple()) {
                        prop.setValue(newValues);
                    } else {
                        String name = prop.getName();
                        Node node = prop.getParent();
                        prop.remove();
                        propertyModel.detach();
                        node.setProperty(name, newValues);
                    }
                } else {
                    if (value == null && propDef != null && propDef.isMandatory()) {
                        value = createNullValue();
                    }
                    if (!prop.isMultiple()) {
                        prop.setValue(value);
                    } else {
                        String name = prop.getName();
                        Node node = prop.getParent();
                        prop.remove();
                        propertyModel.detach();
                        node.setProperty(name, value);
                    }
                }
            } else if (value != null) {
                Node node = (Node) propertyModel.getItemModel().getParentModel().getObject();
                String name;
                if (propDef == null || propDef.getName().equals("*")) {
                    String path = propertyModel.getItemModel().getPath();
                    name = path.substring(path.lastIndexOf('/') + 1);
                } else {
                    name = propDef.getName();
                }
                if (index != NO_INDEX) {
                    Value[] values = new Value[1];
                    values[0] = value;
                    node.setProperty(name, values);
                } else {
                    node.setProperty(name, value);
                }
            }
        } catch (RepositoryException e) {
            log.error("An exception occured while trying to set value: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public T getObject() {
        try {
            load();
            if (value != null) {
                switch (value.getType()) {
                case PropertyType.BOOLEAN:
                    return (T) Boolean.valueOf(value.getBoolean());
                case PropertyType.DATE:
                    return (T) value.getDate().getTime();
                case PropertyType.DOUBLE:
                    return (T) Double.valueOf(value.getDouble());
                case PropertyType.LONG:
                    return (T) Long.valueOf(value.getLong());
                default:
                    return (T) value.getString();
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public void setObject(final Serializable object) {
        load();
        Value value = null;
        try {
            final ValueFactory factory = UserSession.get().getJcrSession().getValueFactory();
            if (object != null) {
                int type = getType();
                switch (type) {
                    case PropertyType.BOOLEAN:
                        value = factory.createValue((Boolean) object);
                        break;
                    case PropertyType.DATE:
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime((Date) object);
                        value = factory.createValue(calendar);
                        break;
                    case PropertyType.DOUBLE:
                        value = factory.createValue((Double) object);
                        break;
                    case PropertyType.LONG:
                        value = factory.createValue((Long) object);
                        break;
                    default:
                        String string = object.toString();
                        value = factory.createValue(string, (type == PropertyType.UNDEFINED ? PropertyType.STRING : type));
                }
            } else if (getType() == PropertyType.STRING) {
                value = factory.createValue("");
            }
        } catch (ValueFormatException ex) {
            log.info("invalid value " + object + ": " + ex.getMessage());
            return;
        } catch (UnsupportedRepositoryOperationException e) {
            log.error("repository is read-only", e);
            return;
        } catch (RepositoryException e) {
            log.error("repository error when setting value", e);
            return;
        }
        setValue(value);
    }

    public void detach() {
        loaded = false;
        value = null;
        propertyDefinition = null;
        propertyModel.detach();
    }

    public void setIndex(int index) {
        PropertyDefinition pdef = getPropertyDefinition();
        if (pdef == null) {
            return;
        }
        if (!pdef.isMultiple()) {
            throw new IllegalArgumentException("Setting the index on a single valued property");
        }
        if (index == NO_INDEX) {
            throw new IllegalArgumentException("Cannot set index to " + NO_INDEX + " for a multivalued property");
        }
        this.index = index;
    }

    private Value createNullValue() throws UnsupportedRepositoryOperationException, RepositoryException {
        ValueFactory factory = UserSession.get().getJcrSession().getValueFactory();
        int propertyType = getType();
        return factory.createValue("", (propertyType == PropertyType.UNDEFINED ? PropertyType.STRING : propertyType));
    }

    private void load() {
        if (!loaded) {
            if (propertyModel.getItemModel().exists()) {
                Property prop = propertyModel.getProperty();
                try {
                    if (prop.getDefinition().isMultiple()) {
                        Value[] values = prop.getValues();
                        if (values.length > 0 && index > -1 && index < values.length) {
                            value = values[index];
                        } else {
                            value = null;
                        }
                    } else {
                        value = prop.getValue();
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                    value = null;
                }
            }
            loaded = true;
        }
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("property",
                propertyModel.getItemModel().getPath()).append("index", index).append("value", value).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrPropertyValueModel<?> == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrPropertyValueModel<?> valueModel = (JcrPropertyValueModel<?>) object;
        return new EqualsBuilder().append(propertyModel, valueModel.propertyModel).append(index, valueModel.index)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(33, 113).append(propertyModel).append(index).toHashCode();
    }

    public Class getObjectClass() {
        int type = getType();
        switch (type) {
        case PropertyType.BOOLEAN:
            return Boolean.class;
        case PropertyType.DATE:
            return Date.class;
        case PropertyType.DOUBLE:
            return Double.class;
        case PropertyType.LONG:
            return Long.class;
        case PropertyType.UNDEFINED:
            return null;
        }
        return String.class;
    }

}
