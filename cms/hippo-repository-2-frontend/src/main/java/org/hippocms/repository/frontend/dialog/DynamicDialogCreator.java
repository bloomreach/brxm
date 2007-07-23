package org.hippocms.repository.frontend.dialog;

import java.lang.reflect.Constructor;

import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippocms.repository.frontend.dialog.AbstractDialog;
import org.hippocms.repository.frontend.dialog.DialogWindow;
import org.hippocms.repository.frontend.dialog.error.ErrorDialog;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class DynamicDialogCreator implements PageCreator {
    private static final long serialVersionUID = 1L;

    private DialogWindow window;
    private JcrNodeModel model;

    public DynamicDialogCreator(DialogWindow window, JcrNodeModel model) {
        this.window = window;
        this.model = model;
    }

    public Page createPage() {
        String classname = getClassname();
        try {
            Class dialogClass = Class.forName(classname);
            Constructor constructor = dialogClass.getConstructor(new Class[] { window.getClass(), model.getClass() });
            return (AbstractDialog) constructor.newInstance(new Object[] { window, model });
        } catch (Exception e) {
            ErrorDialog dialog = new ErrorDialog(window, model);
            dialog.setMessage(e.getClass().getName() + ": " + e.getMessage());
            return dialog;
        }
    }
    
    private String getClassname() {   
        WebApplication application = (WebApplication)Application.get();
        String className = application.getInitParameter("dynamicDialog");
        if (className == null || className.equals("")) {
            className = application.getServletContext().getInitParameter("dynamicDialog");
        }
        if (className == null || className.equals("")) {
            className = "null";
        }
        return className;
    }

}
