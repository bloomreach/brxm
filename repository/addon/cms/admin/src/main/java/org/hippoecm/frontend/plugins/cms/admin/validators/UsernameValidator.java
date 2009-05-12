package org.hippoecm.frontend.plugins.cms.admin.validators;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugins.cms.admin.users.User;

public class UsernameValidator extends StringValidator {
    private static final long serialVersionUID = 1L;

    @Override
    protected void onValidate(IValidatable validatable) {
        String username = (String) validatable.getValue();
        if (User.userExists(username)) {
            error(validatable);
        }
    }

    @Override
    protected String resourceKey() {
        return "UsernameValidator.exists";
    }
}