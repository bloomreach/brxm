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
package org.hippoecm.frontend.editor.workflow;

import java.io.Serializable;
import java.util.Map;

import javax.jcr.Node;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.editor.tools.CndSerializer;
import org.hippoecm.editor.tools.JcrPrototypeStore;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.editor.tools.NamespaceUpdater;
import org.hippoecm.editor.tools.TypeUpdate;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.workflow.dialog.RemodelDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(RemodelWorkflowPlugin.class);

    private static final long serialVersionUID = 1L;

    public RemodelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new WorkflowAction("create", new StringResourceModel("create-type", this, null)) {
            private static final long serialVersionUID = 1L;

            public String name;

            @Override
            protected Dialog createRequestDialog() {
                return new CreateTypeDialog(this);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                NamespaceWorkflow workflow = (NamespaceWorkflow) wf;
                workflow.addType(name);
                return null;
            }
        });

        add(new WorkflowAction("remodel", new StringResourceModel("update-model", this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Dialog createRequestDialog() {
                return new RemodelDialog(this, (WorkflowDescriptorModel) RemodelWorkflowPlugin.this.getModel());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                NamespaceWorkflow workflow = (NamespaceWorkflow) wf;
                try {
                    JcrSessionModel sessionModel = ((UserSession) Session.get()).getJcrSessionModel();

                    Map<String, Serializable> hints = workflow.hints();
                    String prefix = (String) hints.get("prefix");

                    CndSerializer serializer = new CndSerializer(getPluginContext(), sessionModel, prefix);
                    String cnd = serializer.getOutput();

                    JcrTypeStore typeStore = new JcrTypeStore(getPluginContext());
                    Map<String, TypeUpdate> update = typeStore.getUpdate(prefix);

                    JcrPrototypeStore prototypeStore = new JcrPrototypeStore();
                    for (Map.Entry<String, TypeUpdate> entry : update.entrySet()) {
                        String typeName = entry.getKey();
                        JcrNodeModel nodeModel = prototypeStore.getPrototype(prefix + ":" + typeName, true);
                        if (nodeModel != null && nodeModel.getNode() != null) {
                            Node prototypeNode = nodeModel.getNode();
                            if (prototypeNode.isNodeType("nt:unstructured")) {
                                entry.getValue().prototype = prototypeNode.getUUID();
                            }
                        }
                    }

                    Object cargo = NamespaceUpdater.toCargo(update);

                    sessionModel.getSession().save();

                    WorkflowDescriptor descriptor = (WorkflowDescriptor) RemodelWorkflowPlugin.this.getModelObject();
                    WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
                    javax.jcr.Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
                    session.save();
                    session.refresh(true);
                    workflow = (NamespaceWorkflow) manager.getWorkflow(descriptor);
                    if (workflow != null) {
                        log.info("remodelling namespace " + prefix);
                        try {
                            /* String[] nodes = */
                            workflow.updateModel(cnd, cargo);
                            sessionModel.getSession().save();
                        } finally {
                            // log out; the session model will log in again.
                            // Sessions cache path resolver information, which is incorrect after remapping the prefix.
                            sessionModel.flush();
                        }
                    } else {
                        log.warn("no remodeling workflow available on selected node");
                    }
                    return null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return ex.getClass().getName() + ": " + ex.getMessage();
                }
            }
        });
    }

    public class CreateTypeDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
        private static final long serialVersionUID = 1L;

        public CreateTypeDialog(CompatibilityWorkflowPlugin.WorkflowAction action) {
            action.super();
            add(setFocus(new TextFieldWidget("name", new PropertyModel(action, "name"))));
        }

        @Override
        public IModel getTitle() {
            return new StringResourceModel("create-type", RemodelWorkflowPlugin.this, null);
        }

        @Override
        public IValueMap getProperties() {
            return SMALL;
        }
    }

}
