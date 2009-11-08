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
package org.hippoecm.frontend.plugins.console.menu.cnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;
import org.hippoecm.tools.projectexport.DownloadLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndExportDialog extends AbstractDialog {

    static final Logger log = LoggerFactory.getLogger(CndExportDialog.class);

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static LinkedHashSet<NodeType> sortedTypes;
    static HashSet<NodeType> visited;
    static LinkedHashSet<NodeType> result;
    static LinkedHashSet<NodeType> set;

    String selectedNs;

    public CndExportDialog(MenuPlugin plugin) {
        final JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();

        final PropertyModel selectedNsModel = new PropertyModel(this, "selectedNs");

        List<String> nsPrefixes = null;
        try {
            nsPrefixes = getNsPrefixes(nodeModel.getNode().getSession());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        // output for view
        final MultiLineLabel dump = new MultiLineLabel("dump", "") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return selectedNs != null && !getModelObjectAsString().equals("");
            }
        };
        dump.setOutputMarkupId(true);
        add(dump);

        // add dropdown for namespaces
        FormComponent dropdown = new DropDownChoice("nsprefixes", selectedNsModel, nsPrefixes) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isNullValid() {
                return false;
            }

            @Override
            public boolean isRequired() {
                return false;
            }
        };
        add(dropdown);
        dropdown.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String export;
                try {
                    Node node = nodeModel.getNode();
                    Session session = node.getSession();
                    export = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(session.getWorkspace(), selectedNs);
                } catch (RepositoryException e) {
                    log.error("RepositoryException while exporting NodeType Definitions of namespace : " + selectedNs,
                            e);
                    export = e.getMessage();
                } catch (IOException e) {
                    log.error("IOException while exporting NodeType Definitions of namespace : " + selectedNs, e);
                    export = e.getMessage();
                }
                dump.setModel(new Model(export));
                target.addComponent(CndExportDialog.this);
            }
        });

        // Add download link
        DownloadLink link = new DownloadLink("download-link") {
            private static final long serialVersionUID = 1L;

            protected String getFilename() {
                return selectedNs + ".cnd";
            }

            protected InputStream getContent() {
                String export = (String) dump.getModel().getObject();
                ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(ostream);
                writer.print(export);
                writer.flush();
                return new ByteArrayInputStream(ostream.toByteArray());
            }

            @Override
            public boolean isVisible() {
                return selectedNs != null && !dump.getModelObjectAsString().equals("");
            }
        };
        link.add(new Label("download-link-text", "Download (or right click and choose \"Save as..\""));
        link.setOutputMarkupId(true);
        add(link);
        setCancelVisible(false);
    }

    public IModel getTitle() {
        return new Model("Export CND of namespace");
    }

    private List<String> getNsPrefixes(Session session) throws RepositoryException {
        List<String> nsPrefixes = new ArrayList<String>();
        String[] ns = session.getNamespacePrefixes();
        for (int i = 0; i < ns.length; i++) {
            // filter
            if (!"".equals(ns[i])) {
                nsPrefixes.add(ns[i]);
            }
        }
        Collections.sort(nsPrefixes);
        return nsPrefixes;
    }

}
