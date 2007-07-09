package org.hippocms.repository.webapp.menu.node;

import org.apache.wicket.Component;
import org.hippocms.repository.webapp.menu.AbstractDialog;

public class NodeDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public NodeDialog(String id, Component targetComponent) {
        super(id, targetComponent);
        setTitle("Add a new Node");
        setCookieName(id);
    }
 

}
