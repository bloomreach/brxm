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

import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.ValidatorService;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.hippoecm.frontend.validation.ValidatorMessages;

abstract public class AbstractCmsValidator extends Plugin implements ICmsValidator {

    private final String name;

    public AbstractCmsValidator(IPluginContext context, IPluginConfig config) {
        super(context, config);
        name = config.getName().substring(config.getName().lastIndexOf(".") + 1);
        context.registerService(this, ValidatorService.VALIDATOR_SERVICE_ID);
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the translation for the default validator message in the locale of the currently logged-in user.
     * Validator messages are stored as repository based resource bundles at:
     *
     * /hippo:configuration/hippo:translations/hippo:cms/validators
     *
     * To retrieve the message from the resource bundle, the key is constructed using the last section of
     * this plugin's name.
     *
     * @return a model of the translation of the message
     */
    protected IModel<String> getTranslation() {
        return getResourceBundleModel(getName(), Session.get().getLocale());
    }

    /**
     * Gets the translation for the alternate validator message {@code alternateKey} in the locale of the currently
     * logged-in user. Validator messages are stored as repository based resource bundles at:
     *
     * /hippo:configuration/hippo:translations/hippo:cms/validators
     *
     * To retrieve the message from the resource bundle, the key is constructed using the pattern
     * {@code "<name>#<alternateKey>"} where name is the last section of this plugin's name.
     *
     * @return a model of the translation of the message
     */
    protected IModel<String> getTranslation(String alternateKey) {
        String key = getName() + "#" + alternateKey;
        return getResourceBundleModel(key, Session.get().getLocale());
    }

    /**
     * Return translations of the default messages (those in {@link ValidatorMessages})
     *
     * This method is deprecated. Instead, use {@link #getTranslation()} which uses repository resource bundles.
     * @param key the key for a default message
     * @return a model of the translation of the default message
     */
    @Deprecated
    protected IModel<String> getDefaultMessage(String key) {
        return new ClassResourceModel(key, ValidatorMessages.class);
    }

    private IModel<String> getResourceBundleModel(String key, Locale locale) {
        return new ResourceBundleModel("hippo:cms.validators", key, locale);
    }

}
