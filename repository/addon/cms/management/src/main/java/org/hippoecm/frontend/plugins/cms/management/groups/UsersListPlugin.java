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
package org.hippoecm.frontend.plugins.cms.management.groups;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.template.model.ItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.cms.management.FlushableListingPlugin;
import org.hippoecm.frontend.plugins.cms.management.SortableNodesDataProvider;
import org.hippoecm.frontend.plugins.cms.management.users.GroupsListPlugin;
import org.hippoecm.frontend.plugins.yui.dragdrop.node.DropNodeBehavior;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersListPlugin extends FlushableListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UsersListPlugin.class);

    private JcrNodeModel rootModel;

    public UsersListPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        ItemModel itemModel = (ItemModel) model;
        rootModel = itemModel.getNodeModel();

        String caption = pluginDescriptor.getParameter("caption").getStrings().get(0);
        add(new Label("listLabel", new Model(caption)));

        if (!rootModel.getNode().isNew())
            add(new DropNodeBehavior());
        add(new SimpleAttributeModifier("class", "userGroupsList"));
    }

    @Override
    protected SortableDataProvider createDataProvider() {
        return new SortableNodesDataProvider("name") {

            @Override
            protected List<JcrNodeModel> createNodes() {
                List<JcrNodeModel> list = new ArrayList<JcrNodeModel>();
                String usersPath = "/hippo:configuration/hippo:users/";

                //this method is called by super constructor so work around
                HippoNode groupNode = null;
                if (rootModel == null) {//our own constructor hasn't finished yet
                    ItemModel itemModel = (ItemModel) getModel();
                    groupNode = itemModel.getNodeModel().getNode();
                } else {
                    groupNode = rootModel.getNode();
                }
                try {
                    if (groupNode.hasProperty("hippo:members")) {
                        Property property = groupNode.getProperty("hippo:members");
                        Value[] values = property.getValues();
                        for (Value value : values) {
                            list.add(new JcrNodeModel(usersPath + value.getString()));
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("Error while getting hippo:members property from group["
                            + rootModel.getItemModel().getPath() + "]", e);
                }
                return list;
            }
        };
    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return "USER-PREF-GROUP-USER-MEMBERS-LIST";
    }

    @Override
    public void receive(Notification notification) {
        if (notification.getOperation().equals("drop")) {
            String targetId = (String) notification.getModel().getMapRepresentation().get("targetId");
            if (targetId.equals(getMarkupId())) {
                //Is this the best way?
                HippoNode groupNode = rootModel.getNode();
                String userPath = (String) notification.getModel().getMapRepresentation().get("node");
                try {
                    String username = new JcrNodeModel(userPath).getNode().getName();

                    GroupsListPlugin.addMultiValueProperty(groupNode, "hippo:members", username);
                    if (groupNode.pendingChanges().hasNext()) {
                        groupNode.save();
                        flushDataProvider();
                        notification.getContext().addRefresh(UsersListPlugin.this);
                    }
                } catch (RepositoryException e) {
                    log.error("Error while trying to add user[" + userPath + "] to group["
                            + rootModel.getItemModel().getPath() + "]", e);
                }
            }
        } else if (notification.getOperation().equals("flush")) {

            if (!rootModel.getNode().isNew() && getBehaviors(DropNodeBehavior.class).size() == 0) {
                add(new DropNodeBehavior());
                notification.getContext().addRefresh(UsersListPlugin.this);
            }
        }

        super.receive(notification);
    }

    @Override
    protected void modifyDefaultPrefNode(Node prefNode, Channel channel) throws ItemExistsException,
            PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException,
            ConstraintViolationException, RepositoryException, ValueFormatException {

        Node pref = prefNode.addNode("name", LISTINGPROPS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "Name");
        pref.setProperty(PROPERTYNAME_PROPERTY, "name");
        columns.add(getNodeColumn(new Model("Name"), "name", channel));
    }

}
