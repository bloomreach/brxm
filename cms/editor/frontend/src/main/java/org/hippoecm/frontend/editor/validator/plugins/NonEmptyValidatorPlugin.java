package org.hippoecm.frontend.editor.validator.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.HtmlValidator;
import org.hippoecm.frontend.editor.validator.JcrFieldValidator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * @version $Id$
 */
public class NonEmptyValidatorPlugin extends AbstractValidatorPlugin {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(NonEmptyValidatorPlugin.class);

    public NonEmptyValidatorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void preValidation(JcrFieldValidator type) throws Exception {
        if (!"String".equals(type.getFieldType().getType())) {
            throw new StoreException("Invalid validation exception; cannot validate non-string field for emptyness");
        }
        if ("Html".equals(type.getFieldType().getName())) {
            type.setHtmlValidator(new HtmlValidator());
        }
    }

    @Override
    public Set<Violation> validate(JcrFieldValidator fieldValidator, JcrNodeModel model, IModel childModel) throws ValidationException {
        Set<Violation> violations = new HashSet<Violation>();
        String value = (String) childModel.getObject();
        if (StringUtils.isBlank(value)) {
            violations.add(fieldValidator.newValueViolation(childModel, getTranslation()));
        }
        return violations;
    }


}
