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
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WildcardFieldProvider extends AbstractProvider<WildcardModel> {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WildcardFieldProvider.class);

    private FieldDescriptor descriptor;

    // Constructor

    public WildcardFieldProvider(FieldDescriptor descriptor, JcrNodeModel nodeModel) {
        super(nodeModel);
        this.descriptor = descriptor;

        if (descriptor.getField() == null) {
            throw new IllegalArgumentException("Invalid descriptor");
        }
    }

    public void addNew() {
        load();

        FieldDescriptor subDescriptor = descriptor.getField().clone();
        subDescriptor.setPath(null);
        elements.addLast(new WildcardModel(subDescriptor, getNodeModel()));
    }

    public void remove(WildcardModel model) {
        load();
        Iterator<WildcardModel> iterator = elements.iterator();
        while (iterator.hasNext()) {
            if (model.equals(iterator.next())) {
                iterator.remove();
                if (model.getPath() != null) {
                    Node node = getNodeModel().getNode();
                    try {
                        if (descriptor.getField().isNode()) {
                            NodeIterator nodeIterator = node.getNodes(model.getPath());
                            while (nodeIterator.hasNext()) {
                                JcrItemModel itemModel = new JcrItemModel(nodeIterator.nextNode());

                                if (itemModel.exists()) {
                                    Item item = (Item) itemModel.getObject();

                                    // remove the item
                                    log.info("removing item " + item.getPath());
                                    item.remove();
                                } else {
                                    log.info("item " + itemModel.getPath() + " does not exist");
                                }
                            }
                        } else {
                            if (node.hasProperty(model.getPath())) {
                                Property property = node.getProperty(model.getPath());
                                property.remove();
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
                log.info("removed " + model);
                return;
            }
        }
        log.warn("could not find " + model);
    }

    @Override
    protected void load() {
        if (elements != null) {
            return;
        }

        elements = new LinkedList<WildcardModel>();
        try {
            Node node = getNodeModel().getNode();
            if (descriptor.isNode()) {
                // expand the name-pattern
                NodeIterator iterator = node.getNodes(descriptor.getPath());
                while (iterator.hasNext()) {
                    Node child = iterator.nextNode();
                    // add child if it is not excluded.
                    // TODO: filter on node type
                    addItem(new JcrItemModel(child));
                }
            } else {
                PropertyIterator iterator = node.getProperties(descriptor.getPath());
                while (iterator.hasNext()) {
                    Property property = iterator.nextProperty();
                    addItem(new JcrItemModel(property));
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private void addItem(JcrItemModel model) throws RepositoryException {
        Item item = (Item) model.getObject();
        String path = item.getName();
        Set<String> excluded = descriptor.getExcluded();
        if (excluded == null || !excluded.contains(path)) {
            FieldDescriptor subDescriptor = descriptor.getField().clone();
            subDescriptor.setPath(path);
            elements.addLast(new WildcardModel(subDescriptor, getNodeModel()));
        }
    }
}
