/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PreviewWorkflowPlugin.class);

    @SuppressWarnings("unused") // used by a PropertyModel
    private String inUseBy = StringUtils.EMPTY;

    private StdWorkflow infoAction;
    private StdWorkflow infoEditAction;
    private StdWorkflow editAction;

    public PreviewWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel(HippoStdNodeType.NT_PUBLISHABLESUMMARY));
        add(infoAction = new StdWorkflow("info", "info") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                return translator.getValueName(HippoStdNodeType.HIPPOSTD_STATESUMMARY,
                        new PropertyModel<String>(PreviewWorkflowPlugin.this, "stateSummary"));
            }

            @Override
            protected void invoke() {
            }
        });

        add(infoEditAction = new StdWorkflow("infoEdit", "infoEdit") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                return new StringResourceModel("in-use-by", this, null,
                        new PropertyModel(PreviewWorkflowPlugin.this, "inUseBy"));
            }

            @Override
            protected void invoke() {
            }
        });

        add(editAction = new StdWorkflow("edit", new StringResourceModel("edit-label", this, null), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/edit-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Session session = UserSession.get().getJcrSession();
                session.refresh(true);
                Node docNode = session.getNodeByIdentifier(docRef.getIdentity());
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
                return null;
            }
        });

        hideInvalidActions();
    }

    @SuppressWarnings("unused")  // used by a PropertyModel
    public String getStateSummary() {
        try {
            WorkflowDescriptorModel wdm = getModel();
            Node handleNode = wdm.getNode();
            for (Node child : new NodeIterable(handleNode.getNodes())) {
                if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)) {
                    return child.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString();
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Unable to ascertain state summary", ex);
        }
        return "";
    }

    private void hideInvalidActions() {
        Map<String, Serializable> info = getHints();

        hideIfNecessary(info, "obtainEditableInstance", editAction);

        hideIfNecessary(info, "status", infoAction);

        if (info.containsKey("inUseBy") && info.get("inUseBy") instanceof String) {
            inUseBy = (String) info.get("inUseBy");
            infoEditAction.setVisible(true);
        } else {
            infoEditAction.setVisible(false);
        }
    }

}
