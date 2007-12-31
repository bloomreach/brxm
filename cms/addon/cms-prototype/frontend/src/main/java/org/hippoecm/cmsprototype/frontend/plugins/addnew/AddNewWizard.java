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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.MessageContext;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form to create new documents.
 * It gets the available document templates from the configuration in the repository.
 *
 */
public class AddNewWizard extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AddNewWizard.class);

    public AddNewWizard(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        final AddNewForm form = new AddNewForm("addNewForm"); 
        add(new FeedbackPanel("feedback"));

        form.add(new AjaxEventBehavior("onsubmit") {
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                // FIXME save ajax request target so it can be used in onSubmit()
                form.setTarget(target);
            }
            
        });
    
        add(form);
    
    }
    
    private final class AddNewForm extends Form {
        private static final long serialVersionUID = 1L;
        
        private ValueMap properties;
        transient private AjaxRequestTarget target;

        public AddNewForm(String id) {
            super(id);
            properties = new ValueMap();
            TextField name = new TextField("name", new PropertyModel(properties, "name"));
            name.setRequired(true);
            add(name);
            
            List<String> templates = getTemplates();
            DropDownChoice template = new DropDownChoice("template", new PropertyModel(properties, "template"), templates);
            template.setRequired(true);
            add(template);
            
            add(new Button("submit", new Model("Add")));
            
        }
        
        public void setTarget(AjaxRequestTarget target) {
            this.target = target;
        }
        
        @Override
        protected void onSubmit() {
            Node doc = createDocument();
            
            if (doc != null && target != null) {
            	Channel channel = getDescriptor().getIncoming();
            	if(channel != null) {
	                // FIXME target is now available so an update event can be sent but how to get the correct JcrNodeModel?
	                
	                JcrNodeModel model = new JcrNodeModel(doc); // who is my parent??
	                Request request = channel.createRequest("select", model.getMapRepresentation());
	                channel.send(request);
	                MessageContext context = request.getContext();

	                request = channel.createRequest("flush", getNodeModel().findRootModel().getMapRepresentation());
	                request.setContext(context);
	                channel.send(request);

                    request = channel.createRequest("edit", model.getMapRepresentation());
                    request.setContext(context);
                    channel.send(request);
	                context.apply(target);
            	}                
            }
            
            properties.clear();
            
        }
        
        private List<String> getTemplates() {
            List<String> templates = new ArrayList<String>();
            UserSession session = (UserSession) Session.get();

            try {
                Node rootNode = session.getRootNode();
                String path = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH 
                                + "/hippo:cms-prototype/hippo:templates";
                if (rootNode.hasNode(path)) {
                    Node configNode = rootNode.getNode(path);
                    NodeIterator iterator = configNode.getNodes();
                    while (iterator.hasNext()) {
                        templates.add(iterator.nextNode().getName());
                    }
                }
            } 
            catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            
            return templates;
        }

        private Node createDocument() {
            UserSession session = (UserSession) Session.get();
            Node result = null;

            try {
                Node rootNode = session.getRootNode();
                Node typeNode;
                if (rootNode.hasNode((String)properties.get("template"))) {
                    typeNode = rootNode.getNode((String)properties.get("template"));
                }
                else {
                    typeNode = rootNode.addNode((String)properties.get("template"), "nt:unstructured");
                }
                Node handle = typeNode.addNode((String)properties.get("name"), HippoNodeType.NT_HANDLE);
                Node doc = handle.addNode((String)properties.get("name"), (String)properties.get("template"));
                doc.setProperty("state", "unpublished");
                result = doc;
                
                String path = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH 
                                + "/hippo:cms-prototype/hippo:templates/" + (String)properties.get("template") ;
                if (rootNode.hasNode(path)) {
                    Node configNode = rootNode.getNode(path);
                    NodeIterator iterator = configNode.getNodes();
                    while (iterator.hasNext()) {
                        Node fieldNode = iterator.nextNode();
                        
                        // TODO should be able to get a default value for field from config
                        if (fieldNode.getName().equals("state")) {
                            doc.setProperty(fieldNode.getProperty("hippo:path").getString(), "unpublished");
                        }
                        else {
                            doc.setProperty(fieldNode.getProperty("hippo:path").getString(), "");
                        }
                    }
                }
            } 
            catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            
            return result;
        }
        
        
    }

}
