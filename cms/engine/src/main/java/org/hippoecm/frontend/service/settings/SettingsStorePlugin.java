/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.service.settings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.preferences.*;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsStorePlugin extends Plugin implements ISettingsService {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")


    static final Logger log = LoggerFactory.getLogger(SettingsStorePlugin.class);

    transient StringCodecFactory stringCodecFactory;

    public SettingsStorePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, ISettingsService.SERVICE_ID);
    }

    public StringCodecFactory getStringCodecFactory() {
        if (stringCodecFactory == null) {
            IPluginConfig codecsConfig = getPluginConfig().getPluginConfig("codecs");
            Map<String, StringCodec> codecs = new HashMap<String, StringCodec>();
            for (String codecName : codecsConfig.keySet()) {
                try {
                    String className = codecsConfig.getString(codecName);
                    Class<? extends StringCodec> clazz = Thread.currentThread().getContextClassLoader().loadClass(className).asSubclass(StringCodec.class);
                    codecs.put(codecName, clazz.newInstance());
                } catch (InstantiationException ex) {
                    log.error("unable to create " + codecName, ex);
                } catch (IllegalAccessException ex) {
                    log.error("unable to create " + codecName, ex);
                } catch (ClassCastException ex) {
                    log.error("1`unable to create " + codecName, ex);
                } catch (ClassNotFoundException ex) {
                    log.error("unable to create " + codecName, ex);
                }
            }
            codecs.put(null, new StringCodecFactory.IdentEncoding());
            stringCodecFactory = new StringCodecFactory(codecs);
        }
        return stringCodecFactory;
    }
}
