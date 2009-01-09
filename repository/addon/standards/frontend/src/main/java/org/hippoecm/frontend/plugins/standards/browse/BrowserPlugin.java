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

package org.hippoecm.frontend.plugins.standards.browse;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BrowserPlugin.class);
    
    protected final BrowseView browseView;
    
    public BrowserPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        browseView = new BrowseView(context, config, (JcrNodeModel) getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("dialog.list");
            }
        };
    }
 
    abstract public class BrowseView extends AbstractBrowseView {
        private static final long serialVersionUID = 1L;

        protected BrowseView(IPluginContext context, IPluginConfig config, JcrNodeModel document) {
            super(context, config, document);

            context.registerService(new IModelListener() {
                private static final long serialVersionUID = 1L;

                public void updateModel(IModel model) {
                    BrowserPlugin.this.setModel(model);
                }

            }, config.getString("model.document"));

            context.registerService(new IModelListener() {
                private static final long serialVersionUID = 1L;

                public void updateModel(IModel model) {
                    BrowserPlugin.this.setModel(model);
                }

            }, config.getString("model.folder"));
        }
    }

}
