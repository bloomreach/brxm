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
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPropertyValueModel extends Model {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrPropertyValueModel.class);

    private JcrPropertyModel propertyModel;
    private String value;
    private int index;

    public JcrPropertyValueModel(int index, String value, JcrPropertyModel propertyModel) {
        this.propertyModel = propertyModel;
        this.index = index;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public Object getObject() {
        return value;
    }

    @Override
    public void setObject(Object object) {
        if (object != null) {
            value = object.toString();
            try {
                Property prop = propertyModel.getProperty();
                if (prop.getDefinition().isMultiple()) {
                    Value[] oldValues = prop.getValues();
                    String[] newValues = new String[oldValues.length];
                    for (int i = 0; i < oldValues.length; i++) {
                        if (i == index) {
                            newValues[i] = value;
                        } else {
                            newValues[i] = oldValues[i].getString();
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
        return new EqualsBuilder()
            .append(value, valueModel.value)
            .append(index, valueModel.index)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(33, 113)
            .append(value)
            .append(index)
            .toHashCode();
    }
}

