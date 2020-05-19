/*
 *  Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.addon.workflow.DestinationDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.workflow.CopyNameHelper;
import org.hippoecm.frontend.editor.workflow.dialog.DeleteDialog;
import org.hippoecm.frontend.editor.workflow.dialog.WhereUsedDialog;
import org.hippoecm.frontend.model.BranchIdModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standardworkflow.RenameDocumentArguments;
import org.hippoecm.frontend.plugins.standardworkflow.RenameDocumentDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public class DocumentWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    private static final String DEFAULT_FOLDERWORKFLOW_CATEGORY = "embedded";
    public static final String DOCUMENT = "document";
    public static final String UNAVAILABLE_TIP = "unavailable-tip";
    public static final String DELETE = "delete";
    /** workflow action key, that determines the visibility of the "Keep draft" button
     * and if the documentInfo shows "draft changes" */
    private static final String EDIT_DRAFT = "editDraft";
    /** workflow action key, that determines the visibility of the "Publish" button
     * and if the documentInfo show "unpublished changes"  */
    private static final String PUBLISH = "publish";
    /** workflow action key, that determinites if the visibility of the "Unpublish" button
     * and if the documentInfo shows "offline" or "live"  */
    private static final String DEPUBLISH = "depublish";
    /**
     * Id and name of the document Info.
     * The id should start with "info", see MenuHierarchy.
     */
    private static final String INFO_DOCUMENT_INFO = "infoDocumentInfo";

    private StdWorkflow deleteAction;
    private StdWorkflow renameAction;
    private StdWorkflow copyAction;
    private StdWorkflow moveAction;
    private StdWorkflow whereUsedAction;
    private StdWorkflow historyAction;
    private BranchIdModel branchIdModel;


    public DocumentWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(renameAction = new StdWorkflow("rename", new StringResourceModel("rename-label", this), context, getModel()) {
            private RenameDocumentArguments renameDocumentArguments;

            @Override
            public String getSubMenu() {
                return DOCUMENT;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TYPE);
            }

            @Override
            protected IModel getTooltip() {
                if (isEnabled()) {
                    return super.getTooltip();
                } else {
                    return new StringResourceModel(UNAVAILABLE_TIP, this);
                }
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                String locale = null;
                try {
                    final HippoNode node = getHandleOrUnpublishedVariant(getModel().getWorkflow());
                    locale = CodecUtils.getLocaleFromNodeAndAncestors(node);
                    renameDocumentArguments = new RenameDocumentArguments(
                            node.getDisplayName(),
                            node.getName()
                    );
                } catch (RepositoryException ex) {
                    renameDocumentArguments = new RenameDocumentArguments();
                }

                return new RenameDocumentDialog(renameDocumentArguments,
                        new StringResourceModel("rename-title", DocumentWorkflowPlugin.this),
                        this,
                        CodecUtils.getNodeNameCodecModel(context, locale),
                        this.getModel());
            }


            @Override
            protected String execute(Workflow wf) throws Exception {
                final String targetName = renameDocumentArguments.getTargetName();
                final String uriName = renameDocumentArguments.getUriName();

                if (Strings.isEmpty(targetName)) {
                    throw new WorkflowException("No name given for document node");
                }
                if (Strings.isEmpty(uriName)) {
                    throw new WorkflowException("No URL name given for document node");
                }

                final HippoNode node = getHandleOrUnpublishedVariant(getModel().getWorkflow());
                String nodeName = getNodeNameCodec(node).encode(uriName);
                String localName = getLocalizeCodec().encode(targetName);

                if (Strings.isEmpty(nodeName)) {
                    throw new IllegalArgumentException("You need to enter a name");
                }

                WorkflowManager manager = UserSession.get().getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!((WorkflowDescriptorModel) getDefaultModel()).getNode().getName().equals(nodeName)) {
                    ((DocumentWorkflow) wf).rename(nodeName);
                }
                if (!node.getDisplayName().equals(localName)) {
                    defaultWorkflow.setDisplayName(localName);
                }
                return null;
            }
        });

        add(copyAction = new StdWorkflow("copy", new StringResourceModel("copy-label", this), context, getModel()) {
            NodeModelWrapper<Node> destination = null;
            String name = null;

            @Override
            public String getSubMenu() {
                return DOCUMENT;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.FILES);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                destination = new NodeModelWrapper<Node>(getFolder()) {
                };

                try {
                    IModel<StringCodec> codec = ReadOnlyModel.of(() -> getNodeNameCodec(getModelNode(), getFolder().getNode()));

                    final CopyNameHelper copyNameHelper = new CopyNameHelper(codec, translate("copyof"));
                    final Node destinationNode = destination.getChainedModel().getObject();
                    name = copyNameHelper.getCopyName(getModelNode().getDisplayName(), destinationNode);
                } catch (RepositoryException ex) {
                    return new ExceptionDialog(ex);
                }
                return new DestinationDialog(Model.of(translate("copy-title")),
                        Model.of(translate("copy-name")),
                        PropertyModel.of(this, "name"),
                        destination,
                        getPluginContext(),
                        getPluginConfig()) {
                    {
                        setOkEnabled(true);
                    }

                    @Override
                    public void invokeWorkflow() throws Exception {
                        copyAction.invokeWorkflow();
                    }

                    @Override
                    protected boolean checkFolderTypes() {
                        return isDocumentAllowedInFolder(DocumentWorkflowPlugin.this.getModel(), destination.getChainedModel());
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node folder = destination != null ? destination.getChainedModel().getObject() :
                        new JcrNodeModel("/").getNode();
                Node document = getModel().getNode();

                String nodeName = getNodeNameCodec(document, folder).encode(name);

                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.copy(new Document(folder), nodeName, getBranchIdModel().getBranchId());

                JcrNodeModel resultModel = new JcrNodeModel(folder.getPath() + "/" + nodeName);
                Node result = resultModel.getNode();

                WorkflowManager manager = UserSession.get().getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", result.getNode(nodeName));
                defaultWorkflow.setDisplayName(getLocalizeCodec().encode(name));

                browseTo(resultModel);
                return null;
            }

            private HippoNode getModelNode() {
                return getHandleOrUnpublishedVariant(getModel().getWorkflow());
            }

        });

        add(moveAction = new StdWorkflow("move", new StringResourceModel("move-label", this), context, getModel()) {
            NodeModelWrapper<Node> destination;

            @Override
            public String getSubMenu() {
                return DOCUMENT;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.MOVE_INTO);
            }

            @Override
            protected IModel getTooltip() {
                if (isEnabled()) {
                    return super.getTooltip();
                } else {
                    return new StringResourceModel(UNAVAILABLE_TIP, this);
                }
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                destination = new NodeModelWrapper<Node>(getFolder()) {
                };
                return new DestinationDialog(Model.of(translate("move-title")), null, null, destination,
                        getPluginContext(), getPluginConfig()) {
                    @Override
                    public void invokeWorkflow() throws Exception {
                        moveAction.invokeWorkflow();
                    }

                    @Override
                    protected boolean checkFolderTypes() {
                        return isDocumentAllowedInFolder(DocumentWorkflowPlugin.this.getModel(), destination.getChainedModel());
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node document = getHandleOrUnpublishedVariant(getModel().getWorkflow());
                Node folder = destination != null ? destination.getChainedModel().getObject()
                        : new JcrNodeModel("/").getNode();

                String nodeName = document.getName();
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.move(new Document(folder), nodeName);
                browseTo(new JcrNodeModel(folder.getPath() + "/" + nodeName));
                return null;
            }
        });

        add(deleteAction = new StdWorkflow(DELETE,
                new StringResourceModel("delete-label", this), context, getModel()) {

            @Override
            public String getSubMenu() {
                return DOCUMENT;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TIMES);
            }

            @Override
            protected IModel getTooltip() {
                if (isEnabled()) {
                    return super.getTooltip();
                } else {
                    return new StringResourceModel("unavailable-tip", this);
                }
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                IModel<String> message = new StringResourceModel("delete-message", DocumentWorkflowPlugin.this)
                        .setParameters(getDocumentName());
                IModel<String> title = new StringResourceModel("delete-title", DocumentWorkflowPlugin.this);
                return new DeleteDialog(title, getModel(), message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.delete();
                return null;
            }
        });

        add(whereUsedAction = new StdWorkflow("where-used", new StringResourceModel("where-used-label", this), context, getModel()) {

            @Override
            public String getSubMenu() {
                return DOCUMENT;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.LINK);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new WhereUsedDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

        add(historyAction = new StdWorkflow("history", new StringResourceModel("history-label", this), context, getModel()) {

            @Override
            public String getSubMenu() {
                return DOCUMENT;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.RESTORE);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new HistoryDialog(wdm, getEditorManager(), getBranchId());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

        addBranchInfo();

        Map<String, Serializable> info = getHints();
        hideOrDisable(info, DELETE, deleteAction);
        hideOrDisable(info, "rename", renameAction);
        hideOrDisable(info, "move", moveAction);

        hideOrDisable(info, "copy", copyAction);
        hideOrDisable(info, "listVersions", historyAction);
    }

    private void addBranchInfo() {
        addOrReplace(new StdWorkflow<DocumentWorkflow>(INFO_DOCUMENT_INFO
                , INFO_DOCUMENT_INFO) {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel<String> getTitle() {
                final UnaryOperator<String> resolver = key ->
                        new StringResourceModel(key, DocumentWorkflowPlugin.this).getString();
                final Map<String, Serializable> hints = getHints();
                final String info = new BranchInfoBuilder(resolver, getBranchNameResolver())
                        .draftChanges(isActionAllowed(hints, EDIT_DRAFT))
                        .unpublishedChanges(isActionAllowed(hints, PUBLISH))
                        .live(isActionAllowed(hints, DEPUBLISH))
                        .build();
                return new Model<>(info);
            }

            @Override
            protected void invoke() {
                // do not invoke workflow
            }
        });
    }

    /**
     * @return the branchName of the current branch or {@link BranchConstants#MASTER_BRANCH_ID} if there isn't any
     * current branch
     */
    protected Supplier<String> getBranchNameResolver() {
        return () -> BranchConstants.MASTER_BRANCH_ID;
    }

    private static boolean isDocumentAllowedInFolder(final WorkflowDescriptorModel documentModel, IModel<Node> destinationFolder) {

        try {
            final Node handle = getHandleOrUnpublishedVariant(documentModel.getWorkflow());
            if (handle.hasNode(handle.getName())) {
                final String documentType = handle.getNode(handle.getName()).getPrimaryNodeType().getName();

                // get allowed folder types from hints() method on folder workflow
                final Workflow workflow = new WorkflowDescriptorModel(DEFAULT_FOLDERWORKFLOW_CATEGORY, destinationFolder.getObject()).getWorkflow();
                if (workflow instanceof FolderWorkflow) {
                    final Map<String, Set<String>> prototypes = (Map<String, Set<String>>) workflow.hints().get("prototypes");

                    // squash all configured values into one set
                    final Set<String> allowedTypes = new HashSet<>();
                    for (final String key : prototypes.keySet()) {
                        allowedTypes.addAll(prototypes.get(key));
                    }

                    log.debug("Document type {} {} allowed in folder {} by folderTypes {}",
                            documentType, (allowedTypes.contains(documentType) ? "is" : "is NOT"),
                            destinationFolder.getObject().getPath(), allowedTypes);
                    return allowedTypes.contains(documentType);
                } else {
                    log.info("Workflow by category {} on subject {} is not a FolderWorkflow but {}",
                            DEFAULT_FOLDERWORKFLOW_CATEGORY, destinationFolder.getObject(),
                            ((workflow == null) ? "null" : workflow.getClass().getName()));
                }
            } else {
                log.error("(Supposed) document handle {} does not have same-name subnode", handle.getPath());
            }
        } catch (RepositoryException | RemoteException e) {
            log.error(e.getClass().getName() + " during check for workflow allowed in folder: " + e.getMessage());
        } catch (WorkflowException we) {
            log.error(we.getClass().getName() + " during workflow execution", we);
        }

        // forbid workflow action if something's wrong
        return false;
    }

    // Helper method for referencing translations from the DocumentWorkflowPlugin from nested anonymous classes
    private String translate(final String key) {
        return getString(key);
    }

    /**
     * Helper method for fetching a StringCodec for a document. If document has a locale, use it to find a fitting
     * {@link StringCodec}, otherwise search the destination tree for a locale and use that to find a fitting
     * {@link StringCodec}.
     */
    private StringCodec getNodeNameCodec(final Node document, final Node destination) {
        String locale = CodecUtils.getLocaleFromDocumentOrFolder(document, destination);
        return CodecUtils.getNodeNameCodec(getPluginContext(), locale);
    }
}
