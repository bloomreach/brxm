/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.admin.menu.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.w3c.dom.Document;

public class ExportDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public ExportDialog(DialogWindow dialogWindow) {
        super(dialogWindow);

        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        String path;
        try {
            path = nodeModel.getNode().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
        }
        dialogWindow.setTitle("Export " + path);

        String export;
        try {
            Node node = nodeModel.getNode();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            node.getSession().exportSystemView(node.getPath(), out, true, false);
            export = prettyPrint(out.toByteArray());
        } catch (Exception e) {
            export = e.getMessage();
        }
        add(new MultiLineLabel("export", export));
        
        cancel.setVisible(false);
    }

    public JcrEvent ok() {
        return new JcrEvent(dialogWindow.getNodeModel());
    }

    public void cancel() {
    }

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
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(doc);
        return out.toString("UTF-8");
    }

}
