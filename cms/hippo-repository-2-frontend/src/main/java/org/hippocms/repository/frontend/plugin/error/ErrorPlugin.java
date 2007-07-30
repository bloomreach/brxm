package org.hippocms.repository.frontend.plugin.error;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.plugin.Plugin;

public class ErrorPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public ErrorPlugin(String id, JcrNodeModel model, Exception e) {
        super(id, model);
        add(new Label("message", e.getClass().getName() + ": " + e.getMessage()));
    }

    public ErrorPlugin(String id, JcrNodeModel model, String message) {
        super(id, model);
        add(new Label("message", message));
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        // nothing much to do here
    }

}
