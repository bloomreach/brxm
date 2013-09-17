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

package org.onehippo.cms7.essentials.components.gui.panel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.components.gui.ComponentsWizard;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.DocumentTemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DocumentTemplatePanel extends EssentialsWizardStep {


    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(DocumentTemplatePanel.class);
    private final ListMultipleChoice<String> availableDocuments;
    private final ComponentsWizard parent;
    private List<String> selectedDocuments;
    private List<String> items;
    private boolean overwrite;

    public DocumentTemplatePanel(final ComponentsWizard parent, final String title) {
        super(title);
        this.parent = parent;

        final Form<?> form = new Form("form");

        items = new ArrayList<>(parent.getRegisteredDocuments());

        final PropertyModel<List<String>> listModel = new PropertyModel<>(this, "selectedDocuments");
        availableDocuments = new ListMultipleChoice<>("documentTypes", listModel, items);
        availableDocuments.setOutputMarkupId(true);
        availableDocuments.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                log.debug("selectedDocuments {}", selectedDocuments);
            }
        });
        final CheckBox overwriteCheckbox = new CheckBox("overwrite", new PropertyModel<Boolean>(this, "overwrite"));
        form.add(overwriteCheckbox);
        form.add(availableDocuments);
        add(form);

    }

    @Override
    public void refresh(final AjaxRequestTarget target) {
        items.clear();
        items.addAll(parent.getRegisteredDocuments());
        final IModel<Collection<String>> model = availableDocuments.getModel();
        model.setObject(items);
        availableDocuments.modelChanged();
        target.add(availableDocuments);
    }

    @Override
    public void applyState() {
        log.info("selectedDocuments {}", selectedDocuments);
        if (selectedDocuments != null) {
            for (String selectedDocument : selectedDocuments) {
                final String resourceName = String.format("%s%s.xml", '/', selectedDocument);
                final InputStream stream = getClass().getResourceAsStream(resourceName);
                if (stream != null) {
                    final StringBuilder builder = GlobalUtils.readStreamAsText(stream);
                    final PluginContext context = parent.getContext();

                    try {
                        String input = builder.toString();
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
