/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.reviewedactions;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public class StdDocumentWorkflow extends StdWorkflow<DocumentWorkflow> {

    private final Icon icon;

    public StdDocumentWorkflow(final String id, final IModel<String> name, final IPluginContext pluginContext, final WorkflowDescriptorModel model, Icon icon) {
        super(id, name, pluginContext, model);
        this.icon = icon;
    }

    @Override
    public final String getSubMenu() {
        return "publication";
    }

    @Override
    protected final Component getIcon(final String id) {
        return HippoIcon.fromSprite(id, icon);
    }

}
