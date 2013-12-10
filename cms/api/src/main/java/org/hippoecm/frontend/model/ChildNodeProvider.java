/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IDataProvider} that provides a list of models for child nodes, based
 * on a {@link IFieldDescriptor} and a {@link JcrNodeModel}.  A prototype is used to
 * add new child nodes.
 */
public class ChildNodeProvider extends AbstractProvider<Node, JcrNodeModel> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ChildNodeProvider.class);

    private IFieldDescriptor descriptor;
    private JcrNodeModel prototype;

    public ChildNodeProvider(IFieldDescriptor descriptor, JcrNodeModel prototype, JcrItemModel<Node> itemModel) {
        super(itemModel);
        this.descriptor = descriptor;
        this.prototype = prototype;
    }

    public IFieldDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void detach() {
        if (prototype != null) {
            prototype.detach();
        }
        if (descriptor instanceof IDetachable) {
            ((IDetachable) descriptor).detach();
        }
        super.detach();
    }

    @Override
    public void addNew() {
        load();

        try {
            Node parent = (Node) getItemModel().getObject();
            if (parent != null) {
                Node node;
                if (prototype != null) {
                    HippoSession session = (HippoSession) UserSession.get().getJcrSession();
                    node = session.copy(prototype.getNode(), parent.getPath() + "/" + descriptor.getPath());
                } else {
                    log.info("No prototype available to initialize child node for field {} with type {}", descriptor
                            .getName(), descriptor.getTypeDescriptor().getType());
                    node = parent.addNode(descriptor.getPath(), descriptor.getTypeDescriptor().getType());
                }
                elements.addLast(new JcrNodeModel(node));
            } else {
                log.warn("No parent available to initialize child node");
            }
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
                    log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
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
                        String srcPath = model.getNode().getName()
                                + (model.getNode().getIndex() > 1 ? "[" + model.getNode().getIndex() + "]" : "");
                        String destPath = predecessor.getNode().getName()
                                + (predecessor.getNode().getIndex() > 1 ? "[" + predecessor.getNode().getIndex() + "]"
                                        : "");
                        parent.orderBefore(srcPath, destPath);
                    } catch (RepositoryException ex) {
                        log.error("Unable to reorder children", ex);
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
    public ModelPathElement getFieldElement(JcrNodeModel model) {
        try {
            Node node = model.getNode();
            if (node != null) {
                return new ModelPathElement(descriptor, node.getName(), node.getIndex() - 1);
            } else {
                log.warn("Null node in provided model");
            }
        } catch (RepositoryException e) {
            log.error("Failed to build field element", e);
        }
        return null;
    }

    @Override
    protected void loadElements() {
        if (elements != null) {
            return;
        }

        elements = new LinkedList<JcrNodeModel>();
        try {
            Node node = (Node) getItemModel().getObject();
            if (node != null) {
                // expand the name-pattern
                NodeIterator iterator = node.getNodes(descriptor.getPath());
                while (iterator.hasNext()) {
                    Node child = iterator.nextNode();
                    // add child if it is not excluded.
                    // TODO: filter on node type
                    addTemplate(new JcrItemModel(child));
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
