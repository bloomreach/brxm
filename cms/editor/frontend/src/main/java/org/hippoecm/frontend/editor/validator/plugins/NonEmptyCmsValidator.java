/*
 *  Copyright 2012 Hippo.
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
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator that validates that a String value is non-empty.
 * <p>
 * When the type of the value is the builtin "Html" type, an {@link HtmlValidator} is used to verify this.
 * Such a field therefore does not require the html validator to be declared separately.
 */
public class NonEmptyCmsValidator extends AbstractCmsValidator {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(NonEmptyCmsValidator.class);

    private HtmlValidator htmlValidator;

    public NonEmptyCmsValidator(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void preValidation(IFieldValidator type) throws ValidationException {
        if (!"String".equals(type.getFieldType().getType())) {
            throw new ValidationException("Invalid validation exception; cannot validate non-string field for emptiness");
        }
        if ("Html".equals(type.getFieldType().getName())) {
            htmlValidator = new HtmlValidator();
        }
    }

    @Override
    public Set<Violation> validate(IFieldValidator fieldValidator, JcrNodeModel model, IModel childModel) throws ValidationException {
        Set<Violation> violations = new HashSet<Violation>();
        String value = (String) childModel.getObject();
        if (htmlValidator != null) {
            for (String key : htmlValidator.validateNonEmpty(value)) {
                violations.add(fieldValidator.newValueViolation(childModel, key));
            }
        } else {
            if (StringUtils.isBlank(value)) {
                violations.add(fieldValidator.newValueViolation(childModel, getTranslation()));
            }
        }
        return violations;
    }


}
