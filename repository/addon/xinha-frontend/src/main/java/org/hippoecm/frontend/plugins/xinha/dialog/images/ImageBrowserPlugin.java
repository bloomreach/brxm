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

package org.hippoecm.frontend.plugins.xinha.dialog.images;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.browse.BrowserPlugin;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageBrowserPlugin extends BrowserPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(ImageBrowserPlugin.class);

    private final XinhaImage image;
    private final JcrNodeModel initialModel;
    private final Form form;

    public ImageBrowserPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        image = (XinhaImage) getModelObject();
        if (image != null) {
            browseView.browse(image.getNodeModel());
        }
        initialModel = image.getNodeModel();

        add(form = new Form("form1"));
        form.add(new TextFieldWidget("alt", image.getPropertyModel(XinhaImage.ALT)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!ok.isEnabled()) {
                    enableOk(true);
                }
            }
        });
    }

    @Override
    protected void onDocumentChanged(IModel model) {
        JcrNodeModel newModel = (JcrNodeModel) model;
        HippoNode newNode = newModel.getNode();
        try {
            if (newNode.getPrimaryNodeType().getName().equals("hippo:handle")) {
                newModel = new JcrNodeModel(newNode.getPath() + "/" + newNode.getName());
            }
        } catch (RepositoryException e) {
            log.error("Error during hippo:handle check", e);
        }

        JcrNodeModel currentModel = image.getNodeModel();
        if (initialModel == null && currentModel == null) {
            image.setNodeModel(newModel);
            enableOk(true);
        } else if (!currentModel.equals(newModel)) {
            image.setNodeModel(newModel);
            if (!initialModel.equals(newModel)) {
                enableOk(true);
            } else {
                enableOk(false);
            }
        } else {
            enableOk(false);
        }
    }

    private void enableOk(boolean state) {
        AjaxRequestTarget.get().addComponent(ok.setEnabled(state));
    }
}
