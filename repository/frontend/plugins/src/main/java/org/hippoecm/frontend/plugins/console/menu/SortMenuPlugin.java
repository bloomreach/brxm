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
package org.hippoecm.frontend.plugins.console.menu;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.sort.NodeSortPanel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortMenuPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    static final Logger log = LoggerFactory.getLogger(SortMenuPlugin.class);
    
    private NodeSortPanel sorter;

    public SortMenuPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        add(sorter = new NodeSortPanel("sorter-panel") {
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onNodeSorted(AjaxRequestTarget target) {
                try {
                    ((UserSession) Session.get()).getJcrSession().save();
                } catch (RepositoryException e) {
                    log.error("Error saving session after changing order: {}", e);
                }                
            }
        });
    }
    
    @Override
    public void onModelChanged() {
        sorter.setDefaultModel(getDefaultModel());
        redraw();
    }
}
