/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentExportDialog extends Dialog<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ContentExportDialog.class);

    private boolean skipBinary = false;

    public ContentExportDialog(final IModelReference<Node> modelReference) {
        setSize(DialogConstants.LARGE);

        final IModel<Node> nodeModel = modelReference.getModel();
        setModel(nodeModel);

        try {
            String path = nodeModel.getObject().getPath();
            add(new Label("message", new StringResourceModel("dialog.message", this).setParameters(path)));
        } catch (RepositoryException e) {
            log.error("Error getting node from model for content export", e);
            throw new RuntimeException("Error getting node from model for content export: " + e.getMessage());
        }

        IModel<Boolean> skipBinaryModel = new PropertyModel<>(this, "skipBinary");
        AjaxCheckBox skipBinaries = new AjaxCheckBox("skip-binaries", skipBinaryModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(skipBinaries);

        DownloadExportAsPackageLink downloadPackageLink = new DownloadExportAsPackageLink("download-zip-link", modelReference.getModel());
        add(downloadPackageLink);

        DownloadExportAsFileLink downloadFileLink = new DownloadExportAsFileLink("download-xml-link", nodeModel, skipBinaryModel);
        add(downloadFileLink);

        final Label dump = new Label("dump");
        dump.setOutputMarkupId(true);
        add(dump);

        AjaxLink<String> viewLink = new AjaxLink<String>("view-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                String export;
                try {
                    SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                    stf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    stf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    stf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
                    TransformerHandler handler = stf.newTransformerHandler();
                    StringWriter exportWriter = new StringWriter();
                    Transformer transformer = handler.getTransformer();
                    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
                    handler.setResult(new StreamResult(exportWriter));
                    Node node = nodeModel.getObject();
                    ((HippoSession) node.getSession()).exportDereferencedView(node.getPath(), handler, skipBinary, false);
                    export = exportWriter.getBuffer().toString();
                    JcrNodeModel newNodeModel = new JcrNodeModel(node);
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
        return new Model<>("XML export " + path);
    }

    public boolean isSkipBinary() {
        return skipBinary;
    }

    public void setSkipBinary(boolean skipBinary) {
        this.skipBinary = skipBinary;
    }
}
