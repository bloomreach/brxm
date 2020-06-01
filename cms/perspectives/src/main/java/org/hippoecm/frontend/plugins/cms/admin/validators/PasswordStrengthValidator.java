/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.validators;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.IPasswordValidationService;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordStrengthValidator implements IValidator<String> {

    private static final Logger log = LoggerFactory.getLogger(PasswordStrengthValidator.class);

    private final Component form;
    private final IPasswordValidationService passwordValidationService;
    private final IModel<User> userModel;

    public PasswordStrengthValidator(final Component form, final IPluginContext context, final IModel<User> userModel) {
        this.form = form;
        this.passwordValidationService = context.getService(IPasswordValidationService.class.getName(),
                IPasswordValidationService.class);
        this.userModel = userModel;
    }

    @Override
    public void validate(final IValidatable<String> validatable) {
        final String password = validatable.getValue();

        if (passwordValidationService == null) {
            // fallback on pre 7.7 behavior
            if (password.length() < 4) {
                form.error(new ClassResourceModel("PasswordStrength.invalid", getClass()).getObject());
            }
            return;
        }

        try {
            passwordValidationService.checkPassword(password, userModel.getObject()).stream()
                    .filter(status -> !status.accepted())
                    .forEach(status -> form.error(status.getMessage()));
        } catch (RepositoryException e) {
            log.error("Failure validating password using password validation service", e);
        }
    }

}
