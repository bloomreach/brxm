/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.version.Version;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(VersionWorkflowPlugin.class);

    public VersionWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                return new StringResourceModel("created", this, null, new LoadableDetachableModel<Date>() {

                    protected Date load() {
                        try {
                            Node frozenNode = ((WorkflowDescriptorModel) VersionWorkflowPlugin.this.getDefaultModel()).getNode();
                            Node versionNode = frozenNode.getParent();
                            Calendar calendar = versionNode.getProperty("jcr:created").getDate();
                            return calendar.getTime();
                        } catch (ValueFormatException e) {
                            log.error("Value is not a date", e);
                        } catch (PathNotFoundException e) {
                            log.error("Could not find node", e);
                        } catch (RepositoryException e) {
                            log.error("Repository error", e);
                        }
                        return null;
                    }

                });
            }

            @Override
            protected void invoke() {
            }
        });

        add(new StdWorkflow("restore", new StringResourceModel("restore", this, null), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/restore-16.png");
            }

            @Override
            public boolean isVisible() {
                Node frozenNode;
                try {
                    frozenNode = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    String primaryType = frozenNode.getProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE).getString();
                    String prefix = primaryType.substring(0, primaryType.indexOf(':'));
                    if (prefix.contains("_")) {
                        return false;
                    }
                } catch (RepositoryException e) {
                    log.warn("Could not determine whether to enable restore button", e);
                }
                return true;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                WorkflowDescriptorModel model = getModel();

                Node frozenNode = model.getNode();
                Session session = frozenNode.getSession();

                DocumentWorkflow documentWorkflow = model.getWorkflow();

                Version versionNode = (Version) frozenNode.getParent();
                Calendar calendar = versionNode.getCreated();
                // create a revision to prevent loss of content from unpublished.
                documentWorkflow.version();
                Document doc = documentWorkflow.obtainEditableInstance();
                try {
                    documentWorkflow.versionRestoreTo(calendar, doc);
                } finally {
                    doc = documentWorkflow.commitEditableInstance();
                }

                JcrNodeModel previewModel = new JcrNodeModel(session.getNodeByIdentifier(doc.getIdentity()));
                IEditorManager editorMgr = getEditorManager();
                IEditor editor = editorMgr.getEditor(previewModel);
                if (editor == null) {
                    editor = editorMgr.openPreview(previewModel);
                }
                IRenderService renderer = getEditorRenderer(editor);
                if (renderer != null) {
                    renderer.focus(null);
                }

                editor = getEditor();
                editor.close();
                return null;
            }
        });

        add(new StdWorkflow("select", new StringResourceModel("select", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/select-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new HistoryDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                // TODO
                return null;
            }
        });
    }

    @Override
    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) super.getModel();
    }

    private IEditor getEditor() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditor.class);
    }

    private IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    private IRenderService getEditorRenderer(IEditor editor) {
        IPluginContext context = getPluginContext();
        return getPluginContext().getService(context.getReference(editor).getServiceId(), IRenderService.class);
    }

}
