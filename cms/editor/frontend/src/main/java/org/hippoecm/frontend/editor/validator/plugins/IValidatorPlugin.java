package org.hippoecm.frontend.editor.validator.plugins;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.JcrFieldValidator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;

import java.util.Set;

/**
 * @version $Id$
 */
public interface IValidatorPlugin {

    public void preValidation(JcrFieldValidator type) throws Exception;

    public Set<Violation> validate(JcrFieldValidator fieldValidator, JcrNodeModel model, IModel childModel) throws ValidationException;

}
