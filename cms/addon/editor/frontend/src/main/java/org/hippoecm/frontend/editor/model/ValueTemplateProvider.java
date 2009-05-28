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
package org.hippoecm.frontend.editor.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTemplateProvider extends AbstractProvider<JcrPropertyValueModel> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ValueTemplateProvider.class);

    private IFieldDescriptor descriptor;
    private ITypeDescriptor type;

    public ValueTemplateProvider(IFieldDescriptor descriptor, ITypeDescriptor type, JcrItemModel itemModel) {
        super(itemModel);
        this.descriptor = descriptor;
        this.type = type;
    }

    public IFieldDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void addNew() {
        load();

        try {
            int index;
            Value value = type.createValue();
            Node node = (Node) getItemModel().getParentModel().getObject();
            String path = getItemModel().getPath().substring(node.getPath().length() + 1);
            if (!node.hasProperty(path)) {
                if (descriptor.isMultiple()) {
                    node.setProperty(path, new Value[] { value });
                    index = 0;
                } else {
                    node.setProperty(path, value);
                    index = JcrPropertyValueModel.NO_INDEX;
                }
                getItemModel().detach();
            } else {
                if (!descriptor.isMultiple()) {
                    log.error("cannot add more than one value to single-valued property");
                    return;
                }

                Property property = node.getProperty(path);
                if (property.getDefinition().isMultiple()) {
                    Value[] oldValues = property.getValues();
                    Value[] newValues = new Value[oldValues.length + 1];
                    for (int i = 0; i < oldValues.length; i++) {
                        newValues[i] = oldValues[i];
                    }
                    newValues[oldValues.length] = value;
                    node.setProperty(path, newValues);
                    index = oldValues.length;
                } else {
                    Value old = property.getValue();
                    Value[] newValues = new Value[2];
                    newValues[0] = old;
                    newValues[1] = value;
                    property.setValue(newValues);
                    index = 1;
                }
            }
            elements.addLast(new JcrPropertyValueModel(index, value, new JcrPropertyModel(getItemModel())));
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public void remove(JcrPropertyValueModel model) {
        load();
        Iterator<JcrPropertyValueModel> iterator = elements.iterator();
        int newIndex = 0;
        while (iterator.hasNext()) {
            JcrPropertyValueModel currentModel = iterator.next();
            if (model.equals(currentModel)) {
                iterator.remove();

                try {
                    if (!getItemModel().exists()) {
                        log.warn("value not found");
                    } else {
                        Property property = (Property) getItemModel().getObject();
                        if (descriptor.isMultiple()) {
                            if (property.getDefinition().isMultiple()) {
                                Value[] oldValues = property.getValues();
                                int index = model.getIndex();
                                if (index >= 0 && index < oldValues.length) {
                                    Value[] newValues = new Value[oldValues.length - 1];
                                    int j = 0;
                                    for (int i = 0; i < oldValues.length; i++) {
                                        if (i == index)
                                            continue;
                                        newValues[j++] = oldValues[i];
                                    }
                                    property.setValue(newValues);
                                } else {
                                    log.warn("index outside of range");
                                }
                            } else {
                                property.remove();
                            }
                        } else {
                            property.remove();
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            } else {
                currentModel.setIndex(newIndex++);
            }
        }
    }

    @Override
    public void moveUp(JcrPropertyValueModel model) {
        load();
        int index = model.getIndex();
        if (descriptor.isMultiple() && index > 0) {
            try {
                Property property = (Property) getItemModel().getObject();
                Value[] oldValues = property.getValues();
                Value[] newValues = new Value[oldValues.length];
                System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);

                newValues[index] = oldValues[index - 1];
                newValues[index - 1] = oldValues[index];

                property.setValue(newValues);
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("Cannot move first value further up.");
        }
    }

    @Override
    protected void load() {
        if (elements != null) {
            return;
        }

        elements = new LinkedList<JcrPropertyValueModel>();
        try {
            Node node = (Node) getItemModel().getParentModel().getObject();
            String path = getItemModel().getPath().substring(node.getPath().length() + 1);
            if (getItemModel().exists()) {
                Property property = (Property) getItemModel().getObject();
                if (property.getDefinition().isMultiple()) {
                    Value[] values = property.getValues();
                    for (int index = 0; index < values.length; index++) {
                        if (descriptor.isMultiple()) {
                            addTemplate(property, values[index], index);
                        } else {
                            Value value = property.getValues()[0];
                            property.remove();
                            property = node.setProperty(path, value);
                            addTemplate(property, value, JcrPropertyValueModel.NO_INDEX);
                            break;
                        }
                    }
                } else {
                    if (descriptor.isMultiple()) {
                        Value value = property.getValue();
                        property.remove();
                        property = node.setProperty(path, new Value[] { value });
                        addTemplate(property, value, 0);
                    } else {
                        addTemplate(property, property.getValue(), JcrPropertyValueModel.NO_INDEX);
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private void addTemplate(Property property, Value value, int index) throws RepositoryException {
        String name = property.getName();
        Set<String> excluded = descriptor.getExcluded();
        if (excluded == null || !excluded.contains(name)) {
            elements.addLast(new JcrPropertyValueModel(index, value, new JcrPropertyModel(property)));
        }
    }
}
