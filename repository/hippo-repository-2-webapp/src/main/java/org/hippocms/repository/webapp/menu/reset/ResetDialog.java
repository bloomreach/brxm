package org.hippocms.repository.webapp.menu.reset;

import org.apache.wicket.Component;
import org.hippocms.repository.webapp.menu.AbstractDialog;

public class ResetDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public ResetDialog(String id, Component targetComponent) {
        super(id, targetComponent);
        setTitle("Discard all pending changes");
        setCookieName(id);
    }



}
