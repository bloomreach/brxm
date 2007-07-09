package org.hippocms.repository.webapp.menu.property;

import org.apache.wicket.Component;
import org.hippocms.repository.webapp.menu.AbstractDialog;

public class PropertyDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public PropertyDialog(String id, Component targetComponent) {
        super(id, targetComponent);
        setTitle("Add a new Property");
        setCookieName(id);
    }

}
