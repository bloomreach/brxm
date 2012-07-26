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
package org.hippoecm.frontend.model.map;

import java.util.AbstractList;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrValueList<T> extends AbstractList<T> implements IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrValueList.class);

    private JcrPropertyModel property;
    private int type;

    public JcrValueList(JcrPropertyModel model, int type) {
        this.property = model;
        this.type = type;
    }

    @Override
    public T set(int index, T element) {
        T previous = remove(index);
        add(index, element);
        return previous;
    }

    @Override
    public void add(int index, T element) {
        try {
            Value[] values;
            Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            if (property.getItemModel().exists()) {
                values = property.getProperty().getValues();
            } else {
                values = new Value[0];
            }
            Value[] newValues = new Value[values.length + 1];
            System.arraycopy(values, 0, newValues, 0, values.length);
            Value value;
            ValueFactory factory = session.getValueFactory();
            switch (type) {
            case PropertyType.BOOLEAN:
                value = factory.createValue((Boolean) element);
                break;
            case PropertyType.DATE:
                value = factory.createValue((Calendar) element);
                break;
            case PropertyType.DOUBLE:
                value = factory.createValue((Double) element);
                break;
            case PropertyType.LONG:
                value = factory.createValue((Long) element);
                break;
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.STRING:
                value = factory.createValue((String) element);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported type");
            }
            newValues[values.length] = value;
            JcrItemModel parentModel = property.getItemModel().getParentModel();
            Node node = (Node) parentModel.getObject();
            node.setProperty(property.getItemModel().getPath().substring(parentModel.getPath().length() + 1), newValues);
            if (!property.getItemModel().exists()) {
                property.detach();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public T remove(int index) {
        if (index >= size()) {
            throw new IllegalArgumentException("index " + index + " is too large");
        }
        try {
            T result = get(index);
            Value[] values = property.getProperty().getValues();
            Value[] newValues = new Value[values.length - 1];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index + 1, newValues, index, values.length - index - 1);
            property.getProperty().setValue(newValues);
            return result;
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        try {
            Value[] values = property.getProperty().getValues();
            if (index < values.length) {
                switch (type) {
                case PropertyType.BOOLEAN:
                    return (T) Boolean.valueOf(values[index].getBoolean());
                case PropertyType.DATE:
                    return (T) values[index].getDate();
                case PropertyType.DOUBLE:
                    return (T) new Double(values[index].getDouble());
                case PropertyType.LONG:
                    return (T) Long.valueOf(values[index].getLong());
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.STRING:
                    return (T) values[index].getString();
                default:
                    throw new UnsupportedOperationException("Unsupport multi-value type");
                }
            }
            throw new IllegalArgumentException("index too large");
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public int size() {
        if (!property.getItemModel().exists()) {
            return 0;
        }
        try {
            return property.getProperty().getValues().length;
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return 0;
    }

    public void detach() {
        property.detach();
    }

}
