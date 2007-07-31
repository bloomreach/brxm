package org.hippocms.repository.frontend.plugin.error;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.plugin.Plugin;

public class ErrorPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public ErrorPlugin(String id, JcrNodeModel model, Exception exception, String message) {
        super(id, model);
        String errorMessage = "";
        if (exception != null) {
            errorMessage = exception.getClass().getName() + ": " + exception.getMessage();
        }
        if (exception != null &&  message != null) {
            errorMessage += "\n";
        }
        if (message != null) {
            errorMessage += message;
        }
        add(new MultiLineLabel("message", errorMessage));
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        // nothing much to do here
    }

}
