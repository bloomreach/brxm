package org.hippocms.repository.frontend.plugin.samples.component;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippocms.repository.frontend.IUpdatable;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class UpdatableLabel extends Label implements IUpdatable {
    private static final long serialVersionUID = 1L;

    public UpdatableLabel(String id) {
        super(id);
        setOutputMarkupId(true);
    }

    public UpdatableLabel(String id, IModel model)  {
        super(id);
        setOutputMarkupId(true);       
        try {
            JcrNodeModel nodeModel = (JcrNodeModel)model;
            setModel(new Model(nodeModel.getNode().getPath()));
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public UpdatableLabel(String id, String label) {
        super(id, label);
        setOutputMarkupId(true);
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        if (model != null) {
            try {
                setModel(new Model(model.getNode().getPath()));
            } catch (RepositoryException e) {
                setModel(new Model(e.getClass().getName() + ": " + e.getMessage()));
            }
        }
        if (target != null) {
            target.addComponent(this);
        }
    }

}
