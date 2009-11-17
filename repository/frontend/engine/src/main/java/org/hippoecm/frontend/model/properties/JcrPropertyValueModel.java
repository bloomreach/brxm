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
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model of a property value.  Retrieves the value of a single valued property, or an indexed
 * value of a multi-valued property.  The type of the value returned depends on the JCR type
 * of the property.  I.e. JCR type string maps to {@link String}, date maps to {@link Data}.
 * <p>
 * One can also set and retrieve the underlying {@link Value}.
 * 
 * @author Frank van Lankvelt
 */
public class JcrPropertyValueModel<T extends Serializable> implements IModel<T> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrPropertyValueModel.class);

    public static final int NO_INDEX = -1;

    private static final int NO_TYPE = -1;

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
        this.propertyModel = propertyModel;
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
        }
        if (index != NO_INDEX) {
            setIndex(index);
        }
    }

    private PropertyDefinition getPropertyDefinition() {
        if (propertyDefinition == null) {
            if (propertyModel.getItemModel().exists()) {
                try {
                    propertyDefinition = propertyModel.getProperty().getDefinition();
                } catch (RepositoryException e) {
                    log.warn("Unable to determine property definition for {}", propertyModel, e);
                }
            } else {
                // property doesn't exist, try to find pdef in the node definition
                propertyDefinition = propertyModel.getDefinition(type, false);
                if (propertyDefinition == null) {
                    propertyDefinition = propertyModel.getDefinition(type, true);
                }
            }
        }
        return propertyDefinition;
    }

    public int getType() {
        if (type == NO_TYPE) {
            // try to determine real value
            if (value != null) {
                type = value.getType();
            } else {
                PropertyDefinition def = getPropertyDefinition();
                if (def != null) {
                    type = def.getRequiredType();
                } else {
                    type = PropertyType.UNDEFINED;
                }
            }
        }
        return type;
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
        this.value = value;
        this.type = getType();

        PropertyDefinition propDef = getPropertyDefinition();
        if (propDef != null) {
            try {
                if (propertyModel.getItemModel().exists()) {
                    Property prop = propertyModel.getProperty();
                    if (propDef.isMultiple()) {
                        Value[] oldValues = prop.getValues();
                        Value[] newValues = new Value[oldValues.length];
                        for (int i = 0; i < oldValues.length; i++) {
                            if (i == index) {
                                newValues[i] = (value == null ? createNullValue() : value);
                            } else {
                                newValues[i] = oldValues[i];
                            }
                        }
                        prop.setValue(newValues);
                    } else {
                        prop.setValue(value);
                    }
                } else if (value != null) {
                    Node node = (Node) propertyModel.getItemModel().getParentModel().getObject();
                    String name;
                    if (propDef.getName().equals("*")) {
                        String path = propertyModel.getItemModel().getPath();
                        name = path.substring(path.lastIndexOf('/') + 1);
                    } else {
                        name = propDef.getName();
                    }
                    if (propDef.isMultiple()) {
                        Value[] values = new Value[1];
                        values[0] = value;
                        node.setProperty(name, values);
                    } else {
                        node.setProperty(name, value);
                    }
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        } else {
            log.error("unable to set property, no definition found");
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
        if (object == null) {
            if (value != null) {
                setValue(null);
            }
            return;
        }
        try {
            ValueFactory factory = ((UserSession) Session.get()).getJcrSession().getValueFactory();
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
        ValueFactory factory = ((UserSession) Session.get()).getJcrSession().getValueFactory();
        int type = getType();
        return factory.createValue("", (type == PropertyType.UNDEFINED ? PropertyType.STRING : type));
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

}
