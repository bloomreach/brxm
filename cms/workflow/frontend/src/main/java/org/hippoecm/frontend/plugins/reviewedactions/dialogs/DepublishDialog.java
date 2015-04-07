/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import javax.jcr.RepositoryException;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialogRestyling;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.workflow.dialog.ReferringDocumentsView;
import org.hippoecm.frontend.editor.workflow.model.ReferringDocumentsProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.repository.api.WorkflowDescriptor;

public class DepublishDialog extends AbstractWorkflowDialogRestyling<WorkflowDescriptor> {

    private IModel<String> title;

    public DepublishDialog(IModel<String>title, IModel<String> message,
            WorkflowDescriptorModel wdm, IWorkflowInvoker action, IEditorManager editorMgr) {
        super(wdm, message, action);

        this.title = title;

        try {
            ReferringDocumentsProvider provider = new ReferringDocumentsProvider(new JcrNodeModel(wdm.getNode()));
            MarkupContainer rdv = new ReferringDocumentsView("links", provider, editorMgr);
            add(rdv);
        } catch (RepositoryException e) {
            throw new WicketRuntimeException("No document node present", e);
        }

        add(CssClass.append("hippo-window"));
        add(CssClass.append("hippo-depublish-dialog"));

        setFocusOnOk();
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.LARGE_AUTO;
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    protected void onDetach() {
        title.detach();
        super.onDetach();
    }
}
