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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerDialog extends LookupDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: LinkPickerDialog.java 12039 2008-06-13 09:27:05Z bvanhalderen $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LinkPickerDialog.class);

    protected IPluginContext context;
    protected JcrPropertyValueModel valueModel;
    private List<String> nodetypes;
    private Label label;

    public LinkPickerDialog(IPluginContext context, IDialogService dialogWindow,
            JcrPropertyValueModel valueModel, List<String> nodetypes, AbstractTreeNode rootNode) {
        super(dialogWindow, rootNode);
        this.context = context;
        this.nodetypes = nodetypes;
        this.valueModel = valueModel;

        setOutputMarkupId(true);

        String path = "no path selected";
        try {
            if (getSelectedNode() != null) {
                path = getSelectedNode().getNodeModel().getNode().getPath();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        add(label = new Label("info", new Model(path)));
        label.setOutputMarkupId(true);
        ok.setEnabled(false);
    }

    public IModel getTitle() {
        return new StringResourceModel("link-picker", this, null);
    }

    @Override
    public void onSelect(JcrNodeModel model) {
        try {
            label.setModelObject(model.getNode().getPath());
            AjaxRequestTarget target = AjaxRequestTarget.get();
            if (target != null) {
                target.addComponent(label);
                target.addComponent(ok);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
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

        boolean isLinkable;
        try {
            // do not enable linking to not referenceable nodes
            isLinkable = targetNode.isNodeType("mix:referenceable");
            // do not enable linking to hippo documents below hippo handle
            isLinkable = isLinkable
                    && !(targetNode.isNodeType(HippoNodeType.NT_DOCUMENT) && targetNode.getParent().isNodeType(
                            HippoNodeType.NT_HANDLE));
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            isLinkable = false;
        }

        return validType && isLinkable;
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel sourceNodeModel = new JcrNodeModel(valueModel.getJcrPropertymodel().getItemModel()
                .getParentModel());
        if (sourceNodeModel.getParentModel() != null) {
            JcrNodeModel targetNodeModel = getSelectedNode().getNodeModel();
            String targetUUID = targetNodeModel.getNode().getUUID();
            valueModel.setObject(targetUUID);

            IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
            if (jcrService != null) {
                jcrService.flush(sourceNodeModel);
            }
        }
    }

}
