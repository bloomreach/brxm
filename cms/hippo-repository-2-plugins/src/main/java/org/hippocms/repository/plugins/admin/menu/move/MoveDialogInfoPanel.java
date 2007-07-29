package org.hippocms.repository.plugins.admin.menu.move;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class MoveDialogInfoPanel extends Panel {
    private static final long serialVersionUID = 1L;
    private String destinationPath;

    public MoveDialogInfoPanel(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);

        try {
            add(new Label("source", model.getNode().getPath()));
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        add(new Label("destination", new PropertyModel(this, "destinationPath")));
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

}