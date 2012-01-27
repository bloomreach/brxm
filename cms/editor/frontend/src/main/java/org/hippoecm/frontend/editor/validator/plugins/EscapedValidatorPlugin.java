package org.hippoecm.frontend.editor.validator.plugins;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.JcrFieldValidator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @version $Id$
 */
public class EscapedValidatorPlugin extends AbstractValidatorPlugin {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(EscapedValidatorPlugin.class);

    static final Pattern INVALID_CHARS = Pattern.compile(".*[<>&\"'].*");

    public EscapedValidatorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void preValidation(JcrFieldValidator type) throws Exception {
       //do nothing
    }

    @Override
    public Set<Violation> validate(JcrFieldValidator fieldValidator, JcrNodeModel model, IModel childModel) throws ValidationException {
        Set<Violation> violations = new HashSet<Violation>();
        String value = (String) childModel.getObject();
        if (INVALID_CHARS.matcher(value).matches()) {
            violations.add(fieldValidator.newValueViolation(childModel, getTranslation()));
        }
        return violations;
    }


}
