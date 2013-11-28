/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;

    private final WorkflowPlugin plugin;

    public WorkflowDialog(WorkflowPlugin plugin) {
        this.plugin = plugin;

        final IModel<Node> nodeModel = (IModel<Node>)plugin.getDefaultModel();

        final MultiLineLabel dump = new MultiLineLabel("dump", "");
        dump.setOutputMarkupId(true);
        add(dump);

        try {
            Node subject = nodeModel.getObject();
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            PrintWriter out = new PrintWriter(ostream);
            Session session = subject.getSession();
            WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            Node categories = session.getRootNode().getNode("hippo:configuration/hippo:workflows");

            for (NodeIterator iter = categories.getNodes(); iter.hasNext();) {
                Node category = iter.nextNode();
                WorkflowDescriptor descriptor = workflowManager.getWorkflowDescriptor(category.getName(), subject);
                out.println("Category " + category.getName() + ": "
                        + (descriptor != null ? descriptor.getDisplayName() : "none"));
            }
            out.flush();
            dump.setDefaultModel(new Model<String>(new String(ostream.toByteArray())));
        } catch(RepositoryException ex) {
            dump.setDefaultModel(new Model<String>(ex.getClass().getName()+": "+ex.getMessage()));
        }

        setOkVisible(false);
        setFocusOnOk();
    }

    @Override
    public void onOk() {
    }

    @Override
    public void onCancel() {
    }

    public IModel<String> getTitle() {
        IModel<Node> nodeModel = (IModel<Node>)plugin.getDefaultModel();
        String path;
        try {
            path = nodeModel.getObject().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
        }
        return new Model<String>("Workflow for " + path);
    }
}
