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
import org.hippoecm.frontend.template.ItemDescriptor;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WildcardFieldProvider extends AbstractProvider<WildcardModel> {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WildcardFieldProvider.class);

    private ItemDescriptor descriptor;
    private FieldDescriptor field;
    private TypeDescriptor type;
    private TypeConfig config;
    private int lastId;

    // Constructor

    public WildcardFieldProvider(ItemDescriptor item, TypeConfig config, JcrNodeModel nodeModel) {
        super(nodeModel);
        this.descriptor = item;
        this.config = config;
        this.field = item.getTemplate().getTypeDescriptor().getField(item.getField());
        this.type = config.getTypeDescriptor(this.field.getType());
        this.lastId = 0;
    }

    public void addNew() {
        load();

        elements.addLast(new WildcardModel(descriptor, config, getNodeModel(), null, ++lastId));
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
                        if (type.isNode()) {
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
                return;
            }
        }
        log.warn("could not find " + model);
    }

    private boolean isExcluded(String name) {
        Set<String> excluded = field.getExcluded();
        if (excluded == null || !excluded.contains(name)) {
            return false;
        }
        return true;
    }

    @Override
    protected void load() {
        if (elements != null) {
            return;
        }

        elements = new LinkedList<WildcardModel>();
        try {
            Node node = getNodeModel().getNode();
            if (type.isNode()) {
                // expand the name-pattern
                NodeIterator iterator = node.getNodes("*");
                while (iterator.hasNext()) {
                    Node child = iterator.nextNode();
                    if (!isExcluded(child.getName()) && child.isNodeType(type.getType())) {
                        addItem(new JcrItemModel(child));
                    }
                }
            } else {
                PropertyIterator iterator = node.getProperties("*");
                while (iterator.hasNext()) {
                    Property property = iterator.nextProperty();
                    if (!isExcluded(property.getName())) {
                        addItem(new JcrItemModel(property));
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private void addItem(JcrItemModel model) throws RepositoryException {
        Item item = (Item) model.getObject();
        String path = item.getName();
        Set<String> excluded = field.getExcluded();
        if (excluded == null || !excluded.contains(path)) {
            elements.addLast(new WildcardModel(descriptor, config, getNodeModel(), path, ++lastId));
        }
    }
}
