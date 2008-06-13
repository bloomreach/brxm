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
package org.hippoecm.frontend.plugins.console.menu.export;

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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.Model;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.w3c.dom.Document;

public class ExportDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IServiceReference<MenuPlugin> pluginRef;

    public ExportDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);
        this.pluginRef = context.getReference(plugin);

        final JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();

        DownloadExportLink link = new DownloadExportLink("download-link", nodeModel);
        link.add(new Label("download-link-text", "Download"));
        add(link);

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
                        node.getSession().exportSystemView(node.getPath(), out, true, false);
                        export = prettyPrint(out.toByteArray());
                    } catch (Exception e) {
                        export = e.getMessage();
                    }
                    dump.setModel(new Model(export));
                    target.addComponent(dump);
                }
            };
        viewLink.add(new Label("view-link-text", "View"));
        add(viewLink);

        cancel.setVisible(false);
    }

    @Override
    public void ok() {
    }

    @Override
    public void cancel() {
    }

    public String getTitle() {
        JcrNodeModel nodeModel = (JcrNodeModel) pluginRef.getService().getModel();
        String path;
        try {
            path = nodeModel.getNode().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
        }
        return "Export " + path;
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
