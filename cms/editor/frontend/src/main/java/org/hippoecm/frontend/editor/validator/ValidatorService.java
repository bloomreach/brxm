/*
 *  Copyright 2012-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.validator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorService extends Plugin {

    public static final String VALIDATOR_SERVICE_ID = "validator.instance.service.id";
    public static final String FIELD_VALIDATOR_SERVICE_ID = "field.validator.service.id";
    public static final String DEFAULT_FIELD_VALIDATOR_SERVICE = "field.validator.service";

    private static final Logger log = LoggerFactory.getLogger(ValidatorService.class);

    private final Map<String, ICmsValidator> oldStyleValidators = new HashMap<>();

    public ValidatorService(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        context.registerTracker(new ServiceTracker<ICmsValidator>(ICmsValidator.class) {

            protected void onServiceAdded(final ICmsValidator service, final String name) {
                oldStyleValidators.put(service.getName(), service);
            }

            protected void onRemoveService(final ICmsValidator service, final String name) {
                oldStyleValidators.remove(service.getName());
            }
        }, VALIDATOR_SERVICE_ID);

        context.registerService(this, config.getString(FIELD_VALIDATOR_SERVICE_ID, DEFAULT_FIELD_VALIDATOR_SERVICE));
    }

    public ICmsValidator getValidator(final String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        boolean oldStyleValidatorExists = oldStyleValidators.containsKey(name);
        boolean newStyleValidatorExists = CmsValidatorAdapter.hasValidator(name);

        if (oldStyleValidatorExists && newStyleValidatorExists) {
            log.warn("Validator '{}' has two implementations. Only the new style implementation"
                            + " (/hippo:configuration/hippo:modules/validation/hippo:moduleconfig/{})"
                            + " will be used."
                            + " The old style implementation"
                            + " (/hippo:configuration/hippo:frontend/cms/cms-validators/{})"
                            + " will be ignored and can be removed.",
                    name, name, name);
        }

        if (newStyleValidatorExists) {
            return new CmsValidatorAdapter(name);
        }

        if (oldStyleValidatorExists) {
            return oldStyleValidators.get(name);
        }

        log.warn("Cannot find validator '{}'", name);
        return null;
    }

    public boolean containsValidator(final String name) {
        return CmsValidatorAdapter.hasValidator(name) || oldStyleValidators.containsKey(name);
    }

    /**
     * Checks whether there are validators in the system
     * @return always true
     * @deprecated there are always validators, so usage of this method is pointless
     */
    @Deprecated
    public boolean isEmpty() {
        return false;
    }
}
