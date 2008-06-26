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
package org.hippoecm.frontend.plugins.cms.management.sa.groups;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.management.sa.FlushableListingPlugin;
import org.hippoecm.frontend.plugins.cms.management.sa.SortableNodesDataProvider;
import org.hippoecm.frontend.plugins.cms.management.users.GroupsListPlugin;
import org.hippoecm.frontend.plugins.yui.sa.dragdrop.DropBehavior;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersListPlugin extends FlushableListingPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UsersListPlugin.class);

    private JcrNodeModel rootModel;

    public UsersListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        rootModel = (JcrNodeModel) getModel();

        String caption = config.getString("caption");
        add(new Label("listLabel", new Model(caption)));

        if (!rootModel.getNode().isNew()) {
            add(new GroupDropBehavior(context, config));
        }
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
                    groupNode = ((JcrNodeModel) getModel()).getNode();
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
    
    // implements IJcrNodeModelListener

    public void onFlush(JcrNodeModel nodeModel) {
        if (!rootModel.getNode().isNew() && getBehaviors(DropBehavior.class).size() == 0) {
            add(new GroupDropBehavior(getPluginContext(), getPluginConfig()));
            flushDataProvider();
        }
    }
    
    private class GroupDropBehavior extends DropBehavior {

        public GroupDropBehavior(IPluginContext context, IPluginConfig config) {
            super(context, config);
        }

        @Override
        public void onDrop(IModel model) {
            //Is this the best way?
            HippoNode groupNode = rootModel.getNode();
            if (model instanceof JcrNodeModel) {
                JcrNodeModel droppedModel = (JcrNodeModel) model;
                String userPath = droppedModel.getItemModel().getPath();
                try {
                    String username = droppedModel.getNode().getName();

                    GroupsListPlugin.addMultiValueProperty(groupNode, "hippo:members", username);
                    if (groupNode.pendingChanges().hasNext()) {
                        groupNode.save();
                        flushDataProvider();
                    }
                } catch (RepositoryException e) {
                    log.error("Error while trying to add user[" + userPath + "] to group["
                            + rootModel.getItemModel().getPath() + "]", e);
                }
            }
        }
        
    }

}
