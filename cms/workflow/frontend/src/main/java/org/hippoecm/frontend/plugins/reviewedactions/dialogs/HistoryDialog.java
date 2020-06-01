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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.reviewedactions.model.Revision;
import org.hippoecm.frontend.plugins.reviewedactions.model.RevisionHistory;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog that shows the revision history of a document.
 */
public class HistoryDialog extends Dialog<WorkflowDescriptor> {

    private static final Logger log = LoggerFactory.getLogger(HistoryDialog.class);

    public HistoryDialog(WorkflowDescriptorModel model, final IEditorManager editorMgr) {
        super(model);

        setOkVisible(false);
        setCancelLabel(Model.of(getString("close")));

        RevisionHistory history = new RevisionHistory(model);
        add(new RevisionHistoryView("links", history) {
            @Override
            public void onSelect(IModel model) {
                Revision revision = (Revision) model.getObject();
                IModel<Node> docModel = revision.getDocument();
                IEditor editor = editorMgr.getEditor(docModel);
                if (editor == null) {
                    try {
                        editorMgr.openPreview(docModel);
                    } catch (ServiceException ex) {
                        log.error("Could not open editor for " + docModel, ex);
                        error("Could not open editor");
                        return;  // don't close dialog
                    }
                } else {
                    editor.focus();
                }
                closeDialog();
            }
        });

        add(CssClass.append("hippo-revision-history-dialog"));
    }

    public IModel<String> getTitle() {
        return Model.of(getString("history"));
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.LARGE_AUTO;
    }

}
