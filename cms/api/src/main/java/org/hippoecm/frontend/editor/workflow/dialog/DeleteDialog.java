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
package org.hippoecm.frontend.editor.workflow.dialog;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IModel;
import org.hippoecm.addon.workflow.AbstractWorkflowDialogRestyling;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.workflow.model.ReferringDocumentsProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.repository.api.WorkflowDescriptor;

public class DeleteDialog extends AbstractWorkflowDialogRestyling<WorkflowDescriptor> {

    public DeleteDialog(IModel<String> title, WorkflowDescriptorModel model, IModel<String> notification,
                        IWorkflowInvoker invoker, IEditorManager editorMgr) {
        super(invoker, model, title);

        setNotification(notification);
        setFocusOnCancel();

        add(CssClass.append("hippo-delete-dialog"));

        final Node node;
        try {
            node = model.getNode();
        } catch (RepositoryException e) {
            throw new WicketRuntimeException("No document node present", e);
        }

        final JcrNodeModel nodeModel = new JcrNodeModel(node);
        final ReferringDocumentsProvider provider = new ReferringDocumentsProvider(nodeModel, true);
        final MarkupContainer documentsView = new ReferringDocumentsView("links", provider, editorMgr) {
            @Override
            public int getPageSize() {
                return 5;
            }
        };
        add(documentsView);

        setSize(provider.size() > 0 ? DialogConstants.LARGE_AUTO : DialogConstants.MEDIUM_AUTO);
    }
}
