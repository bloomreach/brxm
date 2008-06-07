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
package org.hippoecm.frontend.plugins.standardworkflow;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.legacy.dialog.DialogLink;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.CopyModelDialog;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.EditModelDialog;

@Deprecated
public class EditmodelWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public EditmodelWorkflowPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        add(new DialogLink("editModelRequest-dialog", new Model("Edit model"), EditModelDialog.class,
                getPluginModel(), getTopChannel(), getPluginManager().getChannelFactory()));
        add(new DialogLink("copyModelRequest-dialog", new Model("Copy model"), CopyModelDialog.class,
                getPluginModel(), getTopChannel(), getPluginManager().getChannelFactory()));
    }
}
