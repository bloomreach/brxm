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
package org.hippoecm.frontend.plugins.template.builder;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.frontend.template.ItemDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.JcrTypeModel;
import org.hippoecm.frontend.template.config.RepositoryTemplateConfig;
import org.hippoecm.frontend.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewFieldPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PreviewFieldPlugin.class);

    private JcrNodeModel templateNodeModel;
    private JcrNodeModel propertyModel;
    private JcrTypeModel typeModel;
    private Plugin child;

    public PreviewFieldPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(pluginModel), parentPlugin);

        updateModel();

        child = createChild();
        add(child);
    }

    private void updateModel() {
        try {
            ItemModel model = (ItemModel) getPluginModel();
            templateNodeModel = model.getNodeModel();
            Node templateTypeNode = templateNodeModel.getNode();
            while (!templateTypeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                templateTypeNode = templateTypeNode.getParent();
            }
            RepositoryTypeConfig typeConfig = new RepositoryTypeConfig(RemodelWorkflow.VERSION_DRAFT);
            typeModel = typeConfig.getTypeModel(templateTypeNode.getName());

            propertyModel = new JcrNodeModel(getPrototype());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private Plugin createChild() {
        Plugin child;
        TemplateEngine engine = getPluginManager().getTemplateEngine();

        RepositoryTemplateConfig templateConfig = new RepositoryTemplateConfig();
        RepositoryTypeConfig typeConfig = new RepositoryTypeConfig(RemodelWorkflow.VERSION_DRAFT);
        try {
            Node templateNode = templateNodeModel.getNode();
            String typeName = typeModel.getTypeName();

            TypeDescriptor type = typeConfig.getTypeDescriptor(typeName);
            TemplateDescriptor template = templateConfig.createTemplate(templateNode, type);
            TemplateDescriptor proxy = new PreviewTemplateDescriptor(template);

            Node prototype = propertyModel.getNode();
            JcrNodeModel prototypeModel = new JcrNodeModel(prototype);
            TemplateModel templateModel = new TemplateModel(proxy, prototypeModel.getParentModel(),
                    prototype.getName(), prototype.getIndex());

            child = engine.createTemplate("template", templateModel, this, null);

        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            ex.printStackTrace();
            PluginModel error = new PluginModel();
            error.put("error", ex.getMessage());
            PluginDescriptor plugin = new PluginDescriptor("template", ErrorPlugin.class.getName());
            child = new PluginFactory(getPluginManager()).createPlugin(plugin, error, this);
        }
        return child;
    }

    @Override
    public void receive(Notification notification) {
        ItemModel model = (ItemModel) getPluginModel();
        JcrNodeModel nodeModel = model.getNodeModel();
        if ("flush".equals(notification.getOperation())) {
            if (notification.getModel().equals(nodeModel)) {
                child.destroy();
                replace(child = createChild());
                notification.getContext().addRefresh(this);
                // only child has been replaced, so there is no
                // need to update.
                return;
            }
        } else if ("save".equals(notification.getOperation())) {
            if (notification.getModel().equals(nodeModel)) {
                if (typeModel != null) {
                    typeModel.save();
                }
                try {
                    propertyModel.getNode().save();
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        super.receive(notification);
    }

    @Override
    public void handle(Request request) {
        if ("up".equals(request.getOperation())) {
            String path = (String) request.getModel().getMapRepresentation().get("plugin");
            if (moveUp(path)) {
                // TODO: minimize the AJAX update
                child.destroy();
                replace(child = createChild());

                request.getContext().addRefresh(this);
            }
            return;
        } else if ("down".equals(request.getOperation())) {
            String path = (String) request.getModel().getMapRepresentation().get("plugin");
            if (moveDown(path)) {
                // TODO: minimize the AJAX update
                child.destroy();
                replace(child = createChild());

                request.getContext().addRefresh(this);
            }
            return;
        } else if ("focus".equals(request.getOperation())) {
            String path = (String) request.getModel().getMapRepresentation().get("plugin");
            JcrNodeModel itemNodeModel = new JcrNodeModel(resolvePath(path));

            Channel top = getTopChannel();
            Request select = top.createRequest("template.select", itemNodeModel);
            select.setContext(request.getContext());
            top.send(select);
            return;
        } else if ("remove".equals(request.getOperation())) {
            String path = (String) request.getModel().getMapRepresentation().get("plugin");
            if (removeItem(path)) {
                // TODO: minimize the AJAX update
                child.destroy();
                replace(child = createChild());

                request.getContext().addRefresh(this);
            }
            return;
        }
        super.handle(request);
    }

    @Override
    public void onDetach() {
        typeModel.detach();
        templateNodeModel.detach();
        super.onDetach();
    }

    protected boolean moveUp(String path) {
        Node itemNode = resolvePath(path);
        if (itemNode != null) {
            try {
                if (itemNode.getIndex() > 1) {
                    int idx = itemNode.getIndex();
                    itemNode.getParent().orderBefore("hippo:item[" + idx + "]", "hippo:item[" + (idx - 1) + "]");
                    return true;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return false;
    }

    protected boolean moveDown(String path) {
        Node itemNode = resolvePath(path);
        if (itemNode != null) {
            try {
                int idx = itemNode.getIndex();
                itemNode.getParent().orderBefore("hippo:item[" + (idx + 1) + "]", "hippo:item[" + idx + "]");
                return true;
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return false;
    }

    protected boolean removeItem(String path) {
        Node itemNode = resolvePath(path);
        if (itemNode != null) {
            try {
                deleteItemNode(itemNode);
                return true;
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return false;
    }

    private void deleteItemNode(Node item) throws RepositoryException {
        if (item.hasProperty(HippoNodeType.HIPPO_FIELD)) {
            String field = item.getProperty(HippoNodeType.HIPPO_FIELD).getString();
            typeModel.removeField(field);
        } else {
            NodeIterator itemIter = item.getNodes(HippoNodeType.HIPPO_ITEM);
            while (itemIter.hasNext()) {
                deleteItemNode(itemIter.nextNode());
            }
        }
        item.remove();
    }

    private Node resolvePath(String path) {
        try {
            if (path.startsWith(getPluginPath())) {
                String subPath = path.substring(getPluginPath().length() + 1);
                Node itemNode = templateNodeModel.getNode();
                while (subPath.indexOf(':') > 0) {
                    int idx = subPath.indexOf(':');
                    subPath = subPath.substring(idx + 1);

                    idx = subPath.indexOf(':');
                    Integer id;
                    if (idx > 0) {
                        id = Integer.parseInt(subPath.substring(0, idx));
                        subPath = subPath.substring(idx + 1);
                    } else {
                        id = Integer.parseInt(subPath);
                    }
                    itemNode = itemNode.getNode("hippo:item[" + (id + 1) + "]");
                }
                return itemNode;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private Node getPrototype() throws RepositoryException {
        Node templateTypeNode = templateNodeModel.getNode();
        while (!templateTypeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            templateTypeNode = templateTypeNode.getParent();
        }
        NodeIterator iter = templateTypeNode.getNode(HippoNodeType.HIPPO_PROTOTYPE).getNodes(
                HippoNodeType.HIPPO_PROTOTYPE);
        while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (node.isNodeType(HippoNodeType.NT_REMODEL)) {
                if (node.getProperty(HippoNodeType.HIPPO_REMODEL).getString().equals("draft")) {
                    return node;
                }
            }
        }
        throw new ItemNotFoundException("draft version of prototype was not found");
    }

    static class PreviewTemplateDescriptor extends TemplateDescriptor {
        private static final long serialVersionUID = 1L;

        TemplateDescriptor delegate;

        PreviewTemplateDescriptor(TemplateDescriptor delegate) {
            super(delegate.getMapRepresentation());

            this.delegate = delegate;
        }

        @Override
        public List<ItemDescriptor> getItems() {
            // during construction of TemplateDescriptor, delegate is not available
            if (delegate == null) {
                return new ArrayList<ItemDescriptor>(0);
            }

            List<ItemDescriptor> children = delegate.getItems();
            List<ItemDescriptor> filtered = new ArrayList<ItemDescriptor>(children.size());
            for (ItemDescriptor child : children) {
                ItemDescriptor filter = new PreviewItemPlugin.ItemFilter(child);
                filter.setTemplate(this);
                filtered.add(filter);
            }
            return filtered;
        }
    }
}
