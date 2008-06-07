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
package org.hippoecm.frontend.legacy.template.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.hippoecm.frontend.legacy.template.FieldDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateEngine;
import org.hippoecm.frontend.legacy.template.TypeDescriptor;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class NodeTemplateProvider extends AbstractProvider<TemplateModel> implements IDataProvider {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ItemProvider.class);

    private FieldDescriptor descriptor;
    private TemplateDescriptor template;

    public NodeTemplateProvider(FieldDescriptor descriptor, TemplateEngine engine, JcrNodeModel nodeModel, String mode) {
        super(nodeModel);
        this.descriptor = descriptor;
        TypeDescriptor type = engine.getTypeConfig().getTypeDescriptor(descriptor.getType());
        this.template = engine.getTemplateConfig().getTemplate(type, mode);
    }

    public FieldDescriptor getDescriptor() {
        return descriptor;
    }

    public void addNew() {
        load();

        try {
            Node parent = getNodeModel().getNode();
            Node node = parent.addNode(descriptor.getPath(), template.getTypeDescriptor().getType());
            TemplateModel templateModel = new TemplateModel(template, getNodeModel(), descriptor.getPath(), node
                    .getIndex());
            elements.addLast(templateModel);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void remove(TemplateModel model) {
        load();
        Iterator<TemplateModel> iterator = elements.iterator();
        int newIndex = 1;
        while (iterator.hasNext()) {
            TemplateModel currentModel = iterator.next();
            if (model.equals(currentModel)) {
                iterator.remove();
                JcrNodeModel nodeModel = model.getJcrNodeModel();
                try {
                    JcrItemModel itemModel = nodeModel.getItemModel();

                    if (itemModel.exists()) {
                        Item item = (Item) itemModel.getObject();

                        // remove the item
                        log.info("removing item " + item.getPath());
                        item.remove();
                    } else {
                        log.info("item " + itemModel.getPath() + " does not exist");
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
        load();
        Iterator<TemplateModel> iterator = elements.iterator();
        TemplateModel predecessor = null;
        while (iterator.hasNext()) {
            TemplateModel currentModel = iterator.next();
            if (model.equals(currentModel)) {
                if (predecessor != null) {
                    try {
                        Node parent = getNodeModel().getNode();
                        parent.orderBefore(model.getPath(), predecessor.getPath());
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                } else {
                    log.warn("No predecessor found for " + model);
                }
                elements = null;
                return;
            }
            predecessor = currentModel;
        }
        log.warn("could not find " + model);
    }

    @Override
    protected void load() {
        if (elements != null) {
            return;
        }

        elements = new LinkedList<TemplateModel>();
        try {
            Node node = getNodeModel().getNode();
            if (template.getTypeDescriptor().isNode()) {
                // expand the name-pattern
                NodeIterator iterator = node.getNodes(descriptor.getPath());
                while (iterator.hasNext()) {
                    Node child = iterator.nextNode();
                    // add child if it is not excluded.
                    // TODO: filter on node type
                    addTemplate(new JcrItemModel(child));
                }
            } else {
                PropertyIterator iterator = node.getProperties(descriptor.getPath());
                while (iterator.hasNext()) {
                    Property property = iterator.nextProperty();
                    addTemplate(new JcrItemModel(property));
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private void addTemplate(JcrItemModel model) throws RepositoryException {
        Item item = (Item) model.getObject();
        String name = item.getName();
        Set<String> excluded = descriptor.getExcluded();
        if (excluded == null || !excluded.contains(name)) {
            int index = ((Node) item).getIndex();
            elements.addLast(new TemplateModel(template, getNodeModel(), name, index));
        }
    }
}
