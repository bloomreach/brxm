/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.documents.panels;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeExistsException;

import org.apache.wicket.markup.html.form.Form;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.panels.DoubleSelectBox;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers document types in CND
 *
 * @version "$Id$"
 */
public class DocumentsCndStep extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(DocumentsCndStep.class);
    final DoubleSelectBox selectBox;
    final DashboardPlugin parent;

    public DocumentsCndStep(final DashboardPlugin owner, final String title) {
        super(title);
        parent = owner;
        final Form<?> form = new Form<>("form");
        final List<String> items = new ArrayList<>();// TODO populate
        items.add("newsdocument");
        items.add("eventsdocument");
        selectBox = new DoubleSelectBox("documentTypes", "Select document type(s)", form, items);
        add(form);
    }

    public DoubleSelectBox getSelectBox() {
        return selectBox;
    }

    @Override
    public void applyState() {
        setComplete(false);
        final List<String> selectedDocuments = selectBox.getSelectedLeftItems();
        log.info("@INSTALLING DOCUMENTS", selectedDocuments);
        if (selectedDocuments != null && selectedDocuments.size() > 0) {
            final PluginContext context = parent.getContext();
            for (String selectedDocument : selectedDocuments) {

                final String prefix = context.getProjectNamespacePrefix();
                try {
                    final String superType = String.format("%s:basedocument", prefix);
                    log.debug("registering document: {}", selectedDocument);
                    CndUtils.registerDocumentType(context, prefix, selectedDocument, true, false, superType, "hippostd:relaxed", "hippotranslation:translated");
                    context.getSession().save();
                } catch (NodeTypeExistsException e) {
                    // just add already exiting ones:
                    GlobalUtils.refreshSession(context.getSession(), false);
                    // TODO check if we have all mixins:
                } catch (RepositoryException e) {
                    log.error(String.format("Error registering document type: %s", selectedDocument), e);
                    GlobalUtils.refreshSession(context.getSession(), false);
                }
            }

        } else {
            // skip installing documents
            log.info("No selected documents");
        }

        setComplete(true);

    }
}
