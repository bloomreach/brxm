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
import java.util.Map;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.management.AbstractManagementListingPlugin;
import org.hippoecm.frontend.plugins.cms.management.users.GroupsListPlugin;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragDropSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.DropBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersListPlugin extends AbstractManagementListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UsersListPlugin.class);

    public UsersListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String caption = config.getString("caption");
        add(new Label("listLabel", new Model(caption)));

        JcrNodeModel rootModel = (JcrNodeModel) getDefaultModel();
        if (!rootModel.getNode().isNew()) {
            add(new GroupDropBehavior(context, config));
        }
        add(new SimpleAttributeModifier("class", "userGroupsList"));

        onModelChanged();
    }

    @Override
    protected List<IModel> getRows() {
        final List<IModel> list = new ArrayList<IModel>();
        final String usersPath = "/hippo:configuration/hippo:users/";

        JcrNodeModel rootModel = (JcrNodeModel) getDefaultModel();
        try {
            if (rootModel.getNode().hasProperty("hipposys:members")) {
                Property property = rootModel.getNode().getProperty("hipposys:members");
                Value[] values = property.getValues();
                for (Value value : values) {
                    list.add(new JcrNodeModel(usersPath + value.getString()));
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while getting hipposys:members property from group[" + rootModel.getItemModel().getPath()
                    + "]", e);
        }
        return list;
    }

    private class GroupDropBehavior extends DropBehavior {
        private static final long serialVersionUID = 1L;

        public GroupDropBehavior(IPluginContext context, IPluginConfig config) {
            super(YuiPluginHelper.getManager(context), new DragDropSettings(YuiPluginHelper.getConfig(config)));
        }

        @Override
        public void onDrop(IModel model, Map<String, String[]> parameters, AjaxRequestTarget target) {
            if (model instanceof JcrNodeModel) {
                JcrNodeModel droppedModel = (JcrNodeModel) model;
                JcrNodeModel groupModel = (JcrNodeModel) getDefaultModel();
                String userPath = droppedModel.getItemModel().getPath();
                try {
                    String username = droppedModel.getNode().getName();
                    GroupsListPlugin.addMultiValueProperty(groupModel.getNode(), "hipposys:members", username);
                    javax.jcr.Session session = groupModel.getNode().getSession();
                    if (session.hasPendingChanges()) {
                        session.save();
                        onModelChanged();
                    }
                } catch (RepositoryException e) {
                    log.error("Error while trying to add user[" + userPath + "] to group["
                            + groupModel.getItemModel().getPath() + "]", e);
                }
            }
        }

    }

}
