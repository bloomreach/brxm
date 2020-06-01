/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.repository.api.WorkflowDescriptor;

public class UnpublishedReferencesDialog extends WorkflowDialog<WorkflowDescriptor> {

    public UnpublishedReferencesDialog(final IWorkflowInvoker invoker, final ISortableDataProvider<Node, String> provider,
                                       final IEditorManager mgr) {
        super(invoker);

        setOutputMarkupId(true);

        setTitleKey("title");
        setCssClass("hippo-workflow-dialog");
        setOkLabel(getString("publish"));

        add(new UnpublishedReferencesView("docsview", provider, mgr));
    }
}
