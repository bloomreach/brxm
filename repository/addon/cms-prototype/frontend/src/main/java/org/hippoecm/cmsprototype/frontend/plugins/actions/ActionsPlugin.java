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
package org.hippoecm.cmsprototype.frontend.plugins.actions;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.plugin.PluginManager;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * This component extends the MenuPlugin but essentially only overrides
 * the markup.
 * TODO: find a way to use alternative markup without having to extend the class
 *
 */
public class ActionsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;
    
    AjaxLink link;

    public ActionsPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        
        link = new AjaxLink("editlink", model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Plugin owningPlugin = (Plugin)findParent(Plugin.class);
                PluginManager pluginManager = owningPlugin.getPluginManager();      
                PluginEvent event = new PluginEvent(owningPlugin, JcrEvent.NEW_MODEL, owningPlugin.getNodeModel());
                pluginManager.update(target, event); 
            }
        
        };
        add(link);
        
        HippoNode node = model.getNode();
        try {
            link.setVisible( node.isNodeType(HippoNodeType.NT_DOCUMENT) 
                                && node.hasProperty("state") 
                                && node.getProperty("state").getString().equals("draft") );
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void update(AjaxRequestTarget target, PluginEvent event) {
        super.update(target, event);
        HippoNode node = getNodeModel().getNode();
        try {
            link.setVisible( node.isNodeType(HippoNodeType.NT_DOCUMENT) 
                    && node.hasProperty("state") 
                    && node.getProperty("state").getString().equals("draft") );
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (target != null && findPage() != null) {
            target.addComponent(this);
        }
    }
    
    

}
