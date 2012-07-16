/*
 *  Copyright 2008 Hippo.
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

import javax.jcr.RepositoryException;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.workflow.model.ReferringDocumentsProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.IEditorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteDialog extends WorkflowAction.WorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DeleteDialog.class);

    private IModel title;

    public DeleteDialog(IModel<String> title, IModel<String> message, CompatibilityWorkflowPlugin.WorkflowAction action,
            IEditorManager editorMgr) {
        action.super(message);

        this.title = title;

        try {
            WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) action.getDefaultModel();
            ReferringDocumentsProvider provider = new ReferringDocumentsProvider(new JcrNodeModel(wdm.getNode()), true);
            MarkupContainer rdv = new ReferringDocumentsView("links", provider, editorMgr) {
                private static final long serialVersionUID = 1L;

                @Override
                public int getPageSize() {
                    return 5;
                }
            };
            add(rdv);
        } catch (RepositoryException e) {
            throw new WicketRuntimeException("No document node present", e);
        }
        add(new CssClassAppender(new Model("hippo-delete-dialog")));
    }

    @Override
    protected void init() {
        setFocusOnCancel();
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

    @Override
    public IModel getTitle() {
        return title;
    }

}
