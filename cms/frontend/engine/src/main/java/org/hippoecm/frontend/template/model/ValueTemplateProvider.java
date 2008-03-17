/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.template.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTemplateProvider extends AbstractProvider<TemplateModel> implements IDataProvider {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ValueTemplateProvider.class);

    private FieldDescriptor descriptor;
    private TemplateDescriptor template;
    private String path;

    public ValueTemplateProvider(FieldDescriptor descriptor, TemplateEngine engine, JcrNodeModel nodeModel, String path) {
        super(nodeModel);
        this.descriptor = descriptor;
        this.path = path;
        TypeDescriptor type = engine.getTypeConfig().getTypeDescriptor(descriptor.getType());
        this.template = engine.getTemplateConfig().getTemplate(type);
    }

    public FieldDescriptor getDescriptor() {
        return descriptor;
    }

    public void addNew() {
        load();

        try {
            int index;
            Node node = getNodeModel().getNode();
            Value value = template.getTypeDescriptor().createValue();
            if (!node.hasProperty(path)) {
                if (descriptor.isMultiple()) {
                    node.setProperty(path, new Value[] { value });
                    index = 0;
                } else {
                    node.setProperty(path, value);
                    index = JcrPropertyValueModel.NO_INDEX;
                }
            } else {
                if (!descriptor.isMultiple()) {
                    log.error("cannot add more than one value to single-valued property");
                    return;
                }

                Value[] oldValues = node.getProperty(path).getValues();
                Value[] newValues = new Value[oldValues.length + 1];
                for (int i = 0; i < oldValues.length; i++) {
                    newValues[i] = oldValues[i];
                }
                newValues[oldValues.length] = value;
                node.setProperty(path, newValues);
                index = oldValues.length;
            }

            elements.addLast(new TemplateModel(template, getNodeModel(), path, index));
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void remove(TemplateModel model) {
        load();
        Iterator<TemplateModel> iterator = elements.iterator();
        int newIndex = 0;
        while (iterator.hasNext()) {
            TemplateModel currentModel = iterator.next();
            if (model.equals(currentModel)) {
                iterator.remove();

                try {
                    Node node = getNodeModel().getNode();
                    if (!node.hasProperty(path)) {
                        log.warn("value not found");
                    } else {
                        if (descriptor.isMultiple()) {
                            Value[] oldValues = node.getProperty(path).getValues();
                            int index = model.getIndex();
                            if (index >= 0 && index < oldValues.length) {
                                Value[] newValues = new Value[oldValues.length - 1];
                                int j = 0;
                                for (int i = 0; i < oldValues.length; i++) {
                                    if (i == index)
                                        continue;
                                    newValues[j++] = oldValues[i];
                                }
                                node.getProperty(path).setValue(newValues);
                            } else {
                                log.warn("index outside of range");
                            }
                        } else {
                            Property property = node.getProperty(path);
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

    public void moveUp(TemplateModel model) {
        log.error("reordering values is not supported");
    }

    @Override
    protected void load() {
        if (elements != null) {
            return;
        }

        elements = new LinkedList<TemplateModel>();
        try {
            Node node = getNodeModel().getNode();
            if (node.hasProperty(path)) {
                Property property = node.getProperty(path);
                if (descriptor.isMultiple()) {
                    int size = property.getValues().length;
                    for (int index = 0; index < size; index++) {
                        addTemplate(new JcrItemModel(property), index);
                    }
                } else {
                    addTemplate(new JcrItemModel(property), JcrPropertyValueModel.NO_INDEX);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private void addTemplate(JcrItemModel model, int index) throws RepositoryException {
        Item item = (Item) model.getObject();
        String name = item.getName();
        Set<String> excluded = descriptor.getExcluded();
        if (excluded == null || !excluded.contains(name)) {
            elements.addLast(new TemplateModel(template, getNodeModel(), name, index));
        }
    }
}
