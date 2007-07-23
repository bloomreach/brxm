package org.hippocms.repository.frontend.update;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public interface IUpdatable {
    
    public void update(AjaxRequestTarget target, JcrNodeModel model);

}
