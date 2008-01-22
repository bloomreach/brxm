/*
 * Copyright 2007-2008 Hippo
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
 
package org.hippoecm.cmsprototype.frontend.plugins.tasklist;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.AbstractListingPlugin;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.session.UserSession;

public class TasklistPlugin extends AbstractListingPlugin{

    private static final long serialVersionUID = 1L;
    private JcrNodeModel model;
    public static final String USER_PREF_NODENAME = "hippo:tasklist-listingview";
        
    public TasklistPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);
        this.model = (JcrNodeModel) getModel();
    }

    @Override
    protected void addTable(JcrNodeModel nodeModel, int pageSize, int viewSize) {
        javax.jcr.Session session = (javax.jcr.Session)(((UserSession)Session.get()).getJcrSession()); 
        
        dataTable = new CustomizableDocumentListingDataTable("publish", columns, 
                new SortableTaskListProvider(getTaskList(session, "//element(*, hippo:request)[@type='publish']"), session), pageSize, false);
        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        add((Component)dataTable); 

        /*ICustomizableDocumentListingDataTable deleteTable = new CustomizableDocumentListingDataTable("delete", columns, 
                new SortableTaskListProvider(getTaskList(session, "//element(*, hippo:request)[@type='delete']"), session), pageSize, false);
        deleteTable.addBottomPaging(viewSize);
        deleteTable.addTopColumnHeaders();
        add((Component)deleteTable);*/
        
}

        private QueryResult getTaskList(javax.jcr.Session session, String xpath) {
        
        QueryResult result = null;
        
        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
            result = q.execute();
        } catch (InvalidQueryException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return result;
        }

    @Override
    protected String getPluginUserPrefNodeName() {
        return USER_PREF_NODENAME;
    }

    @Override
    protected void modifyDefaultPrefNode(Node prefNode, Channel incoming) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException, ValueFormatException {
        Node pref = prefNode.addNode("name",USERSETTINGS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "DocName");
        pref.setProperty(PROPERTYNAME_PROPERTY, "documentname");

        pref = prefNode.addNode("type",USERSETTINGS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "RequestType");
        pref.setProperty(PROPERTYNAME_PROPERTY, "type");
        columns.add(getNodeColumn(new Model("DocName"), "documentname" , incoming));
        columns.add(getNodeColumn(new Model("RequestType"), "type" , incoming));
    }
    
    @Override
    protected IStyledColumn getNodeColumn(Model model, String propertyName, Channel incoming) {
        return new TasklistNodeColumn(model, propertyName, incoming);
    }
    
}
