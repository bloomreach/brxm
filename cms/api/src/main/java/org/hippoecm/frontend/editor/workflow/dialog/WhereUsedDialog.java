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

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.workflow.model.ReferringDocumentsProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.IEditorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog that shows where a document is used, i.e. what other documents refer to it.
 */
public class WhereUsedDialog extends AbstractDialog {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(WhereUsedDialog.class);

    public WhereUsedDialog(WorkflowDescriptorModel model, IEditorManager editorMgr) {
        super(model);

        setOkVisible(false);
        setCancelLabel(new StringResourceModel("close", this, null));

        try {
            ReferringDocumentsProvider provider = new ReferringDocumentsProvider(new JcrNodeModel(model.getNode()), true);
            add(new ReferringDocumentsView("links", provider, editorMgr));
        } catch (RepositoryException e) {
            throw new WicketRuntimeException("No document node present", e);
        }
    }

    public IModel getTitle() {
        return new StringResourceModel("where-used", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

}
