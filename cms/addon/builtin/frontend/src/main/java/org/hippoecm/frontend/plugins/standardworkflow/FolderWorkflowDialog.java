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
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.widgets.NamespaceFriendlyChoiceRenderer;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FolderWorkflowDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String category;
    private String prototype;
    private String name;

    public FolderWorkflowDialog(AbstractFolderWorkflowPlugin folderWorkflowPlugin, IDialogService dialogWindow,
            String category) {
        super(folderWorkflowPlugin, dialogWindow, "Add " + category);
        this.category = category;

        TextFieldWidget text;
        add(text = new TextFieldWidget("name", new PropertyModel(this, "name")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                enableButtons();
            }
        });

        Set<String> prototypes = folderWorkflowPlugin.templates.get(category).getPrototypes();
        if (prototypes.size() > 1) {

            final List<String> prototypesList = new LinkedList<String>(prototypes);
            DropDownChoice folderChoice;
            add(folderChoice = new DropDownChoice("prototype", new PropertyModel(this, "prototype"), prototypesList,
                    new NamespaceFriendlyChoiceRenderer(prototypesList)) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean wantOnSelectionChangedNotifications() {
                    return true;
                }

                @Override
                protected void onSelectionChanged(Object newSelection) {
                    super.onSelectionChanged(newSelection);
                    enableButtons();
                }

            });
            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);

            // while not a prototype chosen, disable ok button
            Component notypes;
            add(notypes = new EmptyPanel("notypes"));
            notypes.setVisible(false);

        } else if (prototypes.size() == 1) {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = prototypes.iterator().next();
            Component notypes;
            add(notypes = new EmptyPanel("notypes"));
            notypes.setVisible(false);
        } else {
            // if the folderWorkflowPlugin.templates.get(category).size() = 0 you cannot add this
            // category currently. 
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = null;
            add(new Label("notypes", "There are no types available for : [" + category
                    + "] First create document types please."));
            text.setVisible(false);
        }

        enableButtons();
    }

    private void enableButtons() {
        AbstractFolderWorkflowPlugin folderWorkflowPlugin = (AbstractFolderWorkflowPlugin) getPlugin();
        WorkflowsModel model = (WorkflowsModel) folderWorkflowPlugin.getModel();
        ok.setEnabled(model.getNodeModel().getNode() != null && prototype != null);
    }

    @Override
    protected void execute() throws Exception {
        FolderWorkflow workflow = (FolderWorkflow) getWorkflow();
        if (prototype == null) {
            throw new IllegalArgumentException("You need to select a type");
        }
        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("You need to enter a name");
        }
        if (workflow != null) {
            AbstractFolderWorkflowPlugin folderWorkflowPlugin = (AbstractFolderWorkflowPlugin) getPlugin();
            if (!folderWorkflowPlugin.templates.get(category).getPrototypes().contains(prototype)) {
                log.error("unknown folder type " + prototype);
                throw new WorkflowException("Unknown folder type " + prototype);
            }
            String path = workflow.add(category, prototype, name);
            JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
            folderWorkflowPlugin.select(nodeModel);
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }

}
