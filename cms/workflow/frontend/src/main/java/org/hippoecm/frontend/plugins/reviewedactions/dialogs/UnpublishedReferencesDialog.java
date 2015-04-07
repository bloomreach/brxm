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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialogRestyling;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnpublishedReferencesDialog extends AbstractWorkflowDialogRestyling<WorkflowDescriptor> {

    static final Logger log = LoggerFactory.getLogger(UnpublishedReferencesDialog.class);

    public UnpublishedReferencesDialog(IWorkflowInvoker invoker, ISortableDataProvider<Node, String> provider, IEditorManager mgr) {
        super(null, invoker);

        setOutputMarkupId(true);

        add(new UnpublishedReferencesView("docsview", provider, mgr));

        add(CssClass.append("hippo-workflow-dialog"));

        setOkLabel(getString("publish"));
    }

    public IModel<String> getTitle() {
        return Model.of(getString("title"));
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.LARGE_AUTO;
    }

}
