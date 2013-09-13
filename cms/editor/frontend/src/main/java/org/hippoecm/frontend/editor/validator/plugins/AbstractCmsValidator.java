/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.validator.plugins;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.editor.validator.ValidatorService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.hippoecm.frontend.validation.ValidatorMessages;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
abstract public class AbstractCmsValidator extends Plugin implements ICmsValidator, IStringResourceProvider {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(AbstractCmsValidator.class);

    private final String name;
    private final static String EMPTY = "";

    public AbstractCmsValidator(IPluginContext context, IPluginConfig config) {
        super(context, config);
        name = config.getName().substring(config.getName().lastIndexOf(".") + 1);
        context.registerService(this, ValidatorService.VALIDATOR_SERVICE_ID);
    }

    public String getName() {
        return name;
    }

    protected IModel<String> getTranslation() {
        return new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return translateKey(getName());
            }
        };
    }

    /**
     * Return translations of the default messages (those in {@link ValidatorMessages})
     * @param key
     * @return
     */
    protected IModel<String> getDefaultMessage(String key) {
        return new ClassResourceModel(key, ValidatorMessages.class);
    }

    protected String translateKey(String key) {
        String translated = getString(getCriteria(key));
        if(translated==null){
          return EMPTY;
        }
        return translated;
    }

    protected Map<String, String> getCriteria(String key) {
        Map<String, String> keys = new TreeMap<String, String>();
        String realKey;
        if (key.indexOf(',') > 0) {
            realKey = key.substring(0, key.indexOf(','));
            IValueMap map = new ValueMap(key.substring(key.indexOf(',') + 1));
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
            keys.put("country", locale.getCountry());
        }

        value = locale.getVariant();
        if (value != null) {
            keys.put("variant", locale.getVariant());
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

    @Override
    public String getResourceProviderKey() {
        final String[] translators = getPluginConfig().getStringArray(ITranslateService.TRANSLATOR_ID);
        if (translators != null) {
            return Arrays.toString(translators);
        }
        return null;
    }


}
