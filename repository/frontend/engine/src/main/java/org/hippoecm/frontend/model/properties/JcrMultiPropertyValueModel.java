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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
 */
public class JcrMultiPropertyValueModel<T extends Serializable> implements IModel<List<T>> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrMultiPropertyValueModel.class);

    private static final int NO_TYPE = -1;

    // dynamically reload value
    private transient boolean loaded = false;
    private transient List<Value> values = null;

    private final JcrItemModel itemModel;
    private int type = NO_TYPE;

    // Constructor
    public JcrMultiPropertyValueModel(JcrItemModel itemModel) {
        this.itemModel = itemModel;
    }

    public Property getProperty() {
        return (Property) itemModel.getObject();
    }

    public int getType() {
        if (type == NO_TYPE) {
            if ((values != null) && (values.size() > 0)) {
                type = values.get(0).getType();
            }
            else {
                try {
                    PropertyDefinition def = getProperty().getDefinition();
                    if (def != null) {
                        type = def.getRequiredType();
                    }
                    else {
                        type = PropertyType.UNDEFINED;
                    }
                }
                catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                    type = PropertyType.UNDEFINED;
                }
            }
        }
        return type;
    }

    public JcrItemModel getItemModel() {
        return itemModel;
    }

    @SuppressWarnings("unchecked")
    public List<T> getObject() {
        try {
            load();

            if (values == null) {
                return null;
            }

            int type = getType();

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
        }
        catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public void setObject(final List<T> objects) {
        load();

        if (objects == null) {
            setValues(new ArrayList<Value>(0));
        }
        else {
            List<Value> values = new ArrayList<Value>(objects.size());
            try {
                ValueFactory factory = ((UserSession) Session.get()).getJcrSession().getValueFactory();
                int type = getType();
                for (int i = 0; i < objects.size(); i++) {
                    switch (type) {
                    case PropertyType.BOOLEAN:
                        values.add(factory.createValue((Boolean) objects.get(i)));
                        break;
                    case PropertyType.DATE:
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime((Date) objects.get(i));
                        values.add(factory.createValue(calendar));
                        break;
                    case PropertyType.DOUBLE:
                        values.add(factory.createValue((Double) objects.get(i)));
                        break;
                    case PropertyType.LONG:
                        values.add(factory.createValue((Long) objects.get(i)));
                        break;
                    default:
                        // skip empty string as it cannot be an id in a list UI
                        if (!objects.get(i).toString().equals("")) {
                            values.add(factory.createValue(objects.get(i).toString(),
                                (type == PropertyType.UNDEFINED ? PropertyType.STRING : type)));
                        }
                    }
                }
            }
            catch (RepositoryException ex) {
                log.error(ex.getMessage());
                return;
            }
            setValues(values);
        }
    }

    public void detach() {
        loaded = false;
        values = null;
        itemModel.detach();
    }

    private void setValues(List<Value> values) {
        this.values = values;
        
        try {
            Property prop = getProperty();
            if (prop.getDefinition() == null) {
                throw new IllegalStateException("property " + prop.getName()
                        + " has no definition");
            }
            if (!prop.getDefinition().isMultiple()) {
                throw new IllegalStateException("definition of property " + prop.getName()
                        + " is not multiple");
            }

            if (itemModel.exists()) {

                // set new values
                prop.setValue(values.toArray(new Value[]{}));
            } 
            else {
                // create new property and set new values
                Node node = (Node) itemModel.getParentModel().getObject();
                String name;
                if (prop.getDefinition().getName().equals("*")) {
                    String path = itemModel.getPath();
                    name = path.substring(path.lastIndexOf('/') + 1);
                } else {
                    name = prop.getDefinition().getName();
                }

                Value[] newValues = new Value[this.values.size()];
                for (int i = 0; i < this.values.size(); i++) {
                    newValues[i] = this.values.get(i);
                }
                node.setProperty(name, newValues);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }
    
    private void load() {
        if (!loaded) {
            if (itemModel.exists()) {
                try {
                    Property prop = getProperty();
                    if (prop.getDefinition() == null) {
                        throw new IllegalStateException("property " + prop.getName()
                                + " has no definition");
                    }
                    if (!prop.getDefinition().isMultiple()) {
                        throw new IllegalStateException("definition of property " + prop.getName()
                                + " is not multiple");
                    }

                    values = Arrays.asList(prop.getValues());
                }
                catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                    values = null;
                }
            }
            loaded = true;
        }
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("property",
                itemModel.getPath()).append("values", values).toString();
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
        return new EqualsBuilder().append(itemModel, valueModel.itemModel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(33, 114).append(itemModel).toHashCode();
    }
}