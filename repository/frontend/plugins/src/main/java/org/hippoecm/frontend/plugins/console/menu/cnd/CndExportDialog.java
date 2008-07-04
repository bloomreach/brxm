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

import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndExportDialog extends AbstractDialog {
    
    static final Logger log = LoggerFactory.getLogger(CndExportDialog.class);
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id";

    private static final long serialVersionUID = 1L;

    private String selectedNs;

    public CndExportDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);
       
        final JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();
        
        List<String> nsPrefixes = null;
        try {
            nsPrefixes = Arrays.asList(nodeModel.getNode().getSession().getNamespacePrefixes());
        } catch (RepositoryException e1) {
            log.error(e1.getMessage());
        }
        
        final MultiLineLabel dump = new MultiLineLabel("dump", "");
        dump.setOutputMarkupId(true);
        add(dump);

        
        FormComponent dropdown = new DropDownChoice("nsprefixes", new PropertyModel(this, "selectedNs"), nsPrefixes) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }

            @Override
            protected void onSelectionChanged(Object newSelection) {
               selectedNs = ((String) newSelection);
            }
        }.setRequired(true);

        add(dropdown);
        
        
        AjaxLink viewLink = new AjaxLink("view-link", nodeModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    String export;
                    try {
                        Node node = nodeModel.getNode();
                        Session session = node.getSession();
                        LinkedHashSet<NodeType> types = getSortedNodeTypes(session, selectedNs.concat(":"));
                        Writer out = new JcrCompactNodeTypeDefWriter(session).write(types, true);
                        export = out.toString();
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
        return "Export CND of namespace";
    }
    
    private LinkedHashSet<NodeType> getSortedNodeTypes(Session session, String namespacePrefix) throws RepositoryException {
        NodeTypeManager ntmgr = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator it = ntmgr.getAllNodeTypes();

        LinkedHashSet<NodeType> types = new LinkedHashSet<NodeType>();

        while (it.hasNext()) {
            NodeType nt = (NodeType) it.nextNodeType();
            if (nt.getName().startsWith(namespacePrefix)) {
                types.add(nt);
            }
        }
        types = sortTypes(types);
        return types;
    }

    private LinkedHashSet<NodeType> sortTypes(LinkedHashSet<NodeType> types) {
        return new SortContext(types).sort();
    }

    class SortContext {
        HashSet<NodeType> visited;
        LinkedHashSet<NodeType> result;
        LinkedHashSet<NodeType> set;

        SortContext(LinkedHashSet<NodeType> set) {
            this.set = set;
            visited = new HashSet<NodeType>();
            result = new LinkedHashSet<NodeType>();
        }

        void visit(NodeType nt) {
            if (visited.contains(nt) || !set.contains(nt)) {
                return;
            }

            visited.add(nt);
            for (NodeType superType : nt.getSupertypes()) {
                visit(superType);
            }
            for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
                visit(nd.getDeclaringNodeType());
            }
            result.add(nt);
        }

        LinkedHashSet<NodeType> sort() {
            for (NodeType type : set) {
                visit(type);
            }
            return result;
        }
    }

    // privates

}
