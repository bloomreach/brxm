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

package org.hippoecm.frontend.plugins.xinha.dialog.browse;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.browse.AbstractBrowseView;
import org.hippoecm.frontend.plugins.xinha.dialog.JsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BrowserPlugin extends AbstractBrowserPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BrowserPlugin.class);
    
    protected final BrowseView browseView;
    protected final JcrNodeModel initialModel;
    
    public BrowserPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        JsBean bean = getBean();
        if (bean == null) {
            initialModel = null;
        } else {
            initialModel = bean.getNodeModel();
        }
        JcrNodeModel selectedNode = initialModel;
        if (initialModel == null) {
            String path = config.getString("model.folder.root", "/");
            selectedNode = new JcrNodeModel(path);
        }
        browseView = new BrowseView(context, config, selectedNode) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("dialog.list");
            }

        };
        
    }
    
    protected void onDocumentChanged(IModel model) {
        JcrNodeModel newModel = findNewModel(model);
        JsBean bean = getBean();
        if (newModel == null || bean == null) {
            return;
        }
        
        JcrNodeModel currentModel = bean.getNodeModel();
        if (initialModel == null && currentModel == null) {
            bean.setNodeModel(newModel);
            enableOk(true);
        } else if (!newModel.equals(currentModel)) {
            bean.setNodeModel(newModel);
            if (!newModel.equals(initialModel)) {
                enableOk(true);
            } else {
                enableOk(false);
            }
        } else {
            enableOk(false);
        }
    }

    protected JsBean getBean() {
        return (JsBean) getModelObject();
    }
    
    @Override
    protected void enableOk(boolean state) {
        AjaxRequestTarget.get().addComponent(ok.setEnabled(state));
    }
    
    protected abstract JcrNodeModel findNewModel(IModel model);
    
    abstract public class BrowseView extends AbstractBrowseView {
        private static final long serialVersionUID = 1L;

        protected BrowseView(IPluginContext context, IPluginConfig config, JcrNodeModel document) {
            super(context, config, document);

            context.registerService(new IModelListener() {

                public void updateModel(IModel model) {
                    onDocumentChanged(model);
                }

            }, config.getString("model.document"));
        }
    }

}
