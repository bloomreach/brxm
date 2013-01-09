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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.io.Serializable;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.reviewedactions.UnlockWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(UnlockWorkflowPlugin.class);

    private StdWorkflow unlockAction;

    public UnlockWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
        add(unlockAction = new StdWorkflow<UnlockWorkflow>("unlock", new StringResourceModel("unlock", this, null),
                                                           null, context, model) {

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "unlock-16.png");
            }

            @Override
            protected String execute(UnlockWorkflow workflow) throws Exception {
                workflow.unlock();
                return null;
            }
        });

        if (model != null) {
            try {
                Map<String, Serializable> hints = ((WorkflowDescriptor) model.getObject()).hints();
                if (hints.containsKey("unlock") && (hints.get("unlock") instanceof Boolean) && !(Boolean) hints.get(
                        "unlock")) {
                    unlockAction.setVisible(false);
                }
            } catch (RepositoryException ex) {
                // status unknown, maybe there are legit reasons for this, so don't emit a warning
                log.info(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }
}
