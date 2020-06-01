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
package org.onehippo.cms7.testsuite.cms.folder;

import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyIndexWorkflowPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static Logger log = LoggerFactory.getLogger(DummyIndexWorkflowPlugin.class);

    public DummyIndexWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow<FolderWorkflow>("dummyIndex", Model.of("Dummy Index ..."),
                (WorkflowDescriptorModel) getModel()) {

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.FILE);
            }

            @Override
            protected String execute(FolderWorkflow workflow) throws Exception {
                log.info("Dummy index context menu is executed...");
                return null;
            }
        });
    }
}
