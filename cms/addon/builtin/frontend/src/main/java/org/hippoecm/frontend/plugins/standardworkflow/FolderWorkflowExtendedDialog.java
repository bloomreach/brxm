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
 *  under the License.
 */
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

@Deprecated
public class FolderWorkflowExtendedDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;
    private String category;
    private String prototype;
    private String name;
    private String docbase;
    private String facet;
    private String value;
    private FolderWorkflowPlugin folderWorkflowPlugin;

    public FolderWorkflowExtendedDialog(FolderWorkflowPlugin folderWorkflowPlugin, IDialogService dialogWindow, String category) {
        super(folderWorkflowPlugin, dialogWindow, "Add " + category);
        this.category = category;
        this.folderWorkflowPlugin = folderWorkflowPlugin;

        WorkflowsModel model = (WorkflowsModel)folderWorkflowPlugin.getModel();
        if (model.getNodeModel().getNode() == null) {
            ok.setEnabled(false);
        } else {
            ok.setEnabled(true);
        }

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
        add(new TextFieldWidget("docbase", new PropertyModel(this, "docbase")));
        add(new TextFieldWidget("facet", new PropertyModel(this, "facet")));
        add(new TextFieldWidget("value", new PropertyModel(this, "value")));

        if(folderWorkflowPlugin.templates.get(category).size() > 1) {
            DropDownChoice folderChoice;
            add(folderChoice = new DropDownChoice("prototype", new PropertyModel(this, "prototype"), new LinkedList(folderWorkflowPlugin.templates.get(category))) {
                    private static final long serialVersionUID = 1L;
                    
                    @Override
                        protected boolean wantOnSelectionChangedNotifications() {
                        return true;
                    }
                });
            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);
        } else if(folderWorkflowPlugin.templates.get(category).size() == 1) {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = (String) folderWorkflowPlugin.templates.get(category).iterator().next();
        } else {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = null;
        }
    }

    @Override
    protected void execute() throws Exception {
        FolderWorkflow workflow = (FolderWorkflow)getWorkflow();
        if (workflow != null) {
            if (!folderWorkflowPlugin.templates.get(category).contains(prototype)) {
                log.error("unknown folder type " + prototype);
                return;
            }
            Map arguments = new TreeMap<String,String>();
            arguments.put("name", name);
            String path = (docbase.startsWith("/") ? docbase.substring(1) : docbase);
            arguments.put("docbase", ((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path).getUUID());
            arguments.put("facet", facet);
            arguments.put("value", value);
            path = workflow.add(category, prototype, arguments);
            JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
            FolderWorkflowPlugin plugin = (FolderWorkflowPlugin)getPlugin();
            plugin.select(nodeModel);
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }

    @Override
    public void cancel() {
    }
}
