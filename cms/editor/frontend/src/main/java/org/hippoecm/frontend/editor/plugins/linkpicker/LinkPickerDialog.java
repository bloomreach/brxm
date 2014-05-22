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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerController;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerDialog extends AbstractDialog<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LinkPickerDialog.class);

    private final IPluginContext context;
    private final IPluginConfig config;

    private final NodePickerController controller;

    public LinkPickerDialog(IPluginContext context, IPluginConfig config, IModel<String> model) {
        super(model);

        this.context = context;
        this.config = config;

        setOutputMarkupId(true);

        controller = new NodePickerController(context, NodePickerControllerSettings.fromPluginConfig(config)) {

            @Override
            protected IModel<Node> getInitialModel() {
                final String uuid = getModelObject();
                try {
                    if (StringUtils.isNotEmpty(uuid)) {
                        return new JcrNodeModel(UserSession.get().getJcrSession().getNodeByIdentifier(uuid));
                    }
                } catch (ItemNotFoundException e) {
                    // valid case, node does not exist
                    return null;
                } catch (RepositoryException e) {
                    log.error("Error while getting link picker model for the node with UUID '" + uuid + "'", e);
                }
                return null;
            }

            @Override
            protected void onSelect(boolean isValid) {
                setOkEnabled(isValid);
            }

        };

        add(controller.create("content"));
    }

    public IModel<String> getTitle() {
        return new StringResourceModel("link-picker", this, null);
    }

    @Override
    public final void onClose() {
        controller.onClose();
        super.onClose();
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (controller.getRenderer() != null) {
            controller.getRenderer().render(target);
        }
        super.render(target);
    }

    protected IModel<Node> getFolderModel() {
        return controller.getFolderModel();
    }

    @Override
    public void onOk() {
        if (controller.getSelectedModel() != null) {
            saveNode(controller.getSelectedModel().getObject());
        } else {
            error("No node selected");
        }
    }

    @Override
    protected void onDetach() {
        super.onDetach();

        controller.detach();
    }

    protected void saveNode(Node node) {
        try {
            getModel().setObject(node.getIdentifier());
        } catch (RepositoryException ex) {
            error(ex.getMessage());
        }
    }


    protected IPluginContext getPluginContext() {
        return context;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

}
