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
package org.hippoecm.frontend.sa.template.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.template.FieldDescriptor;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeTemplateProvider extends AbstractProvider<JcrNodeModel> {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(NodeTemplateProvider.class);

    private FieldDescriptor descriptor;
    private TypeDescriptor type;

    public NodeTemplateProvider(FieldDescriptor descriptor, TypeDescriptor type, JcrItemModel itemModel) {
        super(itemModel);
        this.descriptor = descriptor;
        this.type = type;
    }

    public FieldDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void addNew() {
        load();

        try {
            Node parent = (Node) getItemModel().getObject();
            Node node = parent.addNode(descriptor.getPath(), type.getType());
            elements.addLast(new JcrNodeModel(node));
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public void remove(JcrNodeModel model) {
        load();
        Iterator<JcrNodeModel> iterator = elements.iterator();
        while (iterator.hasNext()) {
            JcrNodeModel currentModel = iterator.next();
            if (model.equals(currentModel)) {
                iterator.remove();
                try {
                    JcrItemModel itemModel = model.getItemModel();

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
                currentModel.detach();
            }
        }
    }

    @Override
    public void moveUp(JcrNodeModel model) {
        load();
        Iterator<JcrNodeModel> iterator = elements.iterator();
        JcrNodeModel predecessor = null;
        while (iterator.hasNext()) {
            JcrNodeModel currentModel = iterator.next();
            if (model.equals(currentModel)) {
                if (predecessor != null) {
                    try {
                        Node parent = (Node) getItemModel().getObject();
                        parent.orderBefore(model.getItemModel().getPath(), predecessor.getItemModel().getPath());
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                } else {
                    log.warn("No predecessor found for " + model);
                }
                elements = null;
                return;
            }
            if (predecessor != null) {
                predecessor.detach();
            }
            predecessor = currentModel;
        }
        if (predecessor != null) {
            predecessor.detach();
        }
        log.warn("could not find " + model);
    }

    @Override
    protected void load() {
        if (elements != null) {
            return;
        }

        elements = new LinkedList<JcrNodeModel>();
        try {
            Node node = (Node) getItemModel().getObject();
            if (type.isNode()) {
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
            elements.addLast(new JcrNodeModel(model));
        }
    }
}
