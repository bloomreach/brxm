/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.Map;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public class AdminWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    public AdminWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final StdWorkflow unlockAction;
        add(unlockAction = new StdWorkflow<DocumentWorkflow>("unlock", new StringResourceModel("unlock", this, null),
                null, context, getModel()) {

            @Override
            public String getSubMenu() {
                return "admin";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/unlock-16.png");
            }

            @Override
            protected String execute(DocumentWorkflow workflow) throws Exception {
                workflow.unlock();
                return null;
            }
        });

        Map<String, Serializable> info = getHints();
        hideIfNecessary(info, "unlock", unlockAction);
    }

}
