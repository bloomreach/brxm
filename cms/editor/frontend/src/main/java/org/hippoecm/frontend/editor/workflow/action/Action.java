/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.workflow.action;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.frontend.editor.workflow.NamespaceWorkflowPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Action extends WorkflowAction {

    static final Logger log = LoggerFactory.getLogger(Action.class);
    
    private static final long serialVersionUID = 1L;

    NamespaceWorkflowPlugin plugin;
    
    public Action(NamespaceWorkflowPlugin plugin, String id, String name,
            ResourceReference iconModel) {
        plugin.super(id, name, iconModel);
        this.plugin = plugin;
    }

    public Action(NamespaceWorkflowPlugin plugin, String id, StringResourceModel name) {
        plugin.super(id, name);
        this.plugin = plugin;
    }

    protected void openEditor(String type) {
        IEditorManager editorMgr = plugin.getEditorManager();
        if (editorMgr != null) {
            /* IEditor editor = */ try {
                editorMgr.openEditor(new JcrNodeModel("/hippo:namespaces/" + type.replace(':', '/')));
            } catch (ServiceException ex) {
                log.warn("Unable to open editor", ex);
            }
        }
    }
    
    @Override
    public void execute() {
        try {
            super.execute();
        } catch (Exception e) {
            error("Error while executing action: " + e.getMessage());
        }
    }

}
