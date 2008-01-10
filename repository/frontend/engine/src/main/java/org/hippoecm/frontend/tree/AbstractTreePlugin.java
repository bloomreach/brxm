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
package org.hippoecm.frontend.tree;

import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.MessageContext;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

public abstract class AbstractTreePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private JcrTree tree;
    private AbstractTreeNode rootNode;

    public AbstractTreePlugin(PluginDescriptor pluginDescriptor, AbstractTreeNode rootNode, Plugin parentPlugin) {
        super(pluginDescriptor, rootNode.getNodeModel(), parentPlugin);

        this.rootNode = rootNode;
        JcrTreeModel treeModel = new JcrTreeModel(rootNode);
        tree = new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                AbstractTreeNode treeNodeModel = (AbstractTreeNode) clickedNode;
                AbstractTreePlugin.this.onSelect(treeNodeModel, target);
            }
        };
        add(tree);
    }

    protected void onSelect(AbstractTreeNode treeNodeModel, AjaxRequestTarget target) {
        Channel channel = getDescriptor().getIncoming();
        if(channel != null) {
                // create a "select" request with the node path as a parameter
                Request request = channel.createRequest("select",
                                treeNodeModel.getNodeModel().getMapRepresentation());

                // send the request to the incoming channel
                channel.send(request);

                // add all components that have changed (and are visible!)
                request.getContext().apply(target);
        }
    }

    @Override
    public void receive(Notification notification) {
        Request request = notification.getRequest();
        
        String op = "";
        Map data;
        MessageContext context;
        
        if (request != null) {
            op = request.getOperation();
            data = request.getData();
            context = request.getContext();
        }
        else {
            op = notification.getOperation();
            data = notification.getData();
            context = notification.getContext();
        }
           
        
        if (notification != null) {
            if ("select".equals(op)) {
                JcrNodeModel model = new JcrNodeModel(data);
                
                model = findCollectionModel(model);
               
                AbstractTreeNode node = null;
                while (model != null) {
                    node = rootNode.getTreeModel().lookup(model);
                    if (node != null) {
                        tree.getTreeState().selectNode(node, true);
                        break;
                    } else {
                        model = model.getParentModel();
                    }
                }
            } else if ("flush".equals(op)) {
                AbstractTreeNode node = rootNode.getTreeModel().lookup(new JcrNodeModel(data));
                if (node != null) {
                    node.markReload();
                    node.getTreeModel().nodeStructureChanged(node);
                    context.addRefresh(tree, "updateTree");
                }
            }
        }
        super.receive(notification);
    }

    /**
     * Finds the first parent node that is a collection of handles 
     * @param nodeModel
     * @return the JcrNodeModel of the matching parent node, or null
     */
    protected JcrNodeModel findCollectionModel(JcrNodeModel nodeModel) {
        
        JcrNodeModel result = nodeModel;
        String docUUID = "";
        
        Node resultNode = result.getNode();
            
        try {
            if (resultNode.hasProperty("document")) {
                
                try {
                    docUUID = resultNode.getProperty("document").getString();
                } catch (ValueFormatException e) {
                    // docUUID property has incorrect formatting
                    result = null;
                    return result;
                }
                
                if (!"".equals(docUUID)) {
                    
                    // This is a referencing Node. Maybe a request node? Find the document the nodeModel is referring at.
    
                    javax.jcr.Session session = (javax.jcr.Session)(((UserSession)Session.get()).getJcrSession());
                    result = findCollectionModel(new JcrNodeModel(session.getNodeByUUID(docUUID)));
                    return result;
                }
            }
        } catch (RepositoryException e2) {
            return null;
        }
        
        try {
            if (resultNode.getPrimaryNodeType().isNodeType(HippoNodeType.NT_DOCUMENT)) {
                // This is a document. We need the parent (or maybe even the parent's parent).
                result = findCollectionModel(new JcrNodeModel(resultNode.getParent()));
                return result;
            }
        } catch (ItemNotFoundException e2) {
            return null;
        } catch (AccessDeniedException e2) {
            return null;
        } catch (RepositoryException e2) {
            return null;
        }
        
            try {
                if (resultNode.getPrimaryNodeType().isNodeType(HippoNodeType.NT_HANDLE)){
                    // This is a handle. We need the parent.
                    result = findCollectionModel(new JcrNodeModel(nodeModel.getNode().getParent()));
                    return result;
                }
            } catch (ItemNotFoundException e1) {
                return null;
            } catch (AccessDeniedException e1) {
                return null;
            } catch (RepositoryException e1) {
                return null;
            }
        
       return result;
    }

}
