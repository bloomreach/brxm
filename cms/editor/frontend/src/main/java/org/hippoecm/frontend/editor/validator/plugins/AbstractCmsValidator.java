/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.ValidatorService;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.validation.FeedbackScope;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.hippoecm.frontend.validation.ValidationScope;
import org.hippoecm.frontend.validation.ValidatorUtils;
import org.onehippo.cms.services.validation.api.Validator;

/**
 * @deprecated Implement {@link Validator} instead. Make sure to adjust your repository bootstrap configuration too.
 */
@Deprecated
public abstract class AbstractCmsValidator extends Plugin implements ICmsValidator {

    private static final String SCOPE = "scope";

    private final String name;
    private FeedbackScope feedbackScope = FeedbackScope.DOCUMENT;

    public AbstractCmsValidator(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        name = config.getName().substring(config.getName().lastIndexOf(".") + 1);
        context.registerService(this, ValidatorService.VALIDATOR_SERVICE_ID);

        if (config.containsKey(SCOPE)) {
            feedbackScope = ValidatorUtils.getFeedbackScope(config.getString(SCOPE));
        }
    }

    public String getName() {
        return name;
    }

    /**
     * @deprecated Use {@link #getFeedbackScope()} instead
     */
    @Deprecated
    protected ValidationScope getValidationScope() {
        return ValidationScope.from(feedbackScope);
    }

    protected FeedbackScope getFeedbackScope() {
        return feedbackScope;
    }

    /**
     * Gets the translation for the default validator message in the locale of the currently logged-in user. Validator
     * messages are stored as repository based resource bundles at:
     * <p>
     * /hippo:configuration/hippo:translations/hippo:cms/validators
     * <p>
     * To retrieve the message from the resource bundle, the key is constructed using the last section of this plugin's
     * name.
     *
     * @return a model of the translation of the message
     */
    protected IModel<String> getTranslation() {
        return getResourceBundleModel(getName());
    }

    /**
     * Gets the translation for the alternate validator message {@code alternateKey} in the locale of the currently
     * logged-in user. Validator messages are stored as repository based resource bundles at:
     * <p>
     * /hippo:configuration/hippo:translations/hippo:cms/validators
     * <p>
     * To retrieve the message from the resource bundle, the key is constructed using the pattern {@code
     * "<name>#<alternateKey>"} where name is the last section of this plugin's name.
     *
     * @return a model of the translation of the message
     */
    protected IModel<String> getTranslation(final String alternateKey) {
        final String key = getName() + "#" + alternateKey;
        return getResourceBundleModel(key);
    }

    private IModel<String> getResourceBundleModel(final String key) {
        return new ResourceBundleModel.Builder("hippo:cms.validators", key).build();
    }

}
