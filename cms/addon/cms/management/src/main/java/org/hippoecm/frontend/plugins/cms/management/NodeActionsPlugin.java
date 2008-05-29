package org.hippoecm.frontend.plugins.cms.management;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.repository.api.HippoNode;

public class NodeActionsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final String ACTION_OK = "ok";
    private static final String ACTION_CANCEL = "cancel";

    private static final List<String> builtin = new ArrayList<String>();
    
    static
    {
        builtin.add(ACTION_OK);
        builtin.add(ACTION_CANCEL);
    }
    
    private JcrNodeModel nodeModel;

    public NodeActionsPlugin(final PluginDescriptor pluginDescriptor, final IPluginModel model,
            final Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        String nodePath = (String) model.getMapRepresentation().get("node");
        nodeModel = new JcrNodeModel(nodePath);

        List<String> actions = new ArrayList<String>(builtin);
        for (String action : pluginDescriptor.getParameter("actions").getStrings()) {
            if (!actions.contains(action))
                actions.add(action);
        }

        final ListView actionsView = new ListView("actions", actions) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final String operation = (String) item.getModelObject();
                item.add(new AjaxLink("action") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if(builtin.contains(operation)) {
                            onBuiltinAction(target, operation);
                        } else {
                            Channel top = getTopChannel();
                            Request request = top.createRequest(operation, model);
                            top.send(request);
                            request.getContext().apply(target);
                        }
                    }
                }.add(new Label("actionLabel", new Model(operation))));

            }
        };
        add(actionsView);
    }
    
    private void onBuiltinAction(AjaxRequestTarget target, String operation) {
        if (operation.equals(ACTION_OK)) {
            try {
                Node parentNode = nodeModel.getNode().getParent();
                parentNode.save();
                Request request = getTopChannel().createRequest("flush", new JcrNodeModel(parentNode));
                getTopChannel().send(request);
                request.getContext().apply(target);
                
            } catch (AccessDeniedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ItemExistsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ConstraintViolationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvalidItemStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ReferentialIntegrityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (VersionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (LockException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchNodeTypeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ItemNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //            try {
            //                Node parentNode = newNode.getNode().getParent();
            //                parentNode.save();
            //
            //                notification.getContext().addRefresh(listContainer);
            //            } catch (PathNotFoundException e) {
            //                log.error("Node with path [" + nodePath + "] not found.", e);
            //            } catch (RepositoryException e) {
            //                log.error("Repository exception while trying to save node [" + nodePath + "]", e);
            //            }
        } else if (operation.equals(ACTION_CANCEL)) {
            HippoNode node = nodeModel.getNode();
            if (node.isNew()) {
                try {
                    Node parentNode = node.getParent();
                    node.remove();
                    Request request = getTopChannel().createRequest("flush", new JcrNodeModel(parentNode));
                    getTopChannel().send(request);
                    request.getContext().apply(target);
                } catch (VersionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (LockException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ConstraintViolationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            //            
            //            try {
            //                JcrNodeModel nodeModel = new JcrNodeModel(nodePath);
            //                HippoNode node = nodeModel.getNode();
            //                if (node.isNew()) {
            //                    node.remove();
            //                    //should this be persisted?
            //                    notification.getContext().addRefresh(listContainer);
            //                }
            //            } catch (PathNotFoundException e) {
            //                log.error("Node with path [" + nodePath + "] not found.", e);
            //            } catch (RepositoryException e) {
            //                log.error("Repository exception while trying to save node [" + nodePath + "]", e);
            //            }
        }
    }

}
