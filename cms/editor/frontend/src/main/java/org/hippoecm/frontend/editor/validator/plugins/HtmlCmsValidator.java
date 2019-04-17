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

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.ValidatorMessages;
import org.hippoecm.frontend.validation.Violation;
import org.onehippo.cms.services.validation.validator.NonEmptyHtmlValidator;
import org.onehippo.cms.services.validation.util.HtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for html values.  Verifies that the value is not empty.
 * Use this validator when a customized "Html" type is used.
 * <p>
 * The builtin "Html" type is checked by the
 * {@link NonEmptyCmsValidator} and does not require special treatment.
 * 
 * 
 * @deprecated Use the {@link NonEmptyHtmlValidator} instead.
 */
@Deprecated
public class HtmlCmsValidator extends AbstractCmsValidator {

    private static final Logger log = LoggerFactory.getLogger(HtmlCmsValidator.class);

    public HtmlCmsValidator(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void preValidation(final IFieldValidator type) throws ValidationException {
        if (!"String".equals(type.getFieldType().getType())) {
            throw new ValidationException("Invalid validation exception; " +
                    "cannot validate non-string field for emptiness");
        }
        if ("Html".equals(type.getFieldType().getName())) {
            log.warn("Explicit html validation is not necessary for fields of type 'Html'. " +
                    "This is covered by the 'non-empty' validator.");
        }
    }

    @Override
    public Set<Violation> validate(final IFieldValidator fieldValidator, final JcrNodeModel model,
                                   final IModel childModel) throws ValidationException {

        final Set<Violation> violations = new HashSet<>();
        final String value = (String) childModel.getObject();
        if (HtmlUtils.isEmpty(value)) {
            final ClassResourceModel message = new ClassResourceModel(ValidatorMessages.HTML_IS_EMPTY, ValidatorMessages.class);
            violations.add(fieldValidator.newValueViolation(childModel, message, getFeedbackScope()));
        }
        return violations;
    }

}
