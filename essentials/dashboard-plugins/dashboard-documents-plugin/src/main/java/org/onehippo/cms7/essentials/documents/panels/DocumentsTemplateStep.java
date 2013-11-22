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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.panels.DoubleSelectBox;
import org.onehippo.cms7.essentials.dashboard.utils.DocumentTemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers document types in CND
 *
 * @version "$Id$"
 */
public class DocumentsTemplateStep extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(DocumentsTemplateStep.class);
    final DoubleSelectBox selectBox;
    final DashboardPlugin parent;
    final List<String> items;// TODO populate
    private boolean overwrite;

    public DocumentsTemplateStep(final DashboardPlugin owner, final String title) {
        super(title);
        parent = owner;
        final Form<?> form = new Form<>("form");

        items = new ArrayList<>();
        items.add("newsdocument");
        items.add("eventsdocument");
        selectBox = new DoubleSelectBox("documentTypes", "Select document type(s)", form, items);
        //############################################
        // OVERWRITE
        //############################################
        final CheckBox overwriteCheckbox = new CheckBox("overwrite", new PropertyModel<Boolean>(this, "overwrite"));
        form.add(overwriteCheckbox);


        add(form);
    }

    public DoubleSelectBox getSelectBox() {
        return selectBox;
    }

    @Override
    public void refresh(final AjaxRequestTarget target) {
        try {
            // check which types are registered:
            final PluginContext context = parent.getContext();
            final Session session = context.getSession();
            final Workspace workspace = session.getWorkspace();
            final NodeTypeManager manager = workspace.getNodeTypeManager();
            final NodeTypeIterator nodeTypes = manager.getAllNodeTypes();
            final String prefix = context.getProjectNamespacePrefix() + ':';
            final Collection<String> documents = new HashSet<>();
            final int prefixSize = prefix.length();
            while (nodeTypes.hasNext()) {
                final NodeType nodeType = nodeTypes.nextNodeType();
                final String name = nodeType.getName();
                if (name.startsWith(prefix)) {
                    final String documentName = name.substring(prefixSize);
                    if (items.contains(documentName)) {
                        documents.add(documentName);
                    }
                }
            }
            selectBox.setLeftBoxModel(documents, target);
        } catch (RepositoryException e) {
            log.error("Error fetching registered document types", e);
        }

    }

    @Override
    public void applyState() {
        final List<String> selectedDocuments = selectBox.getSelectedRightItems();
        if (selectedDocuments != null) {
            for (String selectedDocument : selectedDocuments) {
                final String resourceName = String.format("%s%s.xml", '/', selectedDocument);
                final InputStream stream = getClass().getResourceAsStream(resourceName);
                if (stream != null) {
                    final PluginContext context = parent.getContext();

                    try {
                        String input = GlobalUtils.readStreamAsText(stream);
                        final String projectNamespacePrefix = context.getProjectNamespacePrefix();
                        input = GlobalUtils.replacePlaceholders(input, "DOCUMENT_NAME", selectedDocument);
                        input = GlobalUtils.replacePlaceholders(input, "NAMESPACE", projectNamespacePrefix);
                        DocumentTemplateUtils.importTemplate(context, input, selectedDocument, projectNamespacePrefix, overwrite);
                    } catch (RepositoryException e) {
                        GlobalUtils.refreshSession(context, false);
                        log.error(String.format("Error registering template: %s", selectedDocument), e);
                    }
                }
            }

        }
    }
}
