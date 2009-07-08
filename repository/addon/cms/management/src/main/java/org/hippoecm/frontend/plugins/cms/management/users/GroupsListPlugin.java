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
package org.hippoecm.frontend.plugins.cms.management.users;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.management.AbstractManagementListingPlugin;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragDropSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.DropBehavior;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupsListPlugin extends AbstractManagementListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(GroupsListPlugin.class);

    private String username;

    public GroupsListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String caption = config.getString("caption");
        add(new Label("listLabel", new Model(caption)));

        add(new UserDropBehavior(context, config));
        add(new SimpleAttributeModifier("class", "userGroupsList"));

        onModelChanged();
    }

    @Override
    protected List<IModel> getRows() {
        String queryString = "//element(*, hipposys:group)[jcr:contains(@hipposys:members, '" + getUsername() + "')]";
        String queryType = "xpath";
        final List<IModel> list = new ArrayList<IModel>();
        try {
            QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
            HippoQuery query = (HippoQuery) queryManager.createQuery(queryString, queryType);
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            session.refresh(true);
            QueryResult result;
            result = query.execute();

            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                JcrNodeModel modcheck = new JcrNodeModel(it.nextNode());
                list.add(modcheck);
            }
        } catch (RepositoryException e) {
            log.error("Error executing query[" + queryString + "]", e);
        }
        return list;
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

    public static Node addMultiValueProperty(Node node, String propertyName, String propertyValue)
            throws RepositoryException {
        if (!node.hasProperty(propertyName)) {
            node.setProperty(propertyName, new String[] { propertyValue });
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
                newValues[values.length] = node.getSession().getValueFactory().createValue(propertyValue);
                property.setValue(newValues);
            }
        }
        return node;
    }

    private class UserDropBehavior extends DropBehavior {
        private static final long serialVersionUID = 1L;

        public UserDropBehavior(IPluginContext context, IPluginConfig config) {
            super(YuiPluginHelper.getManager(context), new DragDropSettings(YuiPluginHelper.getConfig(config)));
        }

        @Override
        public void onDrop(IModel model, Map<String, String[]> parameters, AjaxRequestTarget target) {
            if (model instanceof JcrNodeModel) {
                JcrNodeModel droppedGroup = (JcrNodeModel) model;
                String myUsername = getUsername();
                if (myUsername != null) {
                    Node groupNode = droppedGroup.getNode();
                    try {
                        addMultiValueProperty(groupNode, "hipposys:members", myUsername);
                        javax.jcr.Session session = groupNode.getSession();
                        if (session.hasPendingChanges()) {
                            session.save();
                            onModelChanged();
                        }
                    } catch (RepositoryException e) {
                        log.error("An error occuirred while trying to add user[" + myUsername
                                + "] to hipposys:members property", e);
                    }
                }
            }
        }
    }
}
