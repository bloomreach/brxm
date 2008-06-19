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
package org.hippoecm.frontend.plugins.yui.sa.dragdrop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.logger.YuiLogBehavior;
import org.hippoecm.frontend.service.render.RenderService;
import org.wicketstuff.yui.YuiHeaderContributor;

public abstract class AbstractDragDropBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private boolean debug;

    protected IPluginConfig config;
    protected IPluginContext context;

    public AbstractDragDropBehavior(IPluginContext context, IPluginConfig config) {
        this.config = config;
        this.context = context;
    }

    @Override
    protected void onBind() {
        super.onBind();
        Component component = getComponent();
        if (debug) {
            component.add(new YuiLogBehavior());
        }

        TextTemplateHeaderContributor headerContributor = TextTemplateHeaderContributor.forJavaScript(getHeaderContributorClass(),
                getHeaderContributorFilename(), new HeaderContributerModel());
        component.add(headerContributor);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        YuiHeaderContributor.forModule("dragdrop", null, debug).renderHead(response);
        response.renderJavascriptReference(new JavascriptResourceReference(AbstractDragDropBehavior.class, "DDModel.js"));
        super.renderHead(response);
    }

    @Override
    protected CharSequence getCallbackScript(boolean onlyTargetActivePage) {
        StringBuffer buf = new StringBuffer()
        .append("var callbackUrl = '").append(getCallbackUrl(onlyTargetActivePage)).append("';\n")
        .append("for(i=0;i<callbackParameters.length;i++) {\n")
        .append("  var paramKey=callbackParameters[i].key, paramValue=Wicket.Form.encode(callbackParameters[i].value);\n")
        .append("  callbackUrl += (callbackUrl.indexOf('?') > -1) ? '&' : '?';\n")
        .append("  callbackUrl += (paramKey + '=' + paramValue);\n")
        .append("}\n")
        .append("    ")
        .append(generateCallbackScript("wicketAjaxGet(callbackUrl"));
        return buf.toString();
    }

    protected abstract String getHeaderContributorFilename();
    
    protected abstract Class<? extends IBehavior> getHeaderContributorClass();

    public IBehavior setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean getDebug() {
        return debug;
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

        private Map<String, Object> getHeaderContributorVariables() {
            Component component = getComponent();

            Map<String, Object> variables = new MiniMap(4);
            variables.put("id", component.getMarkupId(true));
            variables.put("callbackScript", getCallbackScript());
            variables.put("label", getLabel());

            String[] groups = config.getStringArray("yui.dd.groups");
            if (groups != null) {
                List<String> groupNames = Arrays.asList(groups);
                String group = groupNames.size() > 0 ? groupNames.get(0) : "";
                variables.put("group", group);
                if (groupNames.size() > 1) {
                    StringBuilder buf = new StringBuilder(16 * groupNames.size());
                    buf.append('[');
                    for (int i = 1; i < groupNames.size(); i++) {
                        if (i > 1) {
                            buf.append(',');
                        }
                        buf.append('\'').append(groupNames.get(i)).append('\'');
                    }
                    buf.append("];");
                    variables.put("moreGroups", buf.toString());
                } else {
                    variables.put("moreGroups", "null");
                }
            } else {
                variables.put("group", "default");
            }

            return variables;
        }

        private String getLabel() {
            String pluginModelId = config.getString(RenderService.MODEL_ID);
            if (pluginModelId != null) {
                IModelService pluginModelService = context.getService(pluginModelId, IModelService.class);
                if (pluginModelService != null) {
                    IModel draggedModel = pluginModelService.getModel();
                    if (draggedModel instanceof JcrNodeModel) {
                        JcrNodeModel nodeModel = (JcrNodeModel)draggedModel;
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
    };

}
