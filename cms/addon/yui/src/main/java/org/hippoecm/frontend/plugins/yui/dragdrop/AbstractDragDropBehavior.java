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

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.YuiHeaderContributor;
import org.hippoecm.frontend.service.render.RenderService;

public abstract class AbstractDragDropBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected IPluginConfig config;
    protected IPluginContext context;

    public AbstractDragDropBehavior(IPluginContext context, IPluginConfig config) {
        this.config = config;
        this.context = context;
    }

    @Override
    protected void onBind() {
        super.onBind();
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        YuiHeaderContributor.forModule(HippoNamespace.NS, "dragdropmanager").renderHead(response);
        TextTemplateHeaderContributor.forJavaScript(getHeaderContributorClass(), getHeaderContributorFilename(),
                new HeaderContributerModel()).renderHead(response);
        response.renderOnLoadJavascript("YAHOO.hippo.DragDropManager.onLoad()");
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

    private class HeaderContributerModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;
        private Map<String, Object> variables;

        @Override
        public Object getObject() {
            if (variables == null) {
                variables = getHeaderContributorVariables();
            }
            return variables;
        }
    }
    
    /*
    id, label, groups, callbackUrl, callbackParameters
   */
   Map<String, Object> getHeaderContributorVariables() {
       Component component = getComponent();

       Map<String, Object> variables = new MiniMap(7);
       variables.put("id", component.getMarkupId(true));
       variables.put("label", getLabel());

       String[] groups = config.getStringArray("yui.dd.groups");
       if (groups == null || groups.length == 0) {
           groups = new String[] { "default" };
       }

       StringBuilder buf = new StringBuilder(16 * groups.length);
       buf.append('[');
       for (int i = 0; i < groups.length; i++) {
           if (i > 0) {
               buf.append(',');
           }
           buf.append('\'').append(groups[i]).append('\'');
       }
       buf.append("]");
       variables.put("groups", buf.toString());

       variables.put("callbackUrl", getCallbackUrl());
       variables.put("callbackParameters", getCallbackParameters());
       variables.put("callbackFunction", getCallbackScript());
       variables.put("modelType", getModelType());

       return variables;
   }

   protected String getCallbackParameters() {
       return "[]";
   }

    
    /**
     * Specify the clientside class that is used as the DragModel, default is YAHOO.hippo.DDModel 
     */
    String getModelType() {
        return "YAHOO.hippo.DDModel";
    };

}
