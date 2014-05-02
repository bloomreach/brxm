/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class ContentExportDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ContentExportDialog.class);

    private boolean skipBinary = false;

    public ContentExportDialog(final IModelReference<Node> modelReference) {
        final IModel<Node> nodeModel = modelReference.getModel();
        setModel(nodeModel);

        try {
            String path = nodeModel.getObject().getPath();
            add(new Label("message", new StringResourceModel("dialog.message", this, null, null, path)));
            //info("Export content from : " + );
        } catch (RepositoryException e) {
            log.error("Error getting node from model for contant import",e);
            throw new RuntimeException("Error getting node from model for contant import: " + e.getMessage());
        }

        IModel<Boolean> skipBinaryModel = new PropertyModel<>(this, "skipBinary");
        AjaxCheckBox skipBinaries = new AjaxCheckBox("skip-binaries", skipBinaryModel) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        skipBinaries.add(new Label("skip-binaries-text", new Model<>("Do not include binary properties in export")));
        add(skipBinaries);

        DownloadExportAsPackageLink downloadPackageLink = new DownloadExportAsPackageLink("download-package-link", modelReference.getModel());
        downloadPackageLink.add(new Label("download-package-link-text", "Download as zip (or right click and choose \"Save as...\")"));
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
                    Node node = nodeModel.getObject();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ((HippoSession) node.getSession()).exportDereferencedView(node.getPath(), out, skipBinary, false);
                    export = prettyPrint(out.toByteArray());
                    JcrNodeModel newNodeModel = new JcrNodeModel(node);
                    modelReference.setModel(newNodeModel);
                } catch (Exception e) {
                    export = e.getMessage();
                }
                dump.setDefaultModel(new Model<>(export));
                target.add(dump);
            }
        };
        viewLink.add(new Label("view-link-text", "Show export in this window"));
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
        return new Model<>("XML Export " + path);
    }
    
    public boolean isSkipBinary() {
        return skipBinary;
    }
    
    public void setSkipBinary(boolean skipBinary) {
        this.skipBinary = skipBinary;
    }

    // privates

    private String prettyPrint(byte[] bytes) throws Exception {
        Source source = new StreamSource(new ByteArrayInputStream(bytes));
        DOMResult result = new DOMResult();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer identityTransformer = transformerFactory.newTransformer();
        identityTransformer.transform(source, result);
        Document doc = (Document) result.getNode();

        OutputFormat format = new OutputFormat(doc);
        format.setEncoding("UTF-8");
        format.setIndenting(true);
        format.setIndent(2);
        format.setLineWidth(80);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLSerializer xmlSerializer = new XMLSerializer(out, format);
        xmlSerializer.serialize(doc);
        return out.toString("UTF-8");
    }
}
