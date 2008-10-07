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
package org.hippoecm.frontend.plugins.yui.dragdrop;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.util.HeaderContributorHelper;
import org.hippoecm.frontend.plugins.yui.util.HeaderContributorHelper.JsConfig;
import org.hippoecm.frontend.service.render.RenderService;

public abstract class AbstractDragDropBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected IPluginConfig config;
    protected IPluginContext context;
    private HeaderContributorHelper headerContribHelper = new HeaderContributorHelper();

    public AbstractDragDropBehavior(IPluginContext context, IPluginConfig config) {
        this.config = config;
        this.context = context;

        headerContribHelper.addModule(HippoNamespace.NS, "dragdropmanager");

        headerContribHelper.addTemplate(headerContribHelper.new HippoTemplate(getHeaderContributorClass(),
                getHeaderContributorFilename(), getModelClass()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public JsConfig getJsConfig() {
                return AbstractDragDropBehavior.this.getJavacriptConfig();
            }

        });
        headerContribHelper.addOnload("YAHOO.hippo.DragDropManager.onLoad()");
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        headerContribHelper.renderHead(response);
        super.renderHead(response);
    }

    @Override
    protected CharSequence getCallbackScript(boolean onlyTargetActivePage) {
        StringBuffer buf = new StringBuffer();
        buf.append("function doCallBack").append(getComponent().getMarkupId(true)).append("(myCallbackUrl){ ");
        buf.append(generateCallbackScript("wicketAjaxGet(myCallbackUrl")).append(" }");
        return buf.toString();
    }

    protected abstract String getHeaderContributorFilename();

    protected abstract Class<? extends IBehavior> getHeaderContributorClass();

    /**
     * Specify the clientside class that is used as the DragDropModel 
     */
    abstract protected String getModelClass();

    protected String getLabel() {
        String pluginModelId = config.getString(RenderService.MODEL_ID);
        if (pluginModelId != null) {
            IModelService pluginModelService = context.getService(pluginModelId, IModelService.class);
            if (pluginModelService != null) {
                IModel draggedModel = pluginModelService.getModel();
                if (draggedModel instanceof JcrNodeModel) {
                    JcrNodeModel nodeModel = (JcrNodeModel) draggedModel;
                    try {
                        return nodeModel.getNode().getDisplayName();
                    } catch (RepositoryException e) {
                        return getComponent().getMarkupId();
                    }
                }
            }
        }
        return getComponent().getMarkupId();
    }

    protected JsConfig getJavacriptConfig() {
        JsConfig jsConfig = new JsConfig();
        jsConfig.put("label", getLabel());
        jsConfig.put("groups", config.getStringArray("yui.dd.groups"));
        jsConfig.put("callbackUrl", getCallbackUrl().toString());
        jsConfig.put("callbackFunction", getCallbackScript().toString(), false);
        jsConfig.put("callbackParameters", getCallbackParameters());
        return jsConfig;
    }

    /**
     * Provide custom callbackParameters
     * @return JavascriptObjectMap containing key/value pairs that should be used as callbackParameters
     */
    protected JsConfig getCallbackParameters() {
        return null;
    }

}
