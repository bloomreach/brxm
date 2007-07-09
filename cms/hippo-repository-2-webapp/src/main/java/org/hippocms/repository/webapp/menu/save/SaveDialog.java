package org.hippocms.repository.webapp.menu.save;

import org.apache.wicket.Component;
import org.hippocms.repository.webapp.menu.AbstractDialog;

public class SaveDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public SaveDialog(String id, Component targetComponent) {
        super(id, targetComponent);
        setTitle("Save all pending changes");
        setCookieName(id);
    }

}
