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
package org.hippoecm.frontend.plugin.config.impl;

import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.WebApplicationHelper;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceFactory;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginConfigFactory implements IServiceFactory<IPluginConfigService> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(PluginConfigFactory.class);

    private IServiceFactory<IPluginConfigService> pluginConfigServiceFactory;

    public PluginConfigFactory(JcrSessionModel sessionModel, IApplicationFactory defaultFactory) {
        IApplicationFactory  builtinFactory = new BuiltinApplicationFactory();

        String appName = WebApplicationHelper.getConfigurationParameter((WebApplication) Application.get(),
                "config", null);
        Session session = sessionModel.getSession();
        ValueMap credentials = sessionModel.getCredentials();
        if (Main.DEFAULT_CREDENTIALS.equals(credentials)) {
            appName = "login";
        }

        IServiceFactory<IPluginConfigService> baseService;
        if (session == null || !session.isLive()) {
            baseService = builtinFactory.getApplication("login");
        } else if (appName == null) {
            baseService = defaultFactory.getDefaultApplication();
            if (baseService == null) {
                baseService = builtinFactory.getDefaultApplication();
            }
        } else {
            baseService = defaultFactory.getApplication(appName);
            if (baseService == null) {
                baseService = builtinFactory.getDefaultApplication();
            }
        }
        pluginConfigServiceFactory = baseService;
    }

    public IPluginConfigService getPluginConfigService(IPluginContext context) {
        return pluginConfigServiceFactory.getService(context);
    }

    public IPluginConfigService getService(IPluginContext context) {
        return pluginConfigServiceFactory.getService(context);
    }

    public Class<? extends IPluginConfigService> getServiceClass() {
        return IPluginConfigService.class;
    }

    public void releaseService(IPluginContext context, IPluginConfigService service) {
        pluginConfigServiceFactory.releaseService(context, service);
    }

}
