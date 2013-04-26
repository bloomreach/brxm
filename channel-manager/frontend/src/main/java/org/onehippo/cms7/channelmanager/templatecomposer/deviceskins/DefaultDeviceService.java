/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.wicket.Session;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtArrayStore;
import org.wicketstuff.js.ext.data.ExtDataField;
import org.wicketstuff.js.ext.data.ExtStore;

/**
 * @version "$Id$"
 */
public class DefaultDeviceService extends Plugin implements DeviceService, Translatable {

    private static Logger log = LoggerFactory.getLogger(DefaultDeviceService.class);
    protected static final char CH_COMMA = ',';
    protected static final String COUNTRY = "country";
    protected static final String VARIANT = "variant";

    private ExtStore store;

    protected final List<StyleableDevice> list = new ArrayList<StyleableDevice>();

    /**
     * Construct a new Plugin.
     *
     * @param context the plugin context
     * @param config  the plugin config
     */
    public DefaultDeviceService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config != null && config.getString("device.service.id") != null) {
            context.registerService(this, config.getString("device.service.id"));
        }


        if (config != null) {
            final Set<IPluginConfig> pluginConfigSet = config.getPluginConfigSet();
            for (IPluginConfig pluginConfig : pluginConfigSet) {
                StyleableDevice styleable = createStyleable(context, pluginConfig);
                styleable.setName(translateKey(styleable.getName()));
                this.list.add(styleable);
            }
        }


        initExtStore();
    }

    /**
     * Overwrite class whenever you would like to have your custom device store
     */
    public void initExtStore() {
        this.store = new ExtArrayStore<StyleableDevice>(Arrays.asList(new ExtDataField("name"), new ExtDataField("id")),list);
    }


    public StyleableDevice createStyleable(final IPluginContext context, final IPluginConfig config) {
        return new StyleableTemplateDeviceModel(config);
    }

    @Override
    public ExtStore<StyleableDevice> getStore() {
        return store;
    }

    @Override
    public List<StyleableDevice> getStylables() {
        return list;
    }


    public String translateKey(String key) {
        String translated = getString(getCriteria(key));
        if (translated == null) {
            return key;
        }
        return translated;
    }

    protected Map<String, String> getCriteria(String key) {
        Map<String, String> keys = new TreeMap<String, String>();
        String realKey;
        if (key.indexOf(CH_COMMA) > 0) {
            realKey = key.substring(0, key.indexOf(CH_COMMA));
            IValueMap map = new ValueMap(key.substring(key.indexOf(CH_COMMA) + 1));
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String) {
                    keys.put(entry.getKey(), (String) entry.getValue());
                }
            }
        } else {
            realKey = key;
        }
        keys.put(HippoNodeType.HIPPO_KEY, realKey);

        Locale locale = Session.get().getLocale();
        keys.put(HippoNodeType.HIPPO_LANGUAGE, locale.getLanguage());

        String value = locale.getCountry();
        if (value != null) {
            keys.put(COUNTRY, locale.getCountry());
        }

        value = locale.getVariant();
        if (value != null) {
            keys.put(VARIANT, locale.getVariant());
        }
        return keys;
    }

    public String getString(Map<String, String> criteria) {
        String[] translators = getPluginConfig().getStringArray(ITranslateService.TRANSLATOR_ID);
        if (translators != null) {
            for (String translatorId : translators) {
                ITranslateService translator = getPluginContext().getService(translatorId,
                        ITranslateService.class);
                if (translator != null) {
                    String translation = translator.translate(criteria);
                    if (translation != null) {
                        return translation;
                    }
                }
            }
        }
        return null;
    }


}
