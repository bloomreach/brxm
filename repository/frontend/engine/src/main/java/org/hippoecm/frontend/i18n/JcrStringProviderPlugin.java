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
package org.hippoecm.frontend.i18n;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrStringProviderPlugin implements IPlugin {
    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(JcrStringProviderPlugin.class);

    public final static String PROVIDER_ID = "provider.id";

    public JcrStringProviderPlugin(IPluginContext context, IPluginConfig config) {
        if (config.getString(PROVIDER_ID) != null) {
            IModelProvider<IModel> provider = new JcrSearchingProvider();
            context.registerService(provider, config.getString(PROVIDER_ID));
        } else {
            log.warn("No provider id (provider.id) specified.");
        }
    }

}