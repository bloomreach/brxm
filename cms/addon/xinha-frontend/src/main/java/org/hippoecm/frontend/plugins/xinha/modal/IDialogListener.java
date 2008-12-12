package org.hippoecm.frontend.plugins.xinha.modal;

import org.apache.wicket.IClusterable;

public interface IDialogListener extends IClusterable {
    
    void onDialogClose();
    
    String onDialogOk();
}
