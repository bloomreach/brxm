/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.password.validation;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordValidationServiceImpl extends Plugin implements IPasswordValidationService {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PasswordValidationServiceImpl.class);

    private List<IPasswordValidator> validators;

    public PasswordValidationServiceImpl(IPluginContext context, IPluginConfig config) {
        super(context, config);

        loadConfiguration();
        context.registerService(this, IPasswordValidationService.class.getName());
    }

    private void loadConfiguration() {
        // get the child pluginconfig nodes that define the validators
        Set<IPluginConfig> configs = getPluginConfig().getPluginConfigSet();
        validators = new ArrayList<IPasswordValidator>(configs.size());
        List<IPasswordValidator> optionalValidators = new ArrayList<IPasswordValidator>(configs.size());
        for (IPluginConfig config : configs) {
            String validatorClassName = config.getString("validator.class");
            try {
                Class candidateClass = Class.forName(validatorClassName);

                if (!IPasswordValidator.class.isAssignableFrom(candidateClass)) {
                    // throw an Exception if the cast below will fail..
                    throw new IllegalArgumentException(
                            "Class {} does not implement IPasswordValidator".replace("{}", candidateClass.getName()));
                }

                @SuppressWarnings({"unchecked"}) // If clause above already make 100% this cast will succeed
                        Class<IPasswordValidator> validatorClass = (Class<IPasswordValidator>) candidateClass;

                Constructor<IPasswordValidator> validatorConstructor = validatorClass.getConstructor(IPluginConfig.class);
                IPasswordValidator validator = validatorConstructor.newInstance(config);
                if (validator.isOptional()) {
                    optionalValidators.add(validator);
                } else {
                    validators.add(validator);
                }
            } catch (Exception e) {
                log.error("Failed to create password validator: " + e.toString());
            }
        }

        // get the required password strength
        int requiredStrength = getPluginConfig().getInt("password.strength", 0);

        // add the optional password validator
        validators.add(new OptionalPasswordValidator(optionalValidators, requiredStrength));

    }

    @Override
    public List<PasswordValidationStatus> checkPassword(String password, User user) throws RepositoryException {
        List<PasswordValidationStatus> result = new ArrayList<PasswordValidationStatus>(validators.size());
        for (IPasswordValidator validator : validators) {
            PasswordValidationStatus status = validator.checkPassword(password, user);
            result.add(status);
        }
        return result;
    }

    public List<IPasswordValidator> getPasswordValidators() {
        return validators;
    }

    private static class OptionalPasswordValidator implements IPasswordValidator {

        private static final long serialVersionUID = 1L;

        private final List<IPasswordValidator> optionalValidators;
        private final int requiredStrength;

        private OptionalPasswordValidator(List<IPasswordValidator> optionalValidators, int requiredStrength) {
            this.optionalValidators = optionalValidators;
            this.requiredStrength = requiredStrength;
            if (requiredStrength > optionalValidators.size()) {
                log.error("The value of the property password.strength is larger than the number of available " +
                        "optional password validators. This way, no attempt at creating a new password can succeed.");
            }
        }

        @Override
        public PasswordValidationStatus checkPassword(String password, User user) throws RepositoryException {
            int passwordStrength = 0;
            for (IPasswordValidator validator : optionalValidators) {
                PasswordValidationStatus status = validator.checkPassword(password, user);
                if (status.accepted()) {
                    passwordStrength++;
                }
            }
            if (passwordStrength < requiredStrength) {
                return new PasswordValidationStatus(getDescription(), false);
            }
            return PasswordValidationStatus.ACCEPTED;
        }

        @Override
        public boolean isOptional() {
            return false;
        }

        @Override
        public String getDescription() {
            StringBuilder description = new StringBuilder();
            description.append(new ClassResourceModel("message", PasswordValidationServiceImpl.class,
                    new Object[]{requiredStrength}).getObject());
            int counter = 1;
            for (IPasswordValidator validator : optionalValidators) {
                description.append("\n").append(counter).append(") ").append(validator.getDescription());
                if (counter < optionalValidators.size()) {
                    description.append(";");
                }
                counter++;
            }
            return description.append(".").toString();
        }

    }

}
