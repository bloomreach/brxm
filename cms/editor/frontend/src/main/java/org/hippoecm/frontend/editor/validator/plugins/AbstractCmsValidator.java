/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.editor.validator.ValidatorService;
import org.hippoecm.frontend.i18n.TranslatorUtils;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.hippoecm.frontend.validation.ValidatorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     *
     * @param key the key for a default message
     * @return a model of the translation of the default mmessage
     */
    protected IModel<String> getDefaultMessage(String key) {
        return new ClassResourceModel(key, ValidatorMessages.class);
    }

    protected String translateKey(String key) {
        String translated = getString(TranslatorUtils.getCriteria(key));
        if(translated==null){
          return EMPTY;
        }
        return translated;
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
