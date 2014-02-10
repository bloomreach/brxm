package org.hippoecm.frontend.plugins.reviewedactions;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.workflow.dialog.DocumentMetadataDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.Workflow;

public class DocMetaDataPlugin extends AbstractDocumentWorkflowPlugin {

    public DocMetaDataPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("docMetaData", new StringResourceModel("docmetadata-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "docmetadata-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                return new DocumentMetadataDialog(wdm);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

    }
}
