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

import java.util.Map;

import javax.jcr.Node;

import org.apache.wicket.Session;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugins.standardworkflow.export.CndSerializer;
import org.hippoecm.frontend.plugins.standardworkflow.export.NamespaceUpdater;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.TypeUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RemodelDialog.class);

    public RemodelDialog(DialogWindow dialogWindow) {
        super(dialogWindow, "Apply models");
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void execute() throws Exception {
        JcrSessionModel sessionModel = ((UserSession) Session.get()).getJcrSessionModel();

        Node node = getDialogWindow().getNodeModel().getNode();
        String namespace = node.getName();

        CndSerializer serializer = new CndSerializer(sessionModel, namespace);
        serializer.versionNamespace(namespace);
        String cnd = serializer.getOutput();
        System.out.println(cnd);

        NamespaceUpdater updater = new NamespaceUpdater(new RepositoryTypeConfig(RemodelWorkflow.VERSION_CURRENT),
                new RepositoryTypeConfig(RemodelWorkflow.VERSION_DRAFT));
        Map<String, TypeUpdate> update = updater.getUpdate(namespace);

        sessionModel.getSession().save();

        // log out; the session model will log in again.
        // Sessions cache path resolver information, which is incorrect after remapping the prefix.
        sessionModel.flush();

        RemodelWorkflow workflow = (RemodelWorkflow) getWorkflow();
        if (workflow != null) {
            log.info("remodelling namespace " + namespace);
            String[] nodes = workflow.remodel(cnd, update);
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
}
