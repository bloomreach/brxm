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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FolderWorkflowExtendedDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Set<String> prototypes;
    private String category;
    private String prototype;
    private String name;
    private String docbase;
    private String facet;
    private String value;
    private IModel title;

    public FolderWorkflowExtendedDialog(AbstractFolderWorkflowPlugin folderWorkflowPlugin, IModel title,
            String category, Set<String> prototypes) {
        super(folderWorkflowPlugin);
        this.title = title;
        this.category = category;
        this.prototypes = prototypes;

        add(new TextFieldWidget("name", new PropertyModel(this, "name")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                enableButtons();
            }
        });
        add(new TextFieldWidget("docbase", new PropertyModel(this, "docbase")));
        add(new TextFieldWidget("facet", new PropertyModel(this, "facet")));
        add(new TextFieldWidget("value", new PropertyModel(this, "value")));

        if (prototypes.size() > 1) {
            DropDownChoice folderChoice;
            add(folderChoice = new DropDownChoice("prototype", new PropertyModel(this, "prototype"),
                    new LinkedList<String>(prototypes)));
            folderChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onUpdate(AjaxRequestTarget target) {
                    enableButtons();
                }
            });

            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);
        } else if (prototypes.size() == 1) {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = prototypes.iterator().next();
        } else {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = null;
        }

        enableButtons();
    }

    public IModel getTitle() {
        return title;
    }

    private void enableButtons() {
        AbstractFolderWorkflowPlugin folderWorkflowPlugin = (AbstractFolderWorkflowPlugin) getPlugin();
        WorkflowsModel model = (WorkflowsModel) folderWorkflowPlugin.getModel();
        ok.setEnabled(model.getNodeModel().getNode() != null && prototype != null && name != null && !"".equals(name));
    }

    @Override
    protected void execute() throws Exception {
        FolderWorkflow workflow = (FolderWorkflow) getWorkflow();
        if (workflow != null) {
            AbstractFolderWorkflowPlugin folderWorkflowPlugin = (AbstractFolderWorkflowPlugin) getPlugin();
            if (!prototypes.contains(prototype)) {
                log.error("unknown folder type " + prototype);
                return;
            }
            Map<String, String> arguments = new TreeMap<String, String>();
            arguments.put("name", NodeNameCodec.encode(name, true));
            String path = (docbase.startsWith("/") ? docbase.substring(1) : docbase);
            arguments.put("docbase", ((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path)
                    .getUUID());
            arguments.put("facet", facet);
            arguments.put("value", value);
            path = workflow.add(category, prototype, arguments);

            ((UserSession) Session.get()).getJcrSession().refresh(true);

            JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
            folderWorkflowPlugin.select(nodeModel);
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }

}
