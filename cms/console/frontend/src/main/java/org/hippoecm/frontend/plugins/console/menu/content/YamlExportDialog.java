/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.content;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamlExportDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(YamlExportDialog.class);

    private static IValueMap SIZE = new ValueMap("width=855,height=475").makeImmutable();

    public YamlExportDialog(final IModelReference<Node> modelReference) {
        final IModel<Node> nodeModel = modelReference.getModel();
        setModel(nodeModel);

        try {
            String path = nodeModel.getObject().getPath();
            add(new Label("message", new StringResourceModel("dialog.message", this).setParameters(path)));
            //info("Export content from : " + );
        } catch (RepositoryException e) {
            log.error("Error getting node from model for content export",e);
            throw new RuntimeException("Error getting node from model for content export: " + e.getMessage());
        }

        final DownloadExportYamlLink downloadPackageLink = new DownloadExportYamlLink("download-zip-link", modelReference.getModel());
        add(downloadPackageLink);

        final Label dump = new Label("dump");
        dump.setOutputMarkupId(true);
        add(dump);

        AjaxLink<String> viewLink = new AjaxLink<String>("view-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                String export;
                try {

                    final Node nodeToExport = nodeModel.getObject();
                    final ConfigurationService service = HippoServiceRegistry.getService(ConfigurationService.class);
                    export = service.exportContent(nodeToExport);

                    final JcrNodeModel newNodeModel = new JcrNodeModel(nodeToExport);
                    modelReference.setModel(newNodeModel);
                } catch (Exception e) {
                    export = e.getMessage();
                }
                dump.setDefaultModel(new Model<>(export));
                target.add(dump);
            }
        };
        add(viewLink);

        setOkVisible(false);
    }

    public IModel<String> getTitle() {
        IModel<Node> nodeModel = getModel();
        String path;
        try {
            path = nodeModel.getObject().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
        }
        return new Model<>("YAML Export " + path);
    }

    @Override
    public IValueMap getProperties() {
        return SIZE;
    }

}