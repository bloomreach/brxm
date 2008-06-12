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
package org.hippoecm.frontend.plugin.workflow;


import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;


import org.apache.wicket.model.IModel;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class EmbedWorkflowPlugin extends WorkflowPlugin {

    private static final long serialVersionUID = 1L;

    public EmbedWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public void updateModel(IModel model) {
        JcrNodeModel nodeModel = (JcrNodeModel) model;
        Node node = nodeModel.getNode();
        try {
            node = node.getParent();
                while (node != null && !node.isNodeType("hippo:handle") && !node.isNodeType("hippo:document")) {
                    node = node.getParent();
                }
                if (node != null && node.isNodeType("hippo:document")) {
                    super.updateModel(new JcrNodeModel(node));
                    return;
                }
        } catch (ItemNotFoundException ex) {
        } catch (AccessDeniedException ex) {
        } catch (RepositoryException ex) {
        }
        super.updateModel(new JcrNodeModel((Node) null));
    }
}
