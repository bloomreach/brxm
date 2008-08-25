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
package org.hippoecm.frontend.plugin.workflow;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditService;

public class EmbedWorkflowPlugin extends WorkflowPlugin implements IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final String ITEM_ID = "workflow.item";

    private String item;

    public EmbedWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
// andere model ook listen
    }

    @Override
    protected IPluginConfig configureWorkflow(IPluginConfig wflConfig, WorkflowsModel model) {
        wflConfig = super.configureWorkflow(wflConfig, model);
        wflConfig.remove(IBrowseService.BROWSER_ID);
        wflConfig.remove(IEditService.EDITOR_ID);
        wflConfig.put(ITEM_ID, item);
        return wflConfig;
    }

    @Override
    public void onFlush(JcrNodeModel nodeModel) {
        JcrNodeModel myModel = getNodeModel();
        if (myModel != null && myModel.getItemModel().hasAncestor(nodeModel.getItemModel())) {
            updateModel(myModel);
        }
    }

}
