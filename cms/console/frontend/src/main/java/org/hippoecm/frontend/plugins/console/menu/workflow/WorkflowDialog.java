/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.workflow;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowDialog extends Dialog<Node> {

    private static final long serialVersionUID = 1L;

    private final RenderService plugin;

    public WorkflowDialog(WorkflowPlugin plugin) {
        this.plugin = plugin;
        setTitle(Model.of("Workflow for " + getNodePath()));
        
        final IModel<Node> nodeModel = (IModel<Node>) plugin.getDefaultModel();

        final MultiLineLabel dump = new MultiLineLabel("dump", "");
        dump.setOutputMarkupId(true);
        add(dump);

        try {
            final Node subject = nodeModel.getObject();
            final ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            final PrintWriter out = new PrintWriter(ostream);
            final Session session = subject.getSession();
            final WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            final Node categories = session.getRootNode().getNode("hippo:configuration/hippo:workflows");

            for (NodeIterator iter = categories.getNodes(); iter.hasNext(); ) {
                final Node category = iter.nextNode();
                final WorkflowDescriptor descriptor = workflowManager.getWorkflowDescriptor(category.getName(), subject);
                out.println("Category " + category.getName() + ": "
                        + (descriptor != null ? descriptor.getDisplayName() : "none"));
            }
            out.flush();
            dump.setDefaultModel(new Model<String>(new String(ostream.toByteArray())));
        } catch (RepositoryException ex) {
            dump.setDefaultModel(new Model<String>(ex.getClass().getName() + ": " + ex.getMessage()));
        }

        setOkVisible(false);
        setFocusOnCancel();
    }

    private String getNodePath() {
        final IModel<Node> nodeModel = (IModel<Node>) plugin.getDefaultModel();
        try {
            return nodeModel.getObject().getPath();
        } catch (RepositoryException e) {
            return e.getMessage();
        }
    }
}
