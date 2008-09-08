package org.hippoecm.frontend.dialog;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;

public class DialogAction implements IClusterable {
    private static final long serialVersionUID = 1L;
    
    final private PageCreator pageCreator;
    final private IDialogService dialogService;
    private boolean enabled = true;

    public DialogAction(final IDialogFactory dialogFactory, final IDialogService dialogService) {
        this.dialogService = dialogService;
        pageCreator = new PageCreator() {
            private static final long serialVersionUID = 1L;

            public Page createPage() {
                return dialogFactory.createDialog(dialogService);
            }
        };
    }

    public void execute() {
        dialogService.show(pageCreator.createPage());
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
}
