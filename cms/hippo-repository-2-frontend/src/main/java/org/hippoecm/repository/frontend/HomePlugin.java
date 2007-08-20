package org.hippoecm.repository.frontend;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.repository.frontend.model.JcrNodeModel;
import org.hippoecm.repository.frontend.plugin.Plugin;

public class HomePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public HomePlugin(String id, JcrNodeModel model) {
        super(id, model);
    }

    public void update(final AjaxRequestTarget target, final JcrNodeModel model) {        
    }

}
