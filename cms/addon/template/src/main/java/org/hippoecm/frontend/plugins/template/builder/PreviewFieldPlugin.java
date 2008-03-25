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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.ItemDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.RepositoryTemplateConfig;
import org.hippoecm.frontend.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewFieldPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PreviewFieldPlugin.class);

    private JcrNodeModel templateNodeModel;
    private Plugin child;

    public PreviewFieldPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(pluginModel), parentPlugin);

        child = createChild();
        add(child);
    }

    private Plugin createChild() {
        Plugin child;
        TemplateEngine engine = getPluginManager().getTemplateEngine();

        JcrSessionModel session = ((UserSession) Session.get()).getJcrSessionModel();
        RepositoryTemplateConfig templateConfig = new RepositoryTemplateConfig();
        try {
            ItemModel model = (ItemModel) getPluginModel();
            templateNodeModel = model.getNodeModel();
            Node templateNode = templateNodeModel.getNode();
            String typeName = templateNode.getName();

            TypeDescriptor type = engine.getTypeConfig().getTypeDescriptor(typeName);
            TemplateDescriptor template = templateConfig.createTemplate(templateNode, type);
            TemplateDescriptor proxy = new PreviewTemplateDescriptor(template);

            String folder;
            if (typeName.indexOf(':') > 0) {
                folder = typeName.substring(0, typeName.indexOf(':'));
            } else {
                folder = "system";
            }

            javax.jcr.Session jcrSession = (javax.jcr.Session) session.getObject();
            Node prototype = jcrSession.getRootNode().getNode(
                    HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.PROTOTYPES_PATH + "/" + folder + "/"
                            + typeName + "/" + typeName);
            JcrNodeModel prototypeModel = new JcrNodeModel(prototype);
            TemplateModel templateModel = new TemplateModel(proxy, prototypeModel.getParentModel(),
                    prototype.getName(), prototype.getIndex());

            child = engine.createTemplate("template", templateModel, this, null);

        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
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
                try {
                    Node typeNode = getTypeNode();
                    while (typeNode.isNew()) {
                        typeNode = typeNode.getParent();
                    }
                    typeNode.save();
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
                deleteItemNode(itemNode, getTypeNode());
                return true;
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return false;
    }

    private Node getTypeNode() throws RepositoryException {
        RepositoryTypeConfig typeConfig = new RepositoryTypeConfig();
        return typeConfig.getTypeNode(templateNodeModel.getNode().getName());
    }

    private void deleteItemNode(Node item, Node typeNode) throws RepositoryException {
        if (item.hasProperty(HippoNodeType.HIPPO_FIELD)) {
            String field = item.getProperty(HippoNodeType.HIPPO_FIELD).getString();
            NodeIterator fieldIter = typeNode.getNodes(HippoNodeType.HIPPO_FIELD);
            while (fieldIter.hasNext()) {
                Node fieldNode = fieldIter.nextNode();
                if (fieldNode.hasProperty(HippoNodeType.HIPPO_NAME)) {
                    String name = fieldNode.getProperty(HippoNodeType.HIPPO_NAME).getString();
                    if (name.equals(field)) {
                        fieldNode.remove();
                        break;
                    }
                }
            }
        } else {
            NodeIterator itemIter = item.getNodes(HippoNodeType.HIPPO_ITEM);
            while (itemIter.hasNext()) {
                deleteItemNode(itemIter.nextNode(), typeNode);
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
