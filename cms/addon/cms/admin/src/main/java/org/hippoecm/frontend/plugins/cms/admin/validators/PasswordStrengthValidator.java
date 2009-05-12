package org.hippoecm.frontend.plugins.cms.admin.validators;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;


public class PasswordStrengthValidator extends StringValidator {
    private static final long serialVersionUID = 1L;

    @Override
    protected void onValidate(IValidatable validatable) {
        String password = (String) validatable.getValue();
        // currently only check length
        if (password.length() < 4) {
            error(validatable);
        }
    }

    @Override
    protected String resourceKey() {
        return "PasswordStrength.invalid";
    }
}