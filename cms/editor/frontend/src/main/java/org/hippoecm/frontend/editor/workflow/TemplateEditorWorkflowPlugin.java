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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.editor.NamespaceValidator;
import org.hippoecm.editor.repository.TemplateEditorWorkflow;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;

public class TemplateEditorWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    public TemplateEditorWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new WorkflowAction("create", new StringResourceModel("create-namespace", this, null)) {
            private static final long serialVersionUID = 1L;

            public String url;
            public String prefix;

            @Override
            protected Dialog createRequestDialog() {
                return new NamespaceDialog(this);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                NamespaceValidator.checkName(prefix);
                NamespaceValidator.checkURI(url);

                TemplateEditorWorkflow workflow = (TemplateEditorWorkflow) wf;
                String nsPath = workflow.createNamespace(prefix, url);

                Session session = UserSession.get().getJcrSession();
                if (session.itemExists(nsPath + "/basedocument")) {
                    Node baseDocNode = session.getNode(nsPath + "/basedocument");
                    NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                    if (!ntMgr.hasNodeType(prefix + ":basedocument")) {
                        NodeTypeTemplate ntTpl = ntMgr.createNodeTypeTemplate();
                        ntTpl.setName(prefix + ":basedocument");
                        if (baseDocNode.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
                            Node draft = baseDocNode.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                            if (draft.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                                Value[] supers = draft.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
                                String[] superStrings = new String[supers.length];
                                for (int i = 0; i < supers.length; i++) {
                                    superStrings[i] = supers[i].getString();
                                }
                                ntTpl.setDeclaredSuperTypeNames(superStrings);
                            }
                        }
                        ntTpl.setOrderableChildNodes(true);
                        ntMgr.registerNodeType(ntTpl, false);
                    }
                }

                return null;
            }
        });
    }

    public class NamespaceDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
        private static final long serialVersionUID = 1L;

        public NamespaceDialog(CompatibilityWorkflowPlugin.WorkflowAction action) {
            action.super();
            add(setFocus(new TextFieldWidget("prefix", new PropertyModel(action, "prefix"))));
            add(new TextFieldWidget("url", new PropertyModel(action, "url")));
        }

        @Override
        public IModel getTitle() {
            return new StringResourceModel("create-namespace", TemplateEditorWorkflowPlugin.this, null);
        }

        @Override
        public IValueMap getProperties() {
            return DialogConstants.SMALL;
        }
    }
}
