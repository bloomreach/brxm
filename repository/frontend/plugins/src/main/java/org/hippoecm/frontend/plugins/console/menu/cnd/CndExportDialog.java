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

import java.io.ByteArrayOutputStream;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;

public class CndExportDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id";

    private static final long serialVersionUID = 1L;

    private IServiceReference<MenuPlugin> pluginRef;

    public CndExportDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);
        this.pluginRef = context.getReference(plugin);

        final JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();
        
      
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
                        String namespace = node.getName();
                        NodeTypeManager ntmng = node.getSession().getWorkspace().getNodeTypeManager();
                        NodeTypeIterator it = ntmng.getAllNodeTypes();
                        
                        // TODO here create the export cnd. We need to discuss how to add this:
                        // if we want JR built in CompactNodeTypeDefWriter we need to expose this 
                        // in some way in the HippoSession interface perhaps
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        
                        export = out.toString();
                        export = "ha";
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
        String path = " find chosen ns";
        
        return "Export CND of namespace: " + path;
    }

    // privates

}
