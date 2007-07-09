package org.hippocms.repository.webapp.menu.property;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.webapp.menu.AbstractDialogPage;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class PropertyDialogPage extends AbstractDialogPage {
    private static final long serialVersionUID = 1L;

    private String name;
    private String value;
    private JcrNodeModel model;

    public PropertyDialogPage(final PropertyDialog dialog, JcrNodeModel model) {
        super(dialog);
        this.model = model;
        add(new AjaxEditableLabel("name", new PropertyModel(this, "name")));
        add(new AjaxEditableMultiLineLabel("value", new PropertyModel(this, "value")));
    }

    public void ok() {
        PropertyDialogPage page = (PropertyDialogPage) getPage();
        try {
            model.getNode().setProperty(page.getName(), page.getValue());
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void cancel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
