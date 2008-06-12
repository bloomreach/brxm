/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugins.template.editor;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.plugin.channel.Request;
import org.hippoecm.frontend.model.JcrNodeModel;

public class EditorPlugin extends Plugin {

    private static final long serialVersionUID = 1L;

    private EditorForm form;

    public EditorPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        JcrNodeModel nodeModel = new JcrNodeModel(model);
        if (nodeModel.getItemModel().exists()) {
            add(form = newForm());
        } else {
            add(new Form("form"));
        }
        setOutputMarkupId(true);
    }

    protected EditorForm newForm() {
        JcrNodeModel jcrModel = new JcrNodeModel(getPluginModel());
        EditorForm form = new EditorForm("form", jcrModel, this);
        form.setMultiPart(true);
        form.setMaxSize(Bytes.megabytes(5));
        return form;
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel nodeModel = new JcrNodeModel(notification.getModel());
            if (!nodeModel.equals(new JcrNodeModel(getPluginModel()))) {
                if (form != null) {
                    form.destroy();
                    form = null;
                }
                setPluginModel(nodeModel);
                if (nodeModel.getItemModel().exists()) {
                    form = newForm();
                    replace(form);
                } else {
                    replace(new Form("form"));
                }
                notification.getContext().addRefresh(this);
            }
        } else if ("flush".equals(notification.getOperation())) {
            if (form != null) {
                form.destroy();
                form = null;
            }
            JcrNodeModel model = new JcrNodeModel(getPluginModel());
            if (model.getItemModel().exists()) {
                form = newForm();
                replace(form);
                notification.getContext().addRefresh(this);
            } else {
                replace(new Form("form"));
            }
        }
        super.receive(notification);
    }

    @Override
    public void handle(Request request) {
        if ("template.select".equals(request.getOperation())) {
            Channel bottom = getBottomChannel();
            bottom.publish(bottom.createNotification(request));
            return;
        }
        super.handle(request);
    }
}
