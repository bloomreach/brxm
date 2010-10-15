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
package org.hippoecm.frontend.translation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider of {@link HippoLocale}s, based on plugin configuration.  Icons should
 * be made available in it's package and should follow the wicket locale pattern
 * for their names.
 */
public final class LocaleProviderPlugin extends Plugin implements ILocaleProvider {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LocaleProviderPlugin.class);

    public LocaleProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString("locale.id", ILocaleProvider.class.getName()));
    }

    public List<HippoLocale> getLocales() {
        IPluginConfig pluginConfig = getPluginConfig();
        List<HippoLocale> locales = new LinkedList<HippoLocale>();
        for (IPluginConfig config : pluginConfig.getPluginConfigSet()) {
            if (!config.containsKey("language")) {
                log.warn("Locale " + config.getName() + " does not declare a language");
                continue;
            }
            Locale locale;
            if (config.containsKey("country")) {
                if (config.containsKey("variant")) {
                    locale = new Locale(config.getString("language"), config.getString("country"), config
                            .getString("variant"));
                } else {
                    locale = new Locale(config.getString("language"), config.getString("country"));
                }
            } else {
                locale = new Locale(config.getString("language"));
            }
            String name = config.getName();
            name = name.substring(name.lastIndexOf('.') + 1);
            Set<IPluginConfig> translationConfigs = config.getPluginConfigSet();
            final Map<String, String> translations = new HashMap<String, String>();
            for (IPluginConfig translationConfig : translationConfigs) {
                translations.put(translationConfig.getString("hippo:language"), translationConfig
                        .getString("hippo:message"));
            }
            locales.add(new HippoLocale(locale, name) {
                private static final long serialVersionUID = 1L;

                @Override
                public String getDisplayName(Locale locale) {
                    String name = translations.get(locale.getLanguage());
                    if (name == null) {
                        return locale.getLanguage();
                    }
                    return name;
                }

                @Override
                public ResourceReference getIcon(IconSize size, LocaleState state) {
                    String resourceName;
                    switch (state) {
                    case AVAILABLE:
                        resourceName = "flag-new-" + size.getSize() + ".png";
                        break;
                    case DISABLED:
                        resourceName = "flag-disabled-" + size.getSize() + ".png";
                        break;
                    default:
                        resourceName = "flag-" + size.getSize() + ".png";
                        break;
                    }
                    return new ResourceReference(LocaleProviderPlugin.class, resourceName, getLocale(), getName());
                }
            });
        }
        return locales;
    }

}
