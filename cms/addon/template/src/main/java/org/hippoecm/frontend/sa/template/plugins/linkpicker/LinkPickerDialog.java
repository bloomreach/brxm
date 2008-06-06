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
package org.hippoecm.frontend.sa.template.plugins.linkpicker;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.service.IJcrService;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerDialog extends LookupDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LinkPickerDialog.class);

    protected JcrPropertyValueModel valueModel;
    private List<String> nodetypes;
    private IJcrService jcrService;

    public LinkPickerDialog(RenderPlugin plugin, IPluginContext context, IDialogService dialogWindow,
            JcrPropertyValueModel valueModel, List<String> nodetypes) {
        super(plugin, context, dialogWindow);
        this.nodetypes = nodetypes;
        this.valueModel = valueModel;
        this.jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
        setOutputMarkupId(true);
    }

    public String getTitle() {
        return "Link picker";
    }

    @Override
    protected Panel getInfoPanel() {
        //RenderPlugin plugin = pluginRef.getService();
        //JcrPropertyValueModel nodeModel = (JcrPropertyValueModel) plugin.getModel();

        // only make it visible for allowed nodetypes
        ok.setEnabled(false);
        return new LinkPickerDialogInfoPanel("info", valueModel);
    }

    @Override
    protected boolean isValidSelection(AbstractTreeNode targetModel) {
        Node targetNode = targetModel.getNodeModel().getNode();

        boolean validType = false;
        if (nodetypes.size() == 0) {
            validType = true;
        }
        for (int i = 0; i < nodetypes.size(); i++) {
            try {
                if (targetNode.isNodeType(nodetypes.get(i))) {
                    validType = true;
                    break;
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }

        boolean isReferenceable;
        try {
            isReferenceable = targetNode.isNodeType(JcrConstants.MIX_REFERENCEABLE);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            isReferenceable = false;
        }

        return validType && isReferenceable;
    }

    @Override
    protected AbstractTreeNode getRootNode() {
        return new JcrTreeNode(new JcrNodeModel("/"));
    }

    @Override
    public void ok() throws RepositoryException {
        RenderPlugin plugin = (RenderPlugin) pluginRef.getService();
        JcrNodeModel sourceNodeModel = (JcrNodeModel) plugin.getModel();
        if (sourceNodeModel.getParentModel() != null) {
            JcrNodeModel targetNodeModel = getSelectedNode().getNodeModel();
            String targetUUID = targetNodeModel.getNode().getUUID();
            valueModel.setObject(targetUUID);            
            if (jcrService != null) {
                jcrService.flush(targetNodeModel);
            }
        }

    }

    @Override
    public void cancel() {
    }

}
