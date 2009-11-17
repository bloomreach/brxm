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
package org.hippoecm.frontend.plugins.yui;

import java.util.Map;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.javascript.AjaxSettings;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public abstract class AbstractYuiAjaxBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IYuiContext context;
    private AjaxSettings settings;

    public AbstractYuiAjaxBehavior(IYuiManager manager) {
        this(manager, null);
    }

    public AbstractYuiAjaxBehavior(IYuiManager manager, AjaxSettings settings) {
        if (manager == null) {
            throw new IllegalStateException("No root yui behavior found, unable to register module dependencies.");
        }
        context = manager.newContext();
        this.settings = settings;
    }

    protected void updateAjaxSettings() {
        if(settings != null) {
            settings.setCallbackUrl(getCallbackUrl().toString());
            settings.setCallbackFunction(getCallbackFunction());
            settings.setCallbackParameters(getCallbackParameters());
        }
    }

    /**
     * Wrap the callback script in an anonymous function
     */
    protected String getCallbackFunction() {
        StringBuilder buf = new StringBuilder();
        buf.append("function (myCallbackUrl) { ");
        buf.append(generateCallbackScript("wicketAjaxGet(myCallbackUrl")).append(" }");
        return buf.toString();
    }

    /**
     * Provide custom callbackParameters
     * @return JavascriptObjectMap containing key/value pairs that should be used as callbackParameters
     */
    protected Map<String, Object> getCallbackParameters() {
        return null;
    }


    @Override
    protected void onBind() {
        super.onBind();
        addHeaderContribution(context);
    }

    /**
     * Override to implement header contrib
     * @param context
     */
    public void addHeaderContribution(IYuiContext context) {
    }


    /**
     * Don't call super since WicketAjax is loaded by Yui webapp behavior
     * TODO: webapp ajax is configurable, maybe check here and still load it.
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        updateAjaxSettings();
        context.renderHead(response);
    }

}
