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

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.management.FlushableListingPlugin;
import org.hippoecm.frontend.plugins.cms.management.users.GroupsListPlugin;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameResolver;
import org.hippoecm.frontend.plugins.yui.dragdrop.DropBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersListPlugin extends FlushableListingPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UsersListPlugin.class);

    public UsersListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String caption = config.getString("caption");
        add(new Label("listLabel", new Model(caption)));
        
        JcrNodeModel rootModel = (JcrNodeModel) getModel();
        if (!rootModel.getNode().isNew()) {
            add(new GroupDropBehavior(context, config));
        }
        add(new SimpleAttributeModifier("class", "userGroupsList"));
        
        onModelChanged();
    }

    @Override
    protected IDataProvider createDataProvider() {
        final List<JcrNodeModel> list = new ArrayList<JcrNodeModel>();
        final String usersPath = "/hippo:configuration/hippo:users/";
        
        JcrNodeModel rootModel = (JcrNodeModel) getModel();
        try {
            if (rootModel.getNode().hasProperty("hippo:members")) {
                Property property = rootModel.getNode().getProperty("hippo:members");
                Value[] values = property.getValues();
                for (Value value : values) {
                    list.add(new JcrNodeModel(usersPath + value.getString()));
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while getting hippo:members property from group["
                    + rootModel.getItemModel().getPath() + "]", e);
        }
        return new ListDataProvider(list) {
            private static final long serialVersionUID = 1L;

            @Override
            public void detach() {
                for (IModel model : list) {
                    model.detach();
                }
                super.detach();
            }
        };
    }
    
    @Override
    protected List<IStyledColumn> createTableColumns() {
        List<IStyledColumn> columns = new ArrayList<IStyledColumn>();
        columns.add(getNodeColumn(new Model("Name"), "name", new NameResolver()));
        return columns;
    }
    
    
    // implements IJcrNodeModelListener

    @Override
    public void onFlush(JcrNodeModel nodeModel) {
        JcrNodeModel rootModel = (JcrNodeModel) getModel();
        if (!rootModel.getNode().isNew() && getBehaviors(DropBehavior.class).size() == 0) {
            add(new GroupDropBehavior(getPluginContext(), getPluginConfig()));
            onModelChanged();
        }
    }
    
    private class GroupDropBehavior extends DropBehavior {
        private static final long serialVersionUID = 1L;

        public GroupDropBehavior(IPluginContext context, IPluginConfig config) {
            super(context, config);
        }

        @Override
        public void onDrop(IModel model) {
            if (model instanceof JcrNodeModel) {
                JcrNodeModel droppedModel = (JcrNodeModel) model;
                JcrNodeModel groupModel = (JcrNodeModel) getModel();
                String userPath = droppedModel.getItemModel().getPath();
                try {
                    String username = droppedModel.getNode().getName();
                    GroupsListPlugin.addMultiValueProperty(groupModel.getNode(), "hippo:members", username);
                    if (groupModel.getNode().pendingChanges().hasNext()) {
                        groupModel.getNode().save();
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
