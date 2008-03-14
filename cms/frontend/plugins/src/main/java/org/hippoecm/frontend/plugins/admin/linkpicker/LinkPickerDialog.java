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
package org.hippoecm.frontend.plugins.admin.linkpicker;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.lookup.InfoPanel;
import org.hippoecm.frontend.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerDialog extends LookupDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LinkPickerDialog.class);

    protected JcrPropertyValueModel valueModel;
    private List<String> nodetypes;

    public LinkPickerDialog(DialogWindow dialogWindow, JcrPropertyValueModel valueModel, List<String> nodetypes) {
        super("LinkPicker", new JcrTreeNode(dialogWindow.getNodeModel().findRootModel()), dialogWindow);
        this.nodetypes = nodetypes;
        this.valueModel = valueModel;
        setOutputMarkupId(true);
    }

    @Override
    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        super.update(target, model);
        ok.setEnabled(isValidType(model));
        target.addComponent(ok);

    }

    @Override
    protected InfoPanel getInfoPanel(DialogWindow dialogWindow) {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        InfoPanel infoPanel = new LinkPickerDialogInfoPanel("info", nodeModel);
        add(infoPanel);
        // only make it visible for allowed nodetypes
        ok.setEnabled(false);
        return infoPanel;
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel sourceNodeModel = getDialogWindow().getNodeModel();
        if (sourceNodeModel.getParentModel() != null) {
            JcrNodeModel targetNodeModel = getSelectedNode().getNodeModel();
            if (isValidType(targetNodeModel)) {
                String targetPath = targetNodeModel.getNode().getPath();
                valueModel.setObject(targetPath);
            }
        }
        Channel channel = getChannel();
        if (channel != null) {
            Request request = channel.createRequest("flush", sourceNodeModel);
            channel.send(request);
        }

    }

    protected boolean isValidType(JcrNodeModel targetNodeModel) {
        Node targetNode = targetNodeModel.getNode();
        boolean validType = false;
        if (nodetypes.size() == 0) {
            return true;
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
        return validType;
    }

    @Override
    public void cancel() {
    }

}
