/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.regex.Pattern;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class RegExCmsValidator extends AbstractCmsValidator {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(RegExCmsValidator.class);

    private final Pattern pattern;

    private final static String PATTERN_KEY = "regex_pattern";

    public RegExCmsValidator(IPluginContext context, IPluginConfig config) throws Exception {
        super(context, config);
        if (config.containsKey(PATTERN_KEY)) {
            pattern = Pattern.compile(config.getString(PATTERN_KEY));
        } else {
            throw new Exception("regex_pattern property should be set in the iplugin configuration of: " + config.getName());
        }
    }

    @Override
    public void preValidation(IFieldValidator type) throws ValidationException {
        if (!"String".equals(type.getFieldType().getType())) {
            throw new ValidationException("Invalid validation exception; cannot validate non-string field for emptyness");
        }
    }

    @Override
    public Set<Violation> validate(IFieldValidator fieldValidator, JcrNodeModel model, IModel childModel) throws ValidationException {
        Set<Violation> violations = new HashSet<Violation>();
        String value = (String) childModel.getObject();
        if (!pattern.matcher(value).find()) {
            violations.add(fieldValidator.newValueViolation(childModel, getTranslation()));
        }
        return violations;
    }


}
