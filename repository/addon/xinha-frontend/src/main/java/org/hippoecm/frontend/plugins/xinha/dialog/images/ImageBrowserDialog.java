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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.IPersistedMap;
import org.hippoecm.frontend.plugins.xinha.dialog.browse.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImage;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageBrowserDialog extends AbstractBrowserDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(ImageBrowserDialog.class);

    public ImageBrowserDialog(IPluginContext context, IPluginConfig config, final IModel model) {
        super(context, config, model);

        add(new TextFieldWidget("alt", new PropertyModel(model, XinhaImage.ALT)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                IPersistedMap link = (IPersistedMap) model.getObject();
                enableOk(link.isValid() && link.hasChanged());
            }
        });
    }
    
    protected JcrNodeModel findNewModel(IModel model) {
        JcrNodeModel nodeModel = (JcrNodeModel) model;
        HippoNode node = nodeModel.getNode();
        if (node != null) {
            try {
                if (node.getPrimaryNodeType().getName().equals("hippo:handle")) {
                    return new JcrNodeModel(node.getPath() + "/" + node.getName());
                } else {
                    return nodeModel;
                }
            } catch (RepositoryException e) {
                log.error("Error during hippo:handle check", e);
            }
        }
        return null;
    }

    @Override
    protected void onOk() {
        XinhaImage xi = (XinhaImage) getModelObject();
        xi.save();
    }

    @Override
    protected void onRemove() {
        XinhaImage img = (XinhaImage) getModelObject();
        img.delete();
    }

    @Override
    protected String getName() {
        return "imagepicker";
    }

}
