package org.hippoecm.frontend.plugins.xinha.modal;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

public interface IDialog extends IClusterable {
    
    void show(AjaxRequestTarget target);
    
    void ok(AjaxRequestTarget target);

    void cancel(AjaxRequestTarget target);

    void setListener(IDialogListener listener);
    
    ModalWindow getModal();
    
}
