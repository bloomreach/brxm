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
package org.hippoecm.frontend.plugin.workflow;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.api.HippoNodeType;

public class EmbedWorkflowPlugin extends WorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final String ITEM_ID = "workflow.item";

    private String item;

    public EmbedWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void updateModel(IModel model) {
        JcrNodeModel nodeModel = (JcrNodeModel) model;
        Node node = nodeModel.getNode();
        try {
            Node ancestor = node.getParent();
            while (ancestor != null && !ancestor.isNodeType(HippoNodeType.NT_HANDLE) &&
                                       !ancestor.isNodeType(HippoNodeType.NT_DOCUMENT) &&
                                       !ancestor.isNodeType("rep:root")) {
                ancestor = ancestor.getParent();
            }
            if (ancestor != null && ancestor.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                item = node.getPath().substring(ancestor.getPath().length());
                if(item.startsWith("/")) // always the case except when ancestor is the root node
                    item = item.substring(1);
                super.updateModel(new JcrNodeModel(ancestor));
                return;
            }
        } catch (ItemNotFoundException ex) {
        } catch (AccessDeniedException ex) {
        } catch (RepositoryException ex) {
        }
        item = null;
        super.updateModel(new JcrNodeModel((Node) null));
    }

    @Override
    protected IPluginConfig configureWorkflow(IPluginConfig wflConfig, WorkflowsModel model) {
        wflConfig = super.configureWorkflow(wflConfig, model);
        wflConfig.remove(IBrowseService.BROWSER_ID);
        wflConfig.remove(IEditService.EDITOR_ID);
        wflConfig.put(ITEM_ID, item);
        return wflConfig;
    }
}
