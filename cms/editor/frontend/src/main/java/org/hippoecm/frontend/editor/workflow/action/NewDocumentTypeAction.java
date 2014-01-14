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
package org.hippoecm.frontend.editor.workflow.action;

import java.rmi.RemoteException;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.editor.NamespaceValidator;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.editor.template.JcrTemplateStore;
import org.hippoecm.editor.type.JcrDraftStore;
import org.hippoecm.editor.type.JcrTypeStore;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.NamespaceWorkflowPlugin;
import org.hippoecm.frontend.editor.workflow.TemplateFactory;
import org.hippoecm.frontend.editor.workflow.dialog.CreateDocumentTypeDialog;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewDocumentTypeAction extends Action {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NewDocumentTypeAction.class);

    private ILayoutProvider layoutProvider;

    public String name;
    public String layout;
    public List<String> documentTypes;
    public String superType = "basedocument";

    public NewDocumentTypeAction(NamespaceWorkflowPlugin plugin, String id, StringResourceModel name, ILayoutProvider layouts) {
        super(plugin, id, name);
        this.layoutProvider = layouts;
    }

    @Override
    protected Dialog createRequestDialog() {
        WorkflowDescriptor descriptor = (WorkflowDescriptor) plugin.getDefaultModel().getObject();
        WorkflowManager manager = UserSession.get().getWorkflowManager();
        try {
            NamespaceWorkflow namespaceWorkflow = (NamespaceWorkflow) manager.getWorkflow(descriptor);
            documentTypes = (List<String>) namespaceWorkflow.hints().get("documentTypes");
        } catch (RepositoryException | RemoteException | WorkflowException e) {
            log.error("Could not determine list of document types", e);
        }

        return new CreateDocumentTypeDialog(this, layoutProvider);
    }

    @Override
    protected String execute(Workflow wf) throws Exception {
        NamespaceValidator.checkName(name);

        if (layout == null) {
            throw new Exception("No layout specified");
        }

        NamespaceWorkflow workflow = (NamespaceWorkflow) wf;
        try {
            if (superType == null) {
                workflow.addDocumentType(name);
            } else {
                workflow.addDocumentType(name, superType);
            }
        } catch (ItemExistsException ex) {
            return "Type " + name + " already exists";
        }

        String prefix = (String) workflow.hints().get("prefix");

        JcrTypeStore typeStore = new JcrTypeStore();
        JcrDraftStore draftStore = new JcrDraftStore(typeStore, prefix);
        BuiltinTypeStore builtinStore = new BuiltinTypeStore();
        ITypeLocator typeLocator = new TypeLocator(new IStore[]{draftStore, typeStore, builtinStore});
        typeStore.setTypeLocator(typeLocator);
        builtinStore.setTypeLocator(typeLocator);

        // create layout
        // FIXME: should be managed by template engine
        JcrTemplateStore templateStore = new JcrTemplateStore(typeLocator);
        IClusterConfig template = new TemplateFactory().createTemplate(layoutProvider.getDescriptor(layout));
        templateStore.save(template, typeLocator.locate(prefix + ":" + name));

        openEditor(prefix + ":" + name);

        return null;
    }

    @Override
    protected ResourceReference getIcon() {
        return new PackageResourceReference(StdWorkflow.class, "doctype-new-16.png");
    }

}
