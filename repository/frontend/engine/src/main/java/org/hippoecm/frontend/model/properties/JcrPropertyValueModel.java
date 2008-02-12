/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model.properties;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.jackrabbit.value.NameValue;
import org.apache.jackrabbit.value.PathValue;
import org.apache.jackrabbit.value.ReferenceValue;
import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPropertyValueModel extends Model {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrPropertyValueModel.class);
    static public int NO_INDEX = -1;

    // dynamically reload value
    private transient boolean loaded = false;
    private transient Value value;

    private JcrPropertyModel propertyModel;
    private int index;
    private int type;

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
    
    @Override
    public Object getObject() {
        try {
            load();
            if (value != null) {
                return value.getString();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public void setObject(Object object) {
        if (object == null) {
            object = "";
        }

        load();
        try {
            String string = object.toString();
            switch (type) {
            case PropertyType.BOOLEAN:
                value = BooleanValue.valueOf(string);
                break;
            case PropertyType.DATE:
                value = DateValue.valueOf(string);
                break;
            case PropertyType.DOUBLE:
                value = DoubleValue.valueOf(string);
                break;
            case PropertyType.LONG:
                value = LongValue.valueOf(string);
                break;
            case PropertyType.NAME:
                value = NameValue.valueOf(string);
                break;
            case PropertyType.PATH:
                value = PathValue.valueOf(string);
                break;
            case PropertyType.REFERENCE:
                value = ReferenceValue.valueOf(string);
                break;
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
                value = new StringValue(string);
                break;
            default:
                log.info("Unable to parse property type " + PropertyType.nameFromValue(type));
                return;
            }
        } catch (ValueFormatException ex) {
            log.info(ex.getMessage());
            return;
        }

        try {
            Property prop = propertyModel.getProperty();
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
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
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
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("index", index)
            .append("value", value)
            .toString();
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
        return new EqualsBuilder().append(value, valueModel.value).append(index, valueModel.index).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(33, 113).append(value).append(index).toHashCode();
    }

    private void setIndex(int index) {
        try {
            if (propertyModel.getProperty().getDefinition().isMultiple()) {
                this.index = index;
            } else {
                this.index = NO_INDEX;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            this.index = index;
        }
    }

    private void load() {
        if (!loaded) {
            try {
                Property prop = propertyModel.getProperty();
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
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
                value = null;
            }
            loaded = true;
        }
    }
}
