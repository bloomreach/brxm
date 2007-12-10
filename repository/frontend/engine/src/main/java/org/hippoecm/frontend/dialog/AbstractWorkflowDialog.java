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
package org.hippoecm.frontend.dialog;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

/**
 * A dialog operating in a workflow context. Each workflow action should
 * extend this class and implement the doOk() method.
 *
 */
public abstract class AbstractWorkflowDialog extends AbstractDialog {

    public AbstractWorkflowDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
    }
    
    protected Workflow getWorkflow() {
        Plugin owningPlugin = getOwningPlugin();
        Workflow workflow = null;
        try {
            JcrNodeModel nodeModel = owningPlugin.getNodeModel();
            WorkflowManager manager = owningPlugin.getPluginManager().getWorkflowManager();

            //TODO: add optional property 'workflowcategory' to 
            //frontend plugin configuration nodes and use that instead of the plugin id.
            String workflowCategory = owningPlugin.getDescriptor().getPluginId();
            WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(workflowCategory, nodeModel.getNode());
            workflow = manager.getWorkflow(workflowDescriptor);

        } catch (MappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return workflow;
    }
    

    @Override
    protected PluginEvent ok() throws Exception {
        doOk();
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        nodeModel.getNode().getSession().save();
        nodeModel.getNode().getSession().refresh(true);

        //TODO: don't go all the way up to the root here
        //probably up to the nearest hippo:handle will do
        JcrNodeModel rootModel = nodeModel.findRootModel();
        PluginEvent result = new PluginEvent(getOwningPlugin(), JcrEvent.NEW_MODEL, nodeModel);
        result.chainEvent(JcrEvent.NEEDS_RELOAD, rootModel);
        return result;
    }
    
    /**
     * This abstract method is called from ok() and should implement
     * the action to be performed when the dialog's ok button is clicked. 
     */
    protected abstract void doOk() throws Exception;

}
