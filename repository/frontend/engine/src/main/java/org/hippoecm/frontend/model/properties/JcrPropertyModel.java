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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ItemModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPropertyModel extends ItemModelWrapper implements IDataProvider {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ItemModelWrapper.class);

    //  Constructor
    public JcrPropertyModel(Property prop) {
        super(prop);
    }

    // The wrapped jcr property

    public Property getProperty() {
        return (Property) itemModel.getObject();
    }


    // IDataProvider implementation for use in DataViews
    // (lists and tables)

    public Iterator iterator(int first, int count) {
        List list = new ArrayList();
        try {
            Property prop = getProperty();
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    list.add(new IndexedValue(values[i].getString(), i));
                }
            } else {
                list.add(new IndexedValue(prop.getValue().getString(), 0));
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        IndexedValue indexedValue = (IndexedValue) object;
        return new JcrPropertyValueModel(indexedValue.index, indexedValue.value, this);
    }

    public int size() {
        int result = 1;
        try {
            Property prop = getProperty();
            if (prop.getDefinition().isMultiple()) {
                result = prop.getValues().length;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    private class IndexedValue {
        protected String value;
        protected int index;

        IndexedValue(String value, int index) {
            this.index = index;
            this.value = value;
        }
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("itemModel", itemModel.toString())
            .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrPropertyModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrPropertyModel propertyModel = (JcrPropertyModel) object;
        return new EqualsBuilder().append(itemModel, propertyModel.itemModel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(473, 17).append(itemModel).toHashCode();
    }

}
