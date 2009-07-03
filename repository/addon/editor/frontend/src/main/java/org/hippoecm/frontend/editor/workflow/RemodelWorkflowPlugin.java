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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
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
import org.hippoecm.frontend.editor.impl.JcrTemplateStore;
import org.hippoecm.frontend.editor.workflow.dialog.RemodelDialog;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.ITypeDescriptor;
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

        add(new WorkflowAction("new-document-type", new StringResourceModel("new-document-type", this, null)) {
            private static final long serialVersionUID = 1L;

            public String name;
            public List<String> mixins = new LinkedList<String>();

            @Override
            protected Dialog createRequestDialog() {
                return new CreateDocumentTypeDialog(this);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                NamespaceValidator.checkName(name);

                NamespaceWorkflow workflow = (NamespaceWorkflow) wf;
                try {
                    workflow.addType("document", name);
                } catch (ItemExistsException ex) {
                    return "Type " + name + " already exists";
                }

                String prefix = (String) workflow.hints().get("prefix");
                JcrTypeStore typeStore = new JcrTypeStore();

                ITypeDescriptor typeDescriptor = typeStore.getTypeDescriptor(prefix + ":" + name);
                List<String> types = typeDescriptor.getSuperTypes();
                for (String mixin : mixins) {
                    types.add(mixin);
                }
                typeDescriptor.setSuperTypes(types);

                typeStore.save(typeDescriptor);
                return null;
            }
        });

        add(new WorkflowAction("new-compound-type", new StringResourceModel("new-compound-type", this, null)) {
            private static final long serialVersionUID = 1L;

            public String name;

            @Override
            protected Dialog createRequestDialog() {
                return new CreateCompoundTypeDialog(this);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                NamespaceValidator.checkName(name);

                NamespaceWorkflow workflow = (NamespaceWorkflow) wf;
                workflow.addType("compound", name);
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

                    CndSerializer serializer = new CndSerializer(sessionModel, prefix);
                    String cnd = serializer.getOutput();

                    JcrTypeStore typeStore = new JcrTypeStore();
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

    public abstract class CreateTypeDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
        private static final long serialVersionUID = 1L;

        private String title;

        public CreateTypeDialog(CompatibilityWorkflowPlugin.WorkflowAction action, String title) {
            action.super();
            add(setFocus(new TextFieldWidget("name", new PropertyModel(action, "name"))));
            this.title = title;
        }

        @Override
        public IModel getTitle() {
            return new StringResourceModel(title, RemodelWorkflowPlugin.this, null);
        }

        @Override
        public IValueMap getProperties() {
            return SMALL;
        }
    }

    public class CreateDocumentTypeDialog extends CreateTypeDialog {
        private static final long serialVersionUID = 1L;

        public CreateDocumentTypeDialog(WorkflowAction action) {
            super(action, "new-document-type");

            CheckGroup cg = new CheckGroup("checkgroup", new PropertyModel(action, "mixins"));
            add(cg);

            JcrTemplateStore templateStore = new JcrTemplateStore(new JcrTypeStore());
            
            cg.add(new DataView("mixins", new ListDataProvider(templateStore.getAvailableMixins())) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item item) {
                    String mixin = item.getModelObjectAsString();
                    item.add(new Check("check", item.getModel()));
                    IModel typeName = new TypeTranslator(new JcrNodeTypeModel(mixin)).getTypeName();
                    item.add(new Label("mixin", typeName));
                }

            });
        }
    }

    public class CreateCompoundTypeDialog extends CreateTypeDialog {
        private static final long serialVersionUID = 1L;

        public CreateCompoundTypeDialog(WorkflowAction action) {
            super(action, "new-compound-type");
        }
    }

}
