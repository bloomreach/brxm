package org.hippoecm.frontend.plugins.cms.management;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.yui.dragdrop.node.DropNodeBehavior;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupUsersListPlugin extends QueryListPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(GroupUsersListPlugin.class);
    
    private JcrNodeModel rootModel;
    
    //TODO: can I throw a repository exception here, or should I throw an invalid arg exception?
    public GroupUsersListPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin)
            throws RepositoryException {
        super(pluginDescriptor, model, parentPlugin);
        
        ItemModel itemModel = (ItemModel) model;
        rootModel = itemModel.getNodeModel();
        
        String caption = pluginDescriptor.getParameter("caption").getStrings().get(0);
        add(new Label("listLabel", new Model(caption)));

        add(new DropNodeBehavior());
        add(new SimpleAttributeModifier("class", "userGroupsList"));
    }
    
    @Override
    protected FlushableSortableDataProvider createDataProvider() {
        return new SortableNodesDataProvider("name"){

            @Override
            protected List<JcrNodeModel> createNodes() {
                List<JcrNodeModel> list = new ArrayList<JcrNodeModel>();
                String usersPath = "/hippo:configuration/hippo:users/";
                
                //this method is called by super constructor so work around
                HippoNode groupNode = null; 
                if(rootModel == null) {//our own constructor hasn't finished yet
                    ItemModel itemModel = (ItemModel) getModel();
                    groupNode = itemModel.getNodeModel().getNode();
                } else {
                    groupNode = rootModel.getNode();
                }
                try {
                    if(groupNode.hasProperty("hippo:members")) {
                        Property property = groupNode.getProperty("hippo:members");
                        Value[] values = property.getValues();
                        for(Value value : values) {
                            list.add(new JcrNodeModel(usersPath + value.getString()));
                        }
                    }
                } catch (PathNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return list;
            }
        };
    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return "USER-PREF-GROUPS-LIST";
    }

    @Override
    public void receive(Notification notification) {
        if (notification.getOperation().equals("drop")) {
            String targetId = (String) notification.getModel().getMapRepresentation().get("targetId");
            if (targetId.equals(getMarkupId())) {
                //Is this the best way?
                HippoNode groupNode = rootModel.getNode();
                try {
                    String userPath = (String) notification.getModel().getMapRepresentation().get("node");
                    String username = new JcrNodeModel(userPath).getNode().getName();

                    UserGroupsListPlugin.addMultiValueProperty(groupNode, "hippo:members", username);
                    if (groupNode.pendingChanges().hasNext()) {
                        groupNode.save();
                        flushDataProvider();
                        notification.getContext().addRefresh(GroupUsersListPlugin.this);
                    }
                } catch (ValueFormatException e) {
                    e.printStackTrace();
                } catch (VersionException e) {
                    e.printStackTrace();
                } catch (LockException e) {
                    e.printStackTrace();
                } catch (ConstraintViolationException e) {
                    e.printStackTrace();
                } catch (PathNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
            }
        }

        super.receive(notification);
    }

}
