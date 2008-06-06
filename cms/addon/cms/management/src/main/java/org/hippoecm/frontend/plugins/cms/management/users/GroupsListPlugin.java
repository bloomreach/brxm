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
package org.hippoecm.frontend.plugins.cms.management.users;

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

import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugins.cms.management.FlushableListingPlugin;
import org.hippoecm.frontend.plugins.cms.management.QueryDataProvider;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.yui.dragdrop.node.DropNodeBehavior;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupsListPlugin extends FlushableListingPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(GroupsListPlugin.class);

    private String username;

    public GroupsListPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        String caption = pluginDescriptor.getParameter("caption").getStrings().get(0);
        add(new Label("listLabel", new Model(caption)));

        add(new DropNodeBehavior());
        add(new SimpleAttributeModifier("class", "userGroupsList"));
    }

    @Override
    protected SortableDataProvider createDataProvider() {
        String query = "//element(*, hippo:group)[jcr:contains(@hippo:members, '" + getUsername() + "')]";
        return new QueryDataProvider(query, "xpath", "name");
    }

    private String getUsername() {
        if (username == null) {
            ItemModel itemModel = (ItemModel) getModel();
            try {
                username = itemModel.getNodeModel().getNode().getName();
            } catch (RepositoryException e) {
                log.error("Error retrieving node name", e);
            }
        }
        return username;
    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return "USER-PREF-USER-GROUP-MEMBERS-LIST";
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

    @Override
    public void receive(Notification notification) {
        if (notification.getOperation().equals("drop")) {
            String targetId = (String) notification.getModel().getMapRepresentation().get("targetId");
            if (targetId.equals(getMarkupId())) {
                String addGroup = (String) notification.getModel().getMapRepresentation().get("node");

                String myUsername = getUsername();
                if (myUsername != null) {
                    //Is this the best way?
                    HippoNode groupNode = new JcrNodeModel(addGroup).getNode();
                    try {
                        addMultiValueProperty(groupNode, "hippo:members", myUsername);
                        if (groupNode.pendingChanges().hasNext()) {
                            groupNode.save();
                            flushDataProvider();
                            notification.getContext().addRefresh(GroupsListPlugin.this);
                        }
                    } catch (RepositoryException e) {
                        log.error("An error occuirred while trying to add user[" + myUsername
                                + "] to hippo:members property", e);
                    }
                }
            }
        }

        super.receive(notification);
    }

    public static HippoNode addMultiValueProperty(HippoNode node, String propertyName, String propertyValue)
            throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
            PathNotFoundException, IllegalStateException, RepositoryException {
        if (!node.hasProperty(propertyName)) {
            Value[] values = new Value[] { new StringValue(propertyValue) };
            node.setProperty(propertyName, values);
        } else {
            Property property = node.getProperty(propertyName);
            Value[] values = property.getValues();
            boolean found = false;
            for (Value v : values) {
                if (v.getString().equals(propertyValue)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Value[] newValues = new Value[values.length + 1];
                for (int i = 0; i < values.length; i++) {
                    newValues[i] = values[i];
                }
                newValues[values.length] = new StringValue(propertyValue);
                property.setValue(newValues);
            }
        }
        return node;
    }

}
