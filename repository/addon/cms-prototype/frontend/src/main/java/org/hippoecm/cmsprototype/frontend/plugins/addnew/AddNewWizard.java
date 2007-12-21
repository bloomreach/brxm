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

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
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
        AddNewForm form = new AddNewForm("addNewForm"); 
        add(new FeedbackPanel("feedback"));

        /*
        form.add(new AjaxEventBehavior("onsubmit") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                System.out.println("form submit ajax update");
//                Plugin owningPlugin = (Plugin)findParent(Plugin.class);
//                PluginManager pluginManager = owningPlugin.getPluginManager();      
//                PluginEvent event = new PluginEvent(owningPlugin, JcrEvent.NEW_MODEL, (JcrNodeModel) getModel());
//                pluginManager.update(target, event);
            }
            
        });
        */
    
        add(form);
    
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
            
            List<String> types = Arrays.asList(new String[] { "foo", "bar", "berenboot" });
            DropDownChoice template = new DropDownChoice("template", new PropertyModel(properties, "template"), types);
            template.setRequired(true);
            add(template);
            
            add(new Button("submit", new Model("Add")));
            
        }

        @Override
        protected void onSubmit() {
            UserSession session = (UserSession) getSession();
            HippoNode rootNode = session.getRootNode();
            try {
                Node typeNode = rootNode.addNode((String)properties.get("template"), "nt:unstructured");
                Node handle = typeNode.addNode((String)properties.get("name"), HippoNodeType.NT_HANDLE);
                Node doc = handle.addNode((String)properties.get("name"), HippoNodeType.NT_DOCUMENT);
                doc.setProperty("state", "unpublished");

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
