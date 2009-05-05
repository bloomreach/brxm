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
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPropertyValueModel extends Model {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrPropertyValueModel.class);
    static public int NO_INDEX = -1;

    // dynamically reload value
    private transient boolean loaded = false;
    private transient Value value;

    private JcrPropertyModel propertyModel;
    private int index;
    private int type;

    public JcrPropertyValueModel(JcrPropertyModel propertyModel) throws RepositoryException {
        this.propertyModel = propertyModel;
        if (propertyModel.getItemModel().exists()) {
            PropertyDefinition pdef = propertyModel.getProperty().getDefinition();
            this.type = pdef.getRequiredType();
            this.index = pdef.isMultiple() ? 0 : NO_INDEX;
        } else {
            PropertyDefinition pdef = propertyModel.getDefinition(PropertyType.UNDEFINED, false);
            if (pdef != null) {
                this.type = pdef.getRequiredType();
                this.index = NO_INDEX;
            } else {
                pdef = propertyModel.getDefinition(PropertyType.UNDEFINED, true);
                if (pdef != null) {
                    this.type = pdef.getRequiredType();
                    this.index = NO_INDEX;
                } else {
                    this.type = PropertyType.UNDEFINED;
                    this.index = NO_INDEX;
                    log.warn("No property definition found for {}", propertyModel);
                }
            }
        }
    }

    public JcrPropertyValueModel(int index, Value value, JcrPropertyModel propertyModel) {
        this.propertyModel = propertyModel;
        this.value = value;
        this.loaded = true;
        if (value != null) {
            type = value.getType();
        } else {
            type = PropertyType.UNDEFINED;
        }
        setIndex(index);
    }

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

        try {
            Property prop = propertyModel.getProperty();
            if (prop != null) {
                if (prop.getDefinition().isMultiple()) {
                    Value[] oldValues = prop.getValues();
                    Value[] newValues = new Value[oldValues.length];
                    for (int i = 0; i < oldValues.length; i++) {
                        if (i == index) {
                            newValues[i] = value;
                        } else {
                            newValues[i] = oldValues[i];
                        }
                    }
                    prop.setValue(newValues);
                } else {
                    prop.setValue(value);
                }
            } else {
                Node node = (Node) propertyModel.getItemModel().getParentModel().getObject();
                String name;
                PropertyDefinition pdef = propertyModel.getDefinition(value.getType(), index != NO_INDEX);
                if (pdef != null) {
                    if (pdef.getName().equals("*")) {
                        String path = propertyModel.getItemModel().getPath();
                        name = path.substring(path.lastIndexOf('/') + 1);
                    } else {
                        name = pdef.getName();
                    }
                    if (index != NO_INDEX) {
                        Value[] values = new Value[1];
                        values[0] = value;
                        node.setProperty(name, values);
                        this.index = 0;
                    } else {
                        node.setProperty(name, value);
                    }
                    this.type = pdef.getRequiredType();
                } else {
                    log.warn("No definition found for property");
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public Object getObject() {
        try {
            load();
            if (value != null) {
                switch (type) {
                case PropertyType.BOOLEAN:
                    return value.getBoolean();
                case PropertyType.DATE:
                    return value.getDate().getTime();
                case PropertyType.DOUBLE:
                    return value.getDouble();
                case PropertyType.LONG:
                    return value.getLong();
                default:
                    return value.getString();
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public void setObject(final Serializable object) {
        load();
        try {
            ValueFactory factory = ((UserSession) Session.get()).getJcrSession().getValueFactory();
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
                String string = object == null ? "" : object.toString();
                value = factory.createValue(string, (type == PropertyType.UNDEFINED ? PropertyType.STRING : type));
            }
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
            return;
        }

        setValue(value);
    }

    @Override
    public void detach() {
        value = null;
        loaded = false;
        propertyModel.detach();
        super.detach();
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("property",
                propertyModel.getItemModel().getPath()).append("index", index).append("value", value).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrPropertyValueModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrPropertyValueModel valueModel = (JcrPropertyValueModel) object;
        return new EqualsBuilder().append(propertyModel, valueModel.propertyModel).append(index, valueModel.index)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(33, 113).append(propertyModel).append(index).toHashCode();
    }

    public void setIndex(int index) {
        PropertyDefinition pdef = propertyModel.getDefinition(PropertyType.UNDEFINED, index != NO_INDEX);
        if (pdef != null && pdef.isMultiple()) {
            this.index = index;
        } else {
            this.index = NO_INDEX;
        }
    }

    private void load() {
        if (!loaded) {
            try {
                Property prop = propertyModel.getProperty();
                if (prop != null) {
                    if (index == NO_INDEX) {
                        value = prop.getValue();
                    } else {
                        Value[] values = prop.getValues();
                        if (index < values.length) {
                            value = values[index];
                        } else {
                            value = null;
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
                value = null;
            }
            loaded = true;
        }
    }
}
