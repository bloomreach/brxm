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
package org.hippoecm.frontend.editor.validator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.validation.ICmsValidator;
import org.onehippo.cms7.services.validation.ValidationService;
import org.onehippo.cms7.services.validation.Validator;

import static org.onehippo.cms7.services.validation.util.ServiceUtils.getValidationService;

public class ValidatorService extends Plugin {

    public static final String VALIDATOR_SERVICE_ID = "validator.instance.service.id";
    public static final String FIELD_VALIDATOR_SERVICE_ID = "field.validator.service.id";
    public static final String DEFAULT_FIELD_VALIDATOR_SERVICE = "field.validator.service";

    private final Map<String, ICmsValidator> map = new HashMap<>();

    public ValidatorService(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        context.registerTracker(new ServiceTracker<ICmsValidator>(ICmsValidator.class) {

            protected void onServiceAdded(final ICmsValidator service, final String name) {
                map.put(service.getName(), service);
            }

            protected void onRemoveService(final ICmsValidator service, final String name) {
                map.remove(service.getName());
            }
        }, VALIDATOR_SERVICE_ID);

        context.registerService(this, config.getString(FIELD_VALIDATOR_SERVICE_ID, DEFAULT_FIELD_VALIDATOR_SERVICE));
    }

    public ICmsValidator getValidator(final String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        if (!map.containsKey(name)) {
            final ValidationService validationService = getValidationService();
            final Validator validator = validationService.getValidator(name);
            if (validator != null) {
                map.put(name, new CmsValidatorAdapter(validator));
            }
        }

        return map.getOrDefault(name, null);
    }

    public boolean containsValidator(final String name) {
        return map.containsKey(name);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

}
