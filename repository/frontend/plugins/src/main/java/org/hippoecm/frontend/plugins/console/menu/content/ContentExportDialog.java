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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class ContentExportDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final Logger log = LoggerFactory.getLogger(ContentExportDialog.class);
    private static final long serialVersionUID = 1L;

    private boolean skipBinary = false;

    public ContentExportDialog(MenuPlugin plugin) {
        setModel(plugin.getDefaultModel());

        final JcrNodeModel nodeModel = (JcrNodeModel) plugin.getDefaultModel();
        try {
            String path = nodeModel.getNode().getPath();
            add(new Label("message", new StringResourceModel("dialog.message", this, null, new Object[] {path})));
            //info("Export content from : " + );
        } catch (RepositoryException e) {
            log.error("Error getting node from model for contant import",e);
            throw new RuntimeException("Error getting node from model for contant import: " + e.getMessage());
        }

        IModel skipBinaryModel = new PropertyModel(this, "skipBinary");
        CheckBox skipBinaries = new CheckBox("skip-binaries", skipBinaryModel);
        skipBinaries.add(new Label("skip-binaries-text", new Model("Do not include binary properties in export")));
        add(skipBinaries);

        DownloadExportLink link = new DownloadExportLink("download-link", nodeModel, skipBinaryModel);
        link.add(new Label("download-link-text", "Download (or right click and choose \"Save as..\""));
        add(link);
        setFocus(link);

        final MultiLineLabel dump = new MultiLineLabel("dump", "");
        dump.setOutputMarkupId(true);
        add(dump);

        AjaxLink viewLink = new AjaxLink("view-link", nodeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                String export;
                try {
                    Node node = nodeModel.getNode();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ((HippoSession) node.getSession()).exportDereferencedView(node.getPath(), out, skipBinary, false);
                    export = prettyPrint(out.toByteArray());
                } catch (Exception e) {
                    export = e.getMessage();
                }
                dump.add(new AttributeAppender("class", true, new Model("activated"), " "));
                dump.setDefaultModel(new Model(export));
                target.addComponent(dump);
            }
        };
        viewLink.add(new Label("view-link-text", "Show export in this window"));
        add(viewLink);

        setOkVisible(false);
    }

    public IModel getTitle() {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        String path;
        try {
            path = nodeModel.getNode().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
        }
        return new Model("Export " + path);
    }

    // privates

    private String prettyPrint(byte[] bytes) throws Exception {
        Source source = new StreamSource(new ByteArrayInputStream(bytes));
        DOMResult result = new DOMResult();
        TransformerFactory transformerFactory = TransformerFactory
        .newInstance();
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
