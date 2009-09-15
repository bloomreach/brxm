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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.version.Version;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(VersionWorkflowPlugin.class);

    WorkflowAction restoreAction;
    WorkflowAction selectAction;

    public VersionWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("created", this, null, new Object[] { new IModel() {

                    public Object getObject() {
                        try {
                            Node frozenNode = ((WorkflowDescriptorModel) VersionWorkflowPlugin.this.getModel())
                                    .getNode();
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

                    public void setObject(Object object) {
                        // TODO Auto-generated method stub

                    }

                    public void detach() {
                        // TODO Auto-generated method stub

                    }

                } }, "unknown");
            }

            @Override
            protected void invoke() {
            }
        });

        add(restoreAction = new WorkflowAction("restore", new StringResourceModel("restore", this, null).getString(),
                null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "restore-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node frozenNode = ((WorkflowDescriptorModel) getModel()).getNode();
                Session session = frozenNode.getSession();
                Version versionNode = (Version) frozenNode.getParent();
                Version handleVersion = JcrHelper.getVersionParent(versionNode);
                Node handle = session.getNodeByUUID(
                        handleVersion.getContainingHistory().getVersionableUUID());

                WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace())
                        .getWorkflowManager();

                Node document = null;
                NodeIterator docs = handle.getNodes(handle.getName());
                while (docs.hasNext()) {
                    Node node = docs.nextNode();
                    if (node.hasProperty("hippostd:state")
                            && "unpublished".equals(node.getProperty("hippostd:state").getString())) {
                        document = node;
                    }
                }

                if (document != null) {
                    // create a revision to prevent loss of content from unpublished.
                    VersionWorkflow versionWorkflow = (VersionWorkflow) workflowManager.getWorkflow("versioning",
                            document);
                    versionWorkflow.version();
                }

                BasicReviewedActionsWorkflow braw = (BasicReviewedActionsWorkflow) workflowManager.getWorkflow(
                        "default", document);
                Document doc = braw.obtainEditableInstance();

                VersionWorkflow versionWorkflow = (VersionWorkflow) workflowManager.getWorkflow("versioning",
                        frozenNode);
                versionWorkflow.restoreTo(doc);

                doc = braw.commitEditableInstance();

                JcrNodeModel unpubModel = new JcrNodeModel(session.getNodeByUUID(doc.getIdentity()));
                IEditorManager editorMgr = getEditorManager();
                IEditor editor = editorMgr.getEditor(unpubModel);
                if (editor == null) {
                    editor = editorMgr.openPreview(unpubModel);
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

        add(selectAction = new WorkflowAction("select", new StringResourceModel("select", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "select-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getModel();
                return new HistoryDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                // TODO
                return null;
            }
        });

        onModelChanged();
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
