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
package org.hippoecm.cmsprototype.frontend.plugins.addnew;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class AddNewWizard extends Plugin {
    private static final long serialVersionUID = 1L;
    
    public AddNewWizard(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        add(new AddNewForm("addNewForm"));
        add(new FeedbackPanel("feedback"));
    }
    
    private final class AddNewForm extends Form {
        private static final long serialVersionUID = 1L;
        
        private ValueMap properties;
        
        public AddNewForm(String id) {
            super(id);
            properties = new ValueMap();
            TextField name = new TextField("name", new PropertyModel(properties, "name"));
            name.setRequired(true);
            add(name);
            add(new Button("submit", new Model("Add")));
        }

        @Override
        protected void onSubmit() {
            UserSession session = (UserSession) getSession();
            HippoNode rootNode = session.getRootNode();
            try {
                Node handle = rootNode.addNode((String)properties.get("name"), HippoNodeType.NT_HANDLE);
                handle.addNode((String)properties.get("name"), HippoNodeType.NT_DOCUMENT);

                properties.clear();
                
                error("Document created");
                
                // TODO pluginManager.update
                //PluginManager pluginManager = getPluginManager();      
                //pluginManager.update(target, dialogResult);
            
            }
            catch (RepositoryException e) {
                e.printStackTrace();
                error(e);
            }
            
        }
        
        
        
    }

}
