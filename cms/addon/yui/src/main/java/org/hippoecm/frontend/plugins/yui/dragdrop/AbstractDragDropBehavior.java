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

import org.apache.wicket.behavior.IBehavior;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.JavascriptSettings;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;

public abstract class AbstractDragDropBehavior extends AbstractYuiAjaxBehavior {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected final DragDropSettings settings;

    public AbstractDragDropBehavior(IPluginContext context, IPluginConfig config, DragDropSettings settings) {
        super(context, config);
        this.settings = settings;
    }

    @Override
    public void addHeaderContribution(IYuiContext helper) {
        helper.addModule(HippoNamespace.NS, "dragdropmanager");
        
        helper.addTemplate(new HippoTextTemplate(getHeaderContributorClass(), getHeaderContributorFilename(),
                getModelClass()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public JavascriptSettings getJavascriptSettings() {
                updateSettings();
                return AbstractDragDropBehavior.this.settings;
            }

        });
        helper.addOnload("YAHOO.hippo.DragDropManager.onLoad()");
    }
    
    protected void updateSettings() {
        settings.put("callbackUrl", getCallbackUrl().toString());
        settings.put("callbackFunction", getCallbackScript().toString(), false);
        settings.put("callbackParameters", getCallbackParameters());
    }

    @Override
    protected CharSequence getCallbackScript(boolean onlyTargetActivePage) {
        StringBuilder buf = new StringBuilder();
        buf.append("function doCallBack").append(getComponent().getMarkupId(true)).append("(myCallbackUrl){ ");
        buf.append(generateCallbackScript("wicketAjaxGet(myCallbackUrl")).append(" }");
        return buf.toString();
    }

    /**
     * Provide custom callbackParameters
     * @return JavascriptObjectMap containing key/value pairs that should be used as callbackParameters
     */
    protected Map<String, String> getCallbackParameters() {
        return null;
    }
    
    abstract protected String getHeaderContributorFilename();

    abstract protected Class<? extends IBehavior> getHeaderContributorClass();

    /**
     * Specify the clientside class that is used as the DragDropModel 
     */
    abstract protected String getModelClass();

}
