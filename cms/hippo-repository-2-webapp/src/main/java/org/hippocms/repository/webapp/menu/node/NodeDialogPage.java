package org.hippocms.repository.webapp.menu.node;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.webapp.menu.AbstractDialogPage;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class NodeDialogPage extends AbstractDialogPage {
    private static final long serialVersionUID = 1L;

    private String name;
    private JcrNodeModel model;

    public NodeDialogPage(final NodeDialog dialog, JcrNodeModel model) {
        super(dialog);
        this.model = model;
        add(new AjaxEditableLabel("name", new PropertyModel(this, "name")));
    }

    public void ok() {
    }

    public void cancel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
