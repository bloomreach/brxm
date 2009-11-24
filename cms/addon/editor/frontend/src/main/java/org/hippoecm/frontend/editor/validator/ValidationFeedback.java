/**
 * 
 */
package org.hippoecm.frontend.editor.validator;

import org.apache.wicket.Component;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

final class ValidationFeedback extends Component implements IFeedbackLogger {
    private static final long serialVersionUID = 1L;

    ValidationFeedback(String id, IModel<?> model) {
        super(id, model);
    }

    @Override
    protected void onRender(MarkupStream markupStream) {
        throw new UnsupportedOperationException();
    }

    public void error(String messageKey, Object[] parameters) {
        error(new StringResourceModel(messageKey, this, getDefaultModel(), parameters).getObject());
    }

    public void warn(String messageKey, Object[] parameters) {
        warn(new StringResourceModel(messageKey, this, getDefaultModel(), parameters).getObject());
    }
}