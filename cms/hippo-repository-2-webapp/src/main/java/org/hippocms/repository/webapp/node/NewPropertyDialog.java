package org.hippocms.repository.webapp.node;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

public class NewPropertyDialog extends ModalWindow {
    private static final long serialVersionUID = 1L;

    public NewPropertyDialog(String id) {
        super(id);
        
        setContent(new NewPropertyDialogPanel(getContentId()));
        setTitle("Add a new Property");
        setCookieName("newPropertyDialog");

        setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
            private static final long serialVersionUID = 1L;
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                // setResult("Modal window 2 - close button");
                return true;
            }
        });
        
        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;
            public void onClose(AjaxRequestTarget target) {
                //  target.addComponent(result);
            }
        });
    }


}
