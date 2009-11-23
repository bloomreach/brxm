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
package org.hippoecm.frontend.plugins.cms.browse;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.browse.AbstractBrowseView;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(BrowserPerspective.class);

    private static final long serialVersionUID = 1L;

    private AbstractBrowseView browserView;

    public BrowserPerspective(IPluginContext context, final IPluginConfig config) {
        super(context, config);
        
        String path = config.getString("model.folder.root", "/");
        browserView = new AbstractBrowseView(context, config, new JcrNodeModel(path)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("extension.list");
            }
            
            @Override
            protected void onBrowse() {
                focus(null);
            }
        };

        // register as the IRenderService for the browser service
        String browserId = config.getString("browser.id");
        context.registerService(this, browserId);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        browserView.start();
    }
    
    @Override
    protected void onDetach() {
        browserView.detach();
        super.onDetach();
    }

}
