package org.hippocms.repository.frontend.plugin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public abstract class Plugin extends Panel {
    
    public Plugin(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);
    }
    
    public abstract void update(AjaxRequestTarget target, JcrNodeModel model);

}
