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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import javax.jcr.RepositoryException;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.reviewedactions.model.Revision;
import org.hippoecm.frontend.plugins.reviewedactions.model.RevisionHistory;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog that shows the revision history of a document.
 */
public class HistoryDialog extends AbstractDialog implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HistoryDialog.class);

    public HistoryDialog(WorkflowDescriptorModel<?> model, final IEditorManager editorMgr) {
        super(model);

        setOkVisible(false);
        setCancelLabel(new StringResourceModel("close", this, null));

        try {
            RevisionHistory history = new RevisionHistory(new JcrNodeModel(model.getNode()));
            add(new RevisionHistoryView("links", history) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSelect(IModel model) {
                    Revision revision = (Revision) model.getObject();
                    JcrNodeModel handleModel = revision.getHandle();
                    IEditor editor = editorMgr.getEditor(handleModel);
                    if (editor == null) {
                        try {
                            editorMgr.openPreview(handleModel);
                        } catch (ServiceException ex) {
                            log.error("Could not open editor for " + handleModel.getItemModel().getPath(), ex);
                            error("Could not open editor");
                            return;  // don't close dialog
                        }
                    }
                    closeDialog();
                }
            });
        } catch (RepositoryException e) {
            throw new WicketRuntimeException("No document node present", e);
        }
    }

    public IModel getTitle() {
        return new StringResourceModel("history", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return MEDIUM;
    }

}
