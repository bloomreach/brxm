/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.translation.components.document.DocumentTranslationView;
import org.hippoecm.frontend.translation.components.folder.FolderTranslationView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.util.ExtResourcesBehaviour;

/**
 * Provider of {@link HippoLocale}s, based on plugin configuration.  Icons should
 * be made available in it's package and should follow the wicket locale pattern
 * for their names.
 */
public final class LocaleProviderPlugin extends Plugin implements ILocaleProvider, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LocaleProviderPlugin.class);

    static final HippoLocale UNKNOWN_LOCALE = new HippoLocale(Locale.ROOT, "unknown") {

        @Override
        public ResourceReference getIcon(final IconSize size, final LocaleState state) {
            final String country = "_error";
            String resourceName;
            switch (state) {
                case AVAILABLE:
                    resourceName = "plus/flag-plus-" + size.getSize() + country + ".png";
                    break;
                case DOCUMENT:
                    resourceName = "document/flag-document-" + size.getSize() + country + ".png";
                    break;
                case FOLDER:
                    resourceName = "folder_closed/folder-closed-" + size.getSize() + country + ".png";
                    break;
                case FOLDER_OPEN:
                    resourceName = "folder_open/folder-open-" + size.getSize() + country + ".png";
                    break;
                default:
                    resourceName = "flags/flag-" + size.getSize() + country + ".png";
                    break;
            }
            return new ResourceReference(LocaleProviderPlugin.class, "icons/" + resourceName, getLocale(), getName());
        }

        @Override
        public String getDisplayName(final Locale locale) {
            return getLocale().getDisplayLanguage(locale);
        }
    };

    private transient Map<String, HippoLocale> locales;

    public LocaleProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()));

        // debugging pleasure - enable setting breakpoints on the client
        if (Application.get().getConfigurationType().equals(Application.DEVELOPMENT)) {
            Home page = context.getService(Home.class.getName(), Home.class);
            page.add(new ExtResourcesBehaviour());

            page.add(TranslationResources.getTranslationsHeaderContributor());
            page.add(JavascriptPackageResource
                    .getHeaderContribution(DocumentTranslationView.class, "translate-document.js"));

            addFolderViewHeader(page, "treegrid/TreeGridSorter.js");
            addFolderViewHeader(page, "treegrid/TreeGridColumnResizer.js");
            addFolderViewHeader(page, "treegrid/TreeGridNodeUI.js");
            addFolderViewHeader(page, "treegrid/TreeGridLoader.js");
            addFolderViewHeader(page, "treegrid/TreeGridColumns.js");
            addFolderViewHeader(page, "treegrid/TreeGrid.js");
            addFolderViewHeader(page, "folder-translations.js");
        }
    }

    private static void addFolderViewHeader(Page page,  String js) {
        page.add(JavascriptPackageResource.getHeaderContribution(FolderTranslationView.class, js));
    }

    public List<HippoLocale> getLocales() {
        if (locales == null) {
            locales = loadLocales();
        }
        return new ArrayList<HippoLocale>(locales.values());
    }

    public HippoLocale getLocale(String name) {
        if (locales == null) {
            locales = loadLocales();
        }
        if (locales.containsKey(name)) {
            return locales.get(name);
        } else {
            log.warn("Unknown locale {}", name);
            return UNKNOWN_LOCALE;
        }
    }

    public void detach() {
        locales = null;
    }

    private Map<String, HippoLocale> loadLocales() {
        IPluginConfig pluginConfig = getPluginConfig();
        Map<String, HippoLocale> locales = new LinkedHashMap<String, HippoLocale>();
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
            locales.put(name, new HippoLocale(locale, name) {
                private static final long serialVersionUID = 1L;

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
                        country = "_" + country.toLowerCase();
                    } else {
                        country = "_us";
                    }

                    String resourceName;
                    switch (state) {
                    case AVAILABLE:
                        resourceName = "plus/flag-plus-" + size.getSize() + country + ".png";
                        break;
                    case DOCUMENT:
                        resourceName = "document/flag-document-" + size.getSize() + country + ".png";
                        break;
                    case FOLDER:
                        resourceName = "folder_closed/folder-closed-" + size.getSize() + country + ".png";
                        break;
                    case FOLDER_OPEN:
                        resourceName = "folder_open/folder-open-" + size.getSize() + country + ".png";
                        break;
                    default:
                        resourceName = "flags/flag-" + size.getSize() + country + ".png";
                        break;
                    }
                    return new ResourceReference(LocaleProviderPlugin.class, "icons/" + resourceName, locale, getName());
                }
            });
        }
        return locales;
    }

}
