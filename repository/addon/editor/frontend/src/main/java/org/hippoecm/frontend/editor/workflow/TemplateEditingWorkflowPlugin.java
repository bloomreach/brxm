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

import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEditingWorkflowPlugin extends CompatibilityWorkflowPlugin implements IValidateService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(TemplateEditingWorkflowPlugin.class);

    // FIXME: should this be non-transient?
    private transient boolean validated = false;
    private transient boolean isvalid = true;

    public TemplateEditingWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        if (config.getString(IValidateService.VALIDATE_ID) != null) {
            context.registerService(this, config.getString(IValidateService.VALIDATE_ID));
        } else {
            log.warn("No validator id {} defined", IValidateService.VALIDATE_ID);
        }

        IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
        context.registerService(new IEditorFilter() {
            private static final long serialVersionUID = 1L;

            public void postClose(Object object) {
                // nothing to do
            }

            public Object preClose() {
                try {
                    Node node = ((WorkflowDescriptorModel) getModel()).getNode();
                    boolean dirty = node.isModified();
                    if (!dirty) {
                        HippoSession session = (HippoSession) node.getSession();
                        NodeIterator nodes = session.pendingChanges(node, "nt:base", true);
                        if (nodes.hasNext()) {
                            dirty = true;
                        }
                    }
                    if (dirty) {
                        IDialogService dialogService = context.getService(IDialogService.class.getName(),
                                IDialogService.class);
                        dialogService.show(new OnCloseDialog());
                    } else {
                        return new Object();
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                    return new Object();
                }
                return null;
            }

        }, context.getReference(editor).getServiceId());

        add(new WorkflowAction("save", new StringResourceModel("save", this, null)) {
            @Override
            protected String execute(Workflow workflow) throws Exception {
                doSave();
                return null;
            }});
        add(new WorkflowAction("done", new StringResourceModel("done", this, null)) {
            @Override
            protected String execute(Workflow workflow) throws Exception {
                doSave();
                IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
                editor.setMode(IEditor.Mode.VIEW);
                return null;
            }});
    }

    void doSave() throws Exception {
        ((UserSession) Session.get()).getJcrSession().save();
    }

    void doRevert() throws Exception {
        WorkflowDescriptorModel model = (WorkflowDescriptorModel) TemplateEditingWorkflowPlugin.this.getModel();
        model.getNode().refresh(false);
    }

    void closeEditor() {
        IEditor editor = getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditor.class);
        try {
            editor.close();
        } catch (EditorException e) {
            log.error("Could not close editor", e);
        }
    }

    public boolean hasError() {
        if (!validated) {
            validate();
        }
        return !isvalid;
    }

    public void validate() {
        validated = true;
        isvalid = true;
        try {
            Node node = ((WorkflowDescriptorModel) getModel()).getNode();
            if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                NodeIterator ntNodes = node.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)
                        .getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                Node ntNode = null;
                while (ntNodes.hasNext()) {
                    Node child = ntNodes.nextNode();
                    if (!child.isNodeType(HippoNodeType.NT_REMODEL)) {
                        ntNode = child;
                        break;
                    }
                }
                if (ntNode != null) {
                    HashSet<String> paths = new HashSet<String>();
                    NodeIterator fieldIter = ntNode.getNodes(HippoNodeType.HIPPO_FIELD);
                    while (fieldIter.hasNext()) {
                        Node field = fieldIter.nextNode();
                        String path = field.getProperty(HippoNodeType.HIPPO_PATH).getString();
                        if (paths.contains(path)) {
                            error("Path " + path + " is used in multiple fields");
                            isvalid = false;
                        }
                        if (!path.equals("*")) {
                            paths.add(path);
                        }
                    }
                } else {
                    log.error("Draft nodetype not found");
                }
            } else {
                log.warn("Unknown node type {}", node.getPrimaryNodeType().getName());
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    protected void onModelChanged() {
        validated = false;
        super.onModelChanged();
    }

    private class OnCloseDialog extends AbstractDialog {
        private static final long serialVersionUID = 1L;

        public OnCloseDialog() {

            setOkVisible(false);

            final Label exceptionLabel = new Label("exception", "");
            exceptionLabel.setOutputMarkupId(true);
            add(exceptionLabel);

            AjaxButton button = new AjaxButton(getButtonId()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    try {
                        doRevert();
                        closeDialog();
                        closeEditor();
                    } catch (Exception ex) {
                        exceptionLabel.setModel(new Model(ex.getMessage()));
                        target.addComponent(exceptionLabel);
                    }
                }
            };
            button.setModel(new ResourceModel("discard", "Discard"));
            addButton(button);

            button = new AjaxButton(getButtonId()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    try {
                        doSave();
                        closeDialog();
                        closeEditor();
                    } catch (Exception ex) {
                        exceptionLabel.setModel(new Model(ex.getMessage()));
                        target.addComponent(exceptionLabel);
                    }
                }
            };
            button.setModel(new ResourceModel("save", "Save"));
            addButton(button);
        }

        public IModel getTitle() {
            try {
                return new StringResourceModel("close-document", this, null, new Object[] { new PropertyModel(
                        ((WorkflowDescriptorModel)TemplateEditingWorkflowPlugin.this.getModel()).getNode(), "name") }, "Close {0}");
            } catch(RepositoryException ex) {
                return new StringResourceModel("close-document", this, null, new Object[] { });
            }
        }

    }

}
