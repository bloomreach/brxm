/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.xinha.dialog;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin;

public abstract class XinhaDialogBehavior extends AbstractDefaultAjaxBehavior {
    private static final long serialVersionUID = 1L;


    private final IPluginContext context;
    private final IPluginConfig config;

    public XinhaDialogBehavior(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;
    }

    protected IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    protected Map<String, String> getParameters() {
        Request request = RequestCycle.get().getRequest();
        HashMap<String, String> parameters = new HashMap<String, String>();
        final IRequestParameters requestParameters = request.getRequestParameters();
        for (String key : requestParameters.getParameterNames()) {
            if (key.startsWith(AbstractXinhaPlugin.XINHA_PARAM_PREFIX)) {
                parameters.put(key.substring(AbstractXinhaPlugin.XINHA_PARAM_PREFIX.length()), requestParameters.getParameterValue(key).toString());
            }
        }
        return parameters;
    }

    protected IPluginContext getPluginContext() {
        return context;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

}
