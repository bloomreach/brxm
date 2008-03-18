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
package org.hippoecm.cmsprototype.frontend.plugins.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.cmsprototype.frontend.plugins.tabs.TabsPlugin.Tab;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnCloseDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(OnCloseDialog.class);

    private String onclosemessage = "You have possibly changes in this node or child nodes.";

    protected AjaxLink donothing;
    protected AjaxLink discard;
    protected AjaxLink save;
    private ArrayList<JcrNodeModel> jcrNewNodeModelList;
    private Map<JcrNodeModel, String> editors;

    public OnCloseDialog(DialogWindow dialogWindow, final ArrayList<JcrNodeModel> jcrNewNodeModelList,
            Map<JcrNodeModel, Tab> editors) {
        super(dialogWindow);

        this.ok.setVisible(false);
        this.cancel.setVisible(false);

        setOutputMarkupId(true);

        this.jcrNewNodeModelList = jcrNewNodeModelList;

        // convert editors; we cannot keep references to Tab instances, as these are serialized as
        // part of a different page.
        this.editors = new HashMap<JcrNodeModel, String>();
        for (Map.Entry<JcrNodeModel, Tab> entry : editors.entrySet()) {
            Tab tabbie = entry.getValue();
            String pluginPath = tabbie.getPlugin().getPluginPath();
            this.editors.put(entry.getKey(), pluginPath);
        }

        try {
            JcrNodeModel closedJcrNodeModel = dialogWindow.getNodeModel();
            dialogWindow.setTitle("Close " + closedJcrNodeModel.getNode().getName());
        } catch (RepositoryException e) {
            dialogWindow.setTitle("Close ");
            log.error(e.getMessage());
        }

        final Label onCloseMessageLabel = new Label("onclosemessage", onclosemessage);
        onCloseMessageLabel.setOutputMarkupId(true);
        add(onCloseMessageLabel);

        final Label exceptionLabel = new Label("onCloseDialogException", "");
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        save = new AjaxLink("save") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                save();
                getDialogWindow().close(target);
            }
        };
        add(save);

        discard = new AjaxLink("discard") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                discard();
                getDialogWindow().close(target);
            }
        };
        add(discard);

        donothing = new AjaxLink("donothing") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                donothing();
                getDialogWindow().close(target);
            }
        };
        add(donothing);
    }

    protected void save() {
        try {
            JcrNodeModel nodeModel = getDialogWindow().getNodeModel();
            Node n = nodeModel.getNode();

            while (n.isNew()) {
                n = n.getParent();
            }
            n.save();

            sendSave(new JcrNodeModel(n));

            sendClose(nodeModel);
        } catch (RepositoryException e) {
            log.info(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    protected void discard() {
        try {
            Node n = getDialogWindow().getNodeModel().getNode();

            if (n.isNew()) {
                n.remove();
            } else {
                String parentPath;
                parentPath = getDialogWindow().getNodeModel().getNode().getPath();
                for (int i = 0; i < jcrNewNodeModelList.size(); i++) {
                    JcrNodeModel model = jcrNewNodeModelList.get(i);
                    if (model.getNode().getPath().startsWith(parentPath)) {
                        sendClose(model);
                    }
                }
                n.refresh(false);
            }

            sendClose(getDialogWindow().getNodeModel());
        } catch (RepositoryException e) {
            log.info(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    protected void donothing() {
    }

    @Override
    protected void ok() throws Exception {
    }

    @Override
    protected void cancel() {
    }

    private void sendSave(JcrNodeModel nodeModel) {
        Channel channel = getChannel();
        if (channel != null) {
            Request request = channel.createRequest("save", nodeModel);
            channel.send(request);
        }
    }
    
    private void sendClose(JcrNodeModel nodeModel) {
        Channel channel = getChannel();
        if (channel != null) {
            PluginModel requestModel = new PluginModel();
            requestModel.put("plugin", editors.get(nodeModel));
            Request request = channel.createRequest("close", requestModel);
            channel.send(request);
        }
    }
}
