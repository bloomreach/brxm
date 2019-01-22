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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.HtmlValidator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.ValidatorMessages;
import org.hippoecm.frontend.validation.Violation;

/**
 * Validator that validates that a String value is non-empty.
 * <p>
 * When the type of the value is the builtin "Html" type, an {@link HtmlValidator} is used to verify this.
 * Such a field therefore does not require the html validator to be declared separately.
 */
public class NonEmptyCmsValidator extends AbstractCmsValidator {

    private HtmlValidator htmlValidator;

    public NonEmptyCmsValidator(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void preValidation(final IFieldValidator fieldValidator) throws ValidationException {
        final ITypeDescriptor fieldType = fieldValidator.getFieldType();

        if (!"String".equals(fieldType.getType())) {
            throw new ValidationException("Invalid validation exception; " +
                    "cannot validate non-string field for emptiness");
        }

        if ("Html".equals(fieldType.getName()) && htmlValidator == null) {
            htmlValidator = new HtmlValidator();
        }
    }

    @Override
    public Set<Violation> validate(final IFieldValidator fieldValidator, final JcrNodeModel model, 
                                   final IModel childModel) throws ValidationException {
        
        final Set<Violation> violations = new HashSet<>();
        final String value = (String) childModel.getObject();

        if ("Html".equals(fieldValidator.getFieldType().getName())) {
            for (final String key : htmlValidator.validateNonEmpty(value)) {
                final ClassResourceModel message = new ClassResourceModel(key, ValidatorMessages.class);
                final Violation violation = fieldValidator.newValueViolation(childModel, message);
                violation.setValidationScope(getValidationScope());
                violations.add(violation);
            }
        } else {
            if (StringUtils.isBlank(value)) {
                violations.add(fieldValidator.newValueViolation(childModel, getTranslation()));
            }
        }
        return violations;
    }


}
