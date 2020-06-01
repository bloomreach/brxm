/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider of {@link HippoLocale}s, based on plugin configuration.  Icons should
 * be made available in it's package and should follow the wicket locale pattern
 * for their names.
 */
public final class LocaleProviderPlugin extends Plugin implements ILocaleProvider, IDetachable {

    private static final long serialVersionUID = 1L;

    public static final String CONFIG_LANGUAGE = "language";
    public static final String CONFIG_COUNTRY = "country";
    public static final String CONFIG_VARIANT = "variant";
    public static final String DEFAULT_COUNTRY = "us";
    public static final String UNKNOWN_COUNTRY = "error";

    static final Logger log = LoggerFactory.getLogger(LocaleProviderPlugin.class);

    static final HippoLocale UNKNOWN_LOCALE = new HippoLocale(Locale.ROOT, "unknown") {

        @Override
        public ResourceReference getIcon(final IconSize size, final LocaleState state) {
            String iconResourceName = getLocaleIconResourceName(size, state, UNKNOWN_COUNTRY);
            return new PackageResourceReference(LocaleProviderPlugin.class, iconResourceName, getLocale(), getName(), null);
        }

        @Override
        public String getDisplayName(final Locale locale) {
            return getLocale().getDisplayLanguage(locale);
        }
    };

    private static String getLocaleIconResourceName(final IconSize size, final LocaleState state, final String country) {
        switch (state) {
            case AVAILABLE:
                return "icons/plus/flag-plus-" + size.getSize() + "_" + country + ".png";
            case DOCUMENT:
            case FOLDER:
            case FOLDER_OPEN:
                return "icons/flags/flag-11x9_" + country + ".png";
            default:
                return "icons/flags/flag-" + size.getSize() + "_" + country + ".png";
        }
    }

    private transient Map<String, HippoLocale> locales;

    public LocaleProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()));
    }

    public List<HippoLocale> getLocales() {
        if (locales == null) {
            locales = loadLocales();
        }
        return new ArrayList<HippoLocale>(locales.values());
    }

    public HippoLocale getLocale(String name) {
        if (isKnown(name)) {
            return locales.get(name);
        } else {
            log.warn("Unknown locale {}", name);
            return UNKNOWN_LOCALE;
        }
    }

    public boolean isKnown(String locale) {
        if (locales == null) {
            locales = loadLocales();
        }
        if (locales.containsKey(locale)) {
            return true;
        }
        return false;
    }

    public void detach() {
        locales = null;
    }

    private Map<String, HippoLocale> loadLocales() {
        IPluginConfig pluginConfig = getPluginConfig();
        Map<String, HippoLocale> locales = new LinkedHashMap<String, HippoLocale>();
        for (IPluginConfig config : pluginConfig.getPluginConfigSet()) {
            if (!config.containsKey(CONFIG_LANGUAGE)) {
                log.warn("Locale " + config.getName() + " does not declare a language");
                continue;
            }

            final Locale locale = getLocale(config);
            final String name = getLocaleName(config);
            final Map<String, String> translations = getTranslations(config);
            locales.put(name, new TranslationLocale(locale, name, translations));
        }
        return locales;
    }

    private static Locale getLocale(final IPluginConfig config) {
        final String language = config.getString(CONFIG_LANGUAGE);
        if (config.containsKey(CONFIG_COUNTRY)) {
            final String country = config.getString(CONFIG_COUNTRY);
            if (config.containsKey(CONFIG_VARIANT)) {
                final String variant = config.getString(CONFIG_VARIANT);
                return new Locale(language, country, variant);
            } else {
                return new Locale(language, country);
            }
        }
        return new Locale(language);
    }

    private static String getLocaleName(final IPluginConfig config) {
        final String name = config.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private static Map<String, String> getTranslations(final IPluginConfig config) {
        final Set<IPluginConfig> translationConfigs = config.getPluginConfigSet();
        final Map<String, String> translations = new HashMap<String, String>();
        for (IPluginConfig translationConfig : translationConfigs) {
            final String language = translationConfig.getString(HippoNodeType.HIPPO_LANGUAGE);
            final String message = translationConfig.getString(HippoNodeType.HIPPO_MESSAGE);
            translations.put(language, message);
        }
        return translations;
    }

    private static class TranslationLocale extends HippoLocale {

        private static final long serialVersionUID = 1L;
        private final Map<String, String> translations;

        public TranslationLocale(final Locale locale, final String name, final Map<String, String> translations) {
            super(locale, name);
            this.translations = translations;
        }

        @Override
        public String getDisplayName(Locale locale) {
            String name = translations.get(locale.getLanguage());
            if (name == null) {
                return getLocale().getDisplayLanguage(locale);
            }
            return name;
        }

        @Override
        public ResourceReference getIcon(IconSize size, LocaleState state) {
            Locale locale = getLocale();
            String country = locale.getCountry();
            if (country != null) {
                country = country.toLowerCase();
            } else {
                country = DEFAULT_COUNTRY;
            }
            String resourceName = getLocaleIconResourceName(size, state, country);
            return new PackageResourceReference(LocaleProviderPlugin.class, resourceName, locale, getName(), null);
        }

    }
}
