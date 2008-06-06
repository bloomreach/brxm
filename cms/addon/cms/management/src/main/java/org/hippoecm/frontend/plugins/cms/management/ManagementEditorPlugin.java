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
package org.hippoecm.frontend.plugins.cms.management;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugins.template.editor.EditorForm;
import org.hippoecm.frontend.plugins.template.editor.EditorPlugin;

public class ManagementEditorPlugin extends EditorPlugin {
    private static final long serialVersionUID = 1L;

    private FeedbackPanel feedback;

    public ManagementEditorPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        add(feedback = new FeedbackPanel("feedback"));
        feedback.setOutputMarkupId(true);
    }

    @Override
    protected EditorForm newForm() {
        //JcrNodeModel jcrModel = new JcrNodeModel(getPluginModel());
        JcrNodeModel jcrModel = (JcrNodeModel) getModel();
        EditorForm form = new EditorForm("form", jcrModel, this);
        return form;
    }

    @Override
    public void handle(Request request) {
        if (request.getOperation().equals("feedback")) {
            request.getContext().addRefresh(feedback);
        } else {
            super.handle(request);
        }
    }
}
