package org.hippocms.repository.webapp.menu;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

public abstract class AbstractDialog extends ModalWindow {
    private static final long serialVersionUID = 1L;

    protected AbstractDialog(String id, final Component targetComponent) {
        super(id);
        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;
            public void onClose(AjaxRequestTarget target) {
                target.addComponent(targetComponent);
            }
        });
    }

    public AjaxLink dialogLink(String id) {
        return new AjaxLink(id) {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                show(target);
            }
        };
    }

}
