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
package org.hippoecm.cmsprototype.frontend.plugins.todo;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

/**
 * This component extends the MenuPlugin but essentially only overrides
 * the markup.
 * TODO: find a way to use alternative markup without having to extend the class
 *
 */
public class TodoPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final String USER_PATH_PREFIX = "/hippo:configuration/hippo:users/"; 
    private static final String USER_PATH_FOLDERNAME = "hippo:dashboardview";
    private static final String USER_PATH_POSTFIX = "/hippo:dashboardview";
    
    private String path;
   
    @SuppressWarnings("unused")
    private String nodePath;
    AjaxFallbackDefaultDataTable dataTable;
    PluginDescriptor pluginDescriptor;
    
    public TodoPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        this.pluginDescriptor = pluginDescriptor;
        
        UpdateTodoList();

        SettingsForm form = new SettingsForm("settings"); 
        FeedbackPanel feedbackPanel = new FeedbackPanel("settingsfeedback"); 
        
        add(feedbackPanel);
        add(form);
		
    }

    private void UpdateTodoList() {

    	List<IStyledColumn> columns;
        
    	UserSession session = (UserSession) Session.get();

    	if (dataTable != null) {
    		if(contains(dataTable, false)) remove(dataTable);
    	}
    
		columns = new ArrayList<IStyledColumn>();
		columns.add(new NodeColumn(new Model("Name"), "name", "name",  pluginDescriptor.getIncoming()));
		columns.add(new NodeColumn(new Model("Action"), "action", "action",  pluginDescriptor.getIncoming()));
		columns.add(new NodeColumn(new Model("Requester"), "requester", "requester",  pluginDescriptor.getIncoming()));
		columns.add(new NodeColumn(new Model("Document"), "document", "document",  pluginDescriptor.getIncoming()));
		columns.add(new NodeColumn(new Model("Reason"), "reason", "reason",  pluginDescriptor.getIncoming()));
    	
    	try {
			
			Node nodePath = (Node) session.getJcrSession().getItem(USER_PATH_PREFIX + session.getJcrSession().getUserID() + USER_PATH_POSTFIX);
			path = nodePath.getProperty("hippo:path").getString();
			Node todolistNode = (Node) session.getJcrSession().getItem(path);
						
		    dataTable = new AjaxFallbackDefaultDataTable("table", columns, new SortableTaskProvider(new JcrNodeModel(todolistNode)), 2);

		} catch (PathNotFoundException e) {

		    dataTable = new AjaxFallbackDefaultDataTable("table", columns, new SortableTaskProvider(null), 10);

		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    dataTable = new AjaxFallbackDefaultDataTable("table", columns, new SortableTaskProvider(null), 10);
		}
		
	    add(dataTable);

    }
    
    
    private final class SettingsForm extends Form {
        private static final long serialVersionUID = 1L;
        
        private ValueMap properties;
        transient private AjaxRequestTarget target;

        public SettingsForm(String id) {
            super(id);
            properties = new ValueMap();
            TextField name = new TextField("path", new PropertyModel(properties, "path"));
            name.setRequired(true);
            name.setModelValue(new String[] {path});
            add(name);
            
            add(new Button("submit", new Model("Save path")));
        
        }

        public void setTarget(AjaxRequestTarget target) {
            this.target = target;
        }
        
        @Override
        protected void onSubmit() {

        	UserSession session = (UserSession) Session.get();
    	
        	javax.jcr.Session jcrSession = session.getJcrSession();
    	
        	try {
				if(jcrSession.itemExists(properties.getString("path"))) {

					if(!jcrSession.itemExists(USER_PATH_PREFIX + session.getJcrSession().getUserID() + USER_PATH_POSTFIX)) {
						// User doesn't have a user folder yet
						
						Node userNode = (Node) jcrSession.getItem(USER_PATH_PREFIX + session.getJcrSession().getUserID());
						userNode.addNode(USER_PATH_FOLDERNAME, "hippo:usersettings");
					}
					
					Node nodePath = (Node) jcrSession.getItem(USER_PATH_PREFIX + session.getJcrSession().getUserID() + USER_PATH_POSTFIX);
		        	nodePath.setProperty("hippo:path", properties.getString("path"));
		        	jcrSession.save();
		        	this.warn("Path is stored in your personal settings");
		        	UpdateTodoList();
				}
		        	else
		        {
		        	// folder doens't exist
		        		this.warn("Sorry, this path doesn't exist!");
				}
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
       	        	
            //properties.clear();
        }
            
    }
   
}

   