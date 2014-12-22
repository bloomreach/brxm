/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.resourcebundle.validators;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.onehippo.cms7.resourcebundle.data.ValueSet;

/**
 * Validates the key of a resource: it must not be blank or a duplicate.
 */
public class ValueSetNameValidator implements IValidator<String> {

    private static final long serialVersionUID = 1L;
    private final ValueSet valueSet;
    private final Component component;

    public ValueSetNameValidator(Component component, ValueSet valueSet) {
        this.valueSet = valueSet;
        this.component = component;
    }

    protected ValueSet getValueSet() {
        return valueSet;
    }

    @Override
    public void validate(final IValidatable<String> validatable) {
        final String value = validatable.getValue();
        ValueSet currentValueSet = getValueSet();

        if (StringUtils.isBlank(value)) {
            validatable.error(new ValidationError(getMessage("validation.valueset.name.empty")));
            return;
        }

        if (currentValueSet.getBundle().getDefaultValueSetName().equals(value)) {
            validatable.error(new ValidationError(getMessage("validation.valueset.name.reserved")));
            return;
        }

        for (ValueSet other : currentValueSet.getBundle().getValueSets()) {
            if (other == currentValueSet) {
                continue; // don't check for duplication against self.
            }
            if (other.getDisplayName().equals(value)) {
                validatable.error(new ValidationError(getMessage("validation.valueset.name.duplicate")));
                return;
            }
        }
    }

    private String getMessage(String key) {
        return Application.get().getResourceSettings().getLocalizer().getString(key, component);
    }
}
