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
package org.hippoecm.frontend.plugins.cms.management.sa.users;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.management.sa.FlushableListingPlugin;
import org.hippoecm.frontend.plugins.cms.management.sa.QueryDataProvider;
import org.hippoecm.frontend.plugins.yui.sa.dragdrop.DropBehavior;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupsListPlugin extends FlushableListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(GroupsListPlugin.class);

    private String username;

    public GroupsListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String caption = config.getString("caption");
        add(new Label("listLabel", new Model(caption)));

        add(new DropBehavior(context, config) {
            @Override
            public void onDrop(IModel model) {
                if (model instanceof JcrNodeModel) {
                    JcrNodeModel droppedGroup = (JcrNodeModel)model;
                    String myUsername = getUsername();
                    if (myUsername != null) {
                        HippoNode groupNode = droppedGroup.getNode();
                        try {
                            addMultiValueProperty(groupNode, "hippo:members", myUsername);
                            if (groupNode.pendingChanges().hasNext()) {
                                groupNode.save();
                                flushDataProvider();
                            }
                        } catch (RepositoryException e) {
                            log.error("An error occuirred while trying to add user[" + myUsername
                                    + "] to hippo:members property", e);
                        }
                    }
                }
            }
        });
        add(new SimpleAttributeModifier("class", "userGroupsList"));
    }

    @Override
    protected SortableDataProvider createDataProvider() {
        String query = "//element(*, hippo:group)[jcr:contains(@hippo:members, '" + getUsername() + "')]";
        return new QueryDataProvider(query, "xpath", "name");
    }

    private String getUsername() {
        if (username == null) {
            JcrNodeModel nodeModel = (JcrNodeModel) getModel();
            try {
                username = nodeModel.getNode().getName();
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
    protected void modifyDefaultPrefNode(Node prefNode) throws RepositoryException {
        Node pref = prefNode.addNode("name", LISTINGPROPS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "Name");
        pref.setProperty(PROPERTYNAME_PROPERTY, "name");
        columns.add(getNodeColumn(new Model("Name"), "name"));
    }

    public static HippoNode addMultiValueProperty(HippoNode node, String propertyName, String propertyValue) throws RepositoryException {
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
