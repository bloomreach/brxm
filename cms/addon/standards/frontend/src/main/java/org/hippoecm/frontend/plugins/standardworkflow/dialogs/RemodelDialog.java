/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.standardworkflow.dialogs;

import javax.jcr.Node;

import org.apache.wicket.Session;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.frontend.template.export.CndSerializer;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RemodelDialog.class);
    
    public RemodelDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        dialogWindow.setTitle("Apply models");
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void doOk() throws Exception {
        JcrSessionModel sessionModel = ((UserSession) Session.get()).getJcrSessionModel();

        Node node = dialogWindow.getNodeModel().getNode();
        String namespace = node.getName();
        TypeConfig typeConfig = getOwningPlugin().getPluginManager().getTemplateEngine().getTypeConfig();
        CndSerializer serializer = new CndSerializer(sessionModel, typeConfig, namespace);
        serializer.versionNamespace(namespace);
        String cnd = serializer.getOutput();

        // log out; the session model will log in again.
        // Sessions cache path resolver information, which is incorrect after remapping the prefix.
        sessionModel.flush();

        RemodelWorkflow workflow = (RemodelWorkflow) getWorkflow("internal");
        if (workflow != null) {
            log.info("remodelling namespace " + namespace);
            String[] nodes = workflow.remodel(cnd);
//            for(int i = 0; i < nodes.length; i++) {
//                System.err.println("nodes[] " + i + ": " + nodes[i]);
//            }
            sessionModel.getSession().save();

            // flush the root node
            Channel channel = getChannel();
            Request request = channel.createRequest("flush", new JcrNodeModel(new JcrItemModel("/")));
            channel.send(request);
        } else {
            log.warn("no remodeling workflow available on selected node");
        }
    }

    @Override
    public void cancel() {
    }

}
