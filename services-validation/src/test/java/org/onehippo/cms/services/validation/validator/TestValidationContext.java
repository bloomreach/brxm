package org.onehippo.cms.services.validation.validator;

import java.util.Locale;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;

public class TestValidationContext implements ValidationContext {

    private String type;
    private String name;

    public TestValidationContext() {
        this(null, null);
    }

    public TestValidationContext(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Violation createViolation(final Validator validator, final Object... parameters) {
        return new TestViolation();
    }

    @Override
    public Violation createViolation(final Validator validator, final String key, final Object... parameters) {
        return null;
    }
}
