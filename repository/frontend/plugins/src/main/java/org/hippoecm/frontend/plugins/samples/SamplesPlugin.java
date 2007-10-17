package org.hippoecm.frontend.plugins.samples;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.ContextDialogFactory;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrPropertyModel;
import org.hippoecm.frontend.plugin.Plugin;

public class SamplesPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private ContextDialogFactory contextDialogFactory;
    private String renderer;
    private String exception;

    public SamplesPlugin(String id, final JcrNodeModel model) {
        super(id, model);

        //The modal window that will hold the dialog
        DialogWindow dialogWindow = new DialogWindow("plugin-dialog", model, false);
        add(dialogWindow);
        //The link to open the modal window
        add(dialogWindow.dialogLink("plugin-dialog-link"));
      
        //The ContextDialogFactory loads the dialog content panel.
        contextDialogFactory = new ContextDialogFactory(dialogWindow, model);
        dialogWindow.setPageCreator(contextDialogFactory);

        try {
            JcrPropertyModel propertyModel = new JcrPropertyModel(model, model.getNode().getProperty("renderer"));
            setRenderer(propertyModel.getProperty().getString());
        } catch (RepositoryException e) {
            setRenderer(e.getClass().getName() + ": " + e.getMessage());
        }

        final Label exceptionLabel = new Label("exception", new PropertyModel(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        AjaxEditableLabel editor = new AjaxEditableLabel("renderer", new PropertyModel(this, "renderer")) {
            private static final long serialVersionUID = 1L;
            protected void onSubmit(AjaxRequestTarget target) {
                super.onSubmit(target);
                try {
                    model.getNode().setProperty("renderer", getRenderer());
                } catch (RepositoryException e) {
                    setException(e.getMessage());
                    target.addComponent(exceptionLabel);
                }
            }
        };
        add(editor);
    }

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        JcrNodeModel model = jcrEvent.getModel();
        if (model != null && target != null) {
            //This forwards the ajax update event to the contextDialogFactory
            //causing it to be reconfigured with a new classname. 
            contextDialogFactory.update(target, model);
        }
        if (model != null) {
            JcrNodeModel editorNodeModel = (JcrNodeModel) getModel();
            editorNodeModel.impersonate(model);
            try {
                JcrPropertyModel propertyModel = new JcrPropertyModel(model, model.getNode().getProperty("renderer"));
                setRenderer(propertyModel.getProperty().getString());
            } catch (RepositoryException e) {
                setRenderer(e.getClass().getName() + ": " + e.getMessage());
            }
        }
        if (target != null) {
            target.addComponent(this);
        }
    }

    public void setRenderer(String renderer) {
        this.renderer = renderer;
    }

    public String getRenderer() {
        return renderer;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getException() {
        return exception;
    }

}
