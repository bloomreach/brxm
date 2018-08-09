/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.onehippo.forge.demo;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.forge.dashboard.documentwizard.NewDocumentWizardPlugin;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin extends from the default document wizard plugin and adds a new field to the dialog.
 *
 */
public class DocumentChannelWizardPlugin extends NewDocumentWizardPlugin {

    private static final Logger log = LoggerFactory.getLogger(DocumentChannelWizardPlugin.class);

    public DocumentChannelWizardPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    protected NewDocumentWizardPlugin.Dialog getDialog(final IPluginContext context, final IPluginConfig config, NewDocumentWizardPlugin parent) {
        return new Dialog(context, config, parent);
    }

    public class Dialog extends org.onehippo.forge.dashboard.documentwizard.NewDocumentWizardPlugin.Dialog {

        private static final String DOCUMENTFOLDERS_ROOTPATH = "/content/documents";
        private String rootFolder;

        public Dialog(final IPluginContext context, final IPluginConfig config, final Component parent) {
            super(context, config, parent);

            // add label
            add(new Label("channel-dropdown-label", getString("channel-dropdown-label")));

            // add root folder dropdown
            rootFolder = "";
            final PropertyModel<Object> propModel = new PropertyModel<>(this, "rootFolder");
            final ValueList documentRootFolderNames = getDocumentRootFolderNames();
            final IChoiceRenderer<Object> choiceRenderer = new ListChoiceRenderer(documentRootFolderNames);
            DropDownChoice<Object> rootFoldersField = new DropDownChoice<>("rootFolders", propModel, documentRootFolderNames, choiceRenderer);
            rootFoldersField.setRequired(true);
            rootFoldersField.setLabel(new StringResourceModel("channel-dropdown-label", this));
            add(rootFoldersField);
        }

        private ValueList getDocumentRootFolderNames() {
            final ValueList rootFolderNames = new ValueList();
            final Session jcrSession = getSession().getJcrSession();
            try {
                final Node documentsNode = jcrSession.getNode(DOCUMENTFOLDERS_ROOTPATH);
                final NodeIterator rootFolderNodes = documentsNode.getNodes();
                while (rootFolderNodes.hasNext()) {
                    final Node rootFolderNode = rootFolderNodes.nextNode();
                    if(rootFolderNode.isNodeType("hippotranslation:translated")) {
                        final ListItem listItem = new ListItem(rootFolderNode.getName(),
                                JcrUtils.getStringProperty(rootFolderNode, HippoNodeType.HIPPO_NAME, rootFolderNode.getName()));
                        rootFolderNames.add(listItem);
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error occurred while retrieving root folder names: " + e.getClass().getName() + ": " + e.getMessage());
            }
            return rootFolderNames;
        }

        @Override
        protected String getFolderPath() {
            return DOCUMENTFOLDERS_ROOTPATH + "/" + rootFolder + "/" + baseFolder + list;
        }

        @Override
        protected Node processNewDocumentNode(final Node documentNode) throws RepositoryException {

            final Node docNode = super.processNewDocumentNode(documentNode);

            // as demo, set a property with a value from the UI
            docNode.setProperty("dashboarddocumentwizarddemo:rootfolder", rootFolder);
            return docNode;
        }
    }

    private static class ListChoiceRenderer implements IChoiceRenderer<Object> {
        private final ValueList list;

        public ListChoiceRenderer(ValueList list) {
            this.list = list;
        }

        public Object getDisplayValue(Object object) {
            return list.getLabel(object);
        }

        public String getIdValue(Object object, int index) {
            return list.getKey(object);
        }

        @Override
        public Object getObject(final String id, final IModel<? extends List<?>> choices) {
            return list.getListItemByKey(id);
        }
    }

}
