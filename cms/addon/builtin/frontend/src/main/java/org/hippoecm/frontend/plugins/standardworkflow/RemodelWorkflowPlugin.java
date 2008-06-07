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
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.CreateTypeDialog;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.RemodelDialog;

@Deprecated
public class RemodelWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public RemodelWorkflowPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        add(new DialogLink("remodelRequest-dialog", new Model("Update content"), RemodelDialog.class,
                getPluginModel(), getTopChannel(), getPluginManager().getChannelFactory()));

        add(new DialogLink("createTypeRequest-dialog", new Model("Create new type"), CreateTypeDialog.class,
                getPluginModel(), getTopChannel(), getPluginManager().getChannelFactory()));
    }
}
