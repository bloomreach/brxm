package org.hippoecm.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrNodeModel;

public class DialogLink extends Panel {
    private static final long serialVersionUID = 1L;

    public DialogLink(String id, String linktext, Class clazz, JcrNodeModel model) {
        super(id, model);

        final DialogWindow dialogWindow = new DialogWindow("dialog", model);
        dialogWindow.setPageCreator(new DynamicDialogFactory(dialogWindow, clazz));
        add(dialogWindow);

        AjaxLink link = new AjaxLink("dialog-link") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                dialogWindow.show(target);
            } 
        };
        add(link);
        
        link.add(new Label("dialog-link-text", linktext));
    }

}
