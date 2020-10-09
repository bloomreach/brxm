/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.function.Function;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.version.Version;

import org.apache.wicket.Component;
import org.apache.wicket.migrate.StringResourceModelMigration;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.FeedbackStdWorkflow;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.BranchIdModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimePrinter;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionWorkflowPlugin extends RenderPlugin {

    private static final Logger log = LoggerFactory.getLogger(VersionWorkflowPlugin.class);

    public VersionWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final String restoreToBranchId = getBranchInfo(context, BranchIdModel::getBranchId);
        final String restoreToBranchName = getBranchInfo(context, BranchIdModel::getBranchName);

        final String revisionBranchName = getRevisionBranchName();


        add(new FeedbackStdWorkflow("created-for", StringResourceModelMigration.of("created-for-branch", this, null,
                new LoadableDetachableModel<String>() {
                    @Override
                    protected String load() {
                        return revisionBranchName;
                    }
                },
                new LoadableDetachableModel<String>() {
                    @Override
                    protected String load() {
                        try {
                            Node frozenNode = ((WorkflowDescriptorModel) VersionWorkflowPlugin.this.getDefaultModel()).getNode();
                            Node versionNode = frozenNode.getParent();
                            Calendar calendar = versionNode.getProperty("jcr:created").getDate();
                            return DateTimePrinter.of(calendar).print(FormatStyle.LONG, FormatStyle.MEDIUM);
                        } catch (ValueFormatException e) {
                            log.error("Value is not a date", e);
                        } catch (PathNotFoundException e) {
                            log.error("Could not find node", e);
                        } catch (RepositoryException e) {
                            log.error("Repository error", e);
                        }
                        return null;
                    }
                }), new Model<>(Boolean.TRUE)
        ));

        add(new FeedbackStdWorkflow("restoreto", StringResourceModelMigration.of("restore-to", this, null,
                new LoadableDetachableModel<String>() {
                    @Override
                    protected String load() {
                        return restoreToBranchName;
                    }
                }), new Model<>(Boolean.TRUE)));

        add(new StdWorkflow("restore", new StringResourceModel("restore", this), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected ResourceReference getIcon() {
                if (isEnabled()) {
                    return new PackageResourceReference(getClass(), "img/restore-16.png");
                } else {
                    return new PackageResourceReference(getClass(), "img/restore-disabled-16.png");
                }
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                WorkflowDescriptorModel model = getModel();

                Node frozenNode = model.getNode();
                Session session = frozenNode.getSession();

                DocumentWorkflow documentWorkflow = model.getWorkflow();

                Version versionNode = (Version) frozenNode.getParent();
                // create a revision to prevent loss of content from unpublished.
                final Document doc = documentWorkflow.restoreVersionToBranch(versionNode, restoreToBranchId);

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

        add(new StdWorkflow("select", new StringResourceModel("select", this), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected ResourceReference getIcon() {
                if (isEnabled()) {
                    return new PackageResourceReference(getClass(), "img/select-16.png");
                } else {
                    return new PackageResourceReference(getClass(), "img/select-disabled-16.png");
                }
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new HistoryDialog(wdm, getEditorManager(), getBranchIdModel(context).getBranchId());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                // TODO
                return null;
            }
        });

        add(new StdWorkflow("cancel", new StringResourceModel("cancel", this), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TIMES_CIRCLE);
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            public String execute(Workflow wf) throws Exception {
                IEditor editor = getEditor();
                editor.close();
                return null;
            }
        });
    }

    private String getRevisionBranchName() {
        try {
            return JcrUtils.getStringProperty(getModel().getNode(), HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "Core");
        } catch (RepositoryException e) {
            log.error("Exception while trying to get property", e);
            return "Core";
        }
    }

    private String getBranchInfo(final IPluginContext context, final Function<BranchIdModel, String> function) {
        final BranchIdModel branchIdModel = getBranchIdModel(context);
        return function.apply(branchIdModel);
    }

    private BranchIdModel getBranchIdModel(final IPluginContext context) {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow();
        try {
            final String identifier = documentWorkflow.getNode().getIdentifier();
            return new BranchIdModel(context, identifier);
        } catch (RepositoryException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private DocumentWorkflow getDocumentWorkflow() {
        return getModel().getWorkflow();
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
