/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.editor.NamespaceValidator;
import org.hippoecm.editor.repository.TemplateEditorWorkflow;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.RequiredTextFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;

public class TemplateEditorWorkflowPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static final String NODETYPE = HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE;

    public TemplateEditorWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<String> titleModel = new StringResourceModel("create-namespace", this);
        final WorkflowDescriptorModel descriptorModel = (WorkflowDescriptorModel) getModel();
        final IModel<Namespace> namespaceModel = Model.of(new Namespace());
        final NamespaceWorkflow workflow = new NamespaceWorkflow("create", titleModel, context, descriptorModel, namespaceModel);

        add(workflow);
    }

    private static class NamespaceWorkflow<K extends Workflow> extends StdWorkflow<K> {

        private final IModel<Namespace> namespaceModel;

        public NamespaceWorkflow(final String id, final IModel<String> name, final IPluginContext pluginContext,
                                 final WorkflowDescriptorModel model, IModel<Namespace> namespaceModel) {
            super(id, name, pluginContext, model);

            this.namespaceModel = namespaceModel;
        }

        @Override
        protected Dialog createRequestDialog() {
            return new NamespaceDialog(this, namespaceModel, getTitle());
        }

        @Override
        protected Component getIcon(final String id) {
            final HippoIconStack icon = new HippoIconStack(id, IconSize.M);
            icon.addFromSprite(Icon.GEAR);
            icon.addFromCms(CmsIcon.OVERLAY_PLUS, IconSize.M, HippoIconStack.Position.TOP_LEFT);
            return icon;
        }

        @Override
        protected String execute(Workflow wf) throws Exception {
            final Session session = UserSession.get().getJcrSession();
            final NodeTypeManager typeManager = session.getWorkspace().getNodeTypeManager();
            final TemplateEditorWorkflow workflow = (TemplateEditorWorkflow) wf;

            final String prefix = namespaceModel.getObject().getPrefix();
            final String url = namespaceModel.getObject().getUrl();

            final String namespacePath = workflow.createNamespace(prefix, url);
            final String baseDocPath = namespacePath + "/basedocument";

            if (!session.itemExists(baseDocPath)) {
                throw new WorkflowException("Namespace created at " + namespacePath + " is missing a basedocument node");
            }

            final String baseDocTypeName = prefix + ":basedocument";

            if (!typeManager.hasNodeType(baseDocTypeName)) {
                createNodeTypeTemplate(session, typeManager, baseDocPath, baseDocTypeName);
            }

            return null;
        }

        private void createNodeTypeTemplate(final Session session, final NodeTypeManager typeManager, final String baseDocPath, final String baseDocTypeName) throws RepositoryException {
            final Node baseDocNode = session.getNode(baseDocPath);
            final NodeTypeTemplate baseDocTemplate = typeManager.createNodeTypeTemplate();
            baseDocTemplate.setName(baseDocTypeName);

            if (baseDocNode.hasNode(NODETYPE)) {
                Node draft = baseDocNode.getNode(NODETYPE);
                if (draft.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                    Value[] supers = draft.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
                    String[] superStrings = new String[supers.length];
                    for (int i = 0; i < supers.length; i++) {
                        superStrings[i] = supers[i].getString();
                    }
                    baseDocTemplate.setDeclaredSuperTypeNames(superStrings);
                }
            }
            baseDocTemplate.setOrderableChildNodes(true);
            typeManager.registerNodeType(baseDocTemplate, false);
        }
    }

    private static class Namespace implements IClusterable {

        private String url;
        private String prefix;

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }
    }

    private static class NamespaceDialog extends WorkflowDialog<Namespace> {

        public NamespaceDialog(final IWorkflowInvoker invoker, final IModel<Namespace> namespaceModel,
                               final IModel<String> titleModel) {
            super(invoker, namespaceModel, titleModel);

            final PropertyModel<String> prefixModel = PropertyModel.of(namespaceModel, "prefix");
            final Model<String> prefixLabel = Model.of(getString("prefix"));
            final TextFieldWidget prefixField = new RequiredTextFieldWidget("prefix", prefixModel, prefixLabel);

            prefixField.getFormComponent().add(NamespaceValidator.createNameValidator());
            add(prefixField);

            final PropertyModel<String> urlModel = PropertyModel.of(namespaceModel, "url");
            final Model<String> urlLabel = Model.of(getString("url"));
            final TextFieldWidget urlField = new RequiredTextFieldWidget("url", urlModel, urlLabel);

            urlField.getFormComponent().add(NamespaceValidator.createUrlValidator());
            add(urlField);

            setFocus(prefixField);
            setSize(DialogConstants.SMALL_AUTO);
        }
    }
}
