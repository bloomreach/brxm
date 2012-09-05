/*
 *  Copyright 2009 Hippo.
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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog;
import org.hippoecm.frontend.service.IEditorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnpublishedReferencesDialog extends WorkflowDialog  {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UnpublishedReferencesDialog.class);

    public UnpublishedReferencesDialog(CompatibilityWorkflowPlugin.WorkflowAction base, ISortableDataProvider<Node> provider, IEditorManager mgr) {
        base.super();

        setOutputMarkupId(true);

        add(new UnpublishedReferencesView("docsview", provider, mgr));
        
        setOkLabel(new StringResourceModel("publish", this, null));
    }

    public IModel<String> getTitle() {
        return new StringResourceModel("title", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return MEDIUM;
    }

}
