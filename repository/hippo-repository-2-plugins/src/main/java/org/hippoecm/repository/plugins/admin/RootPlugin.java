package org.hippoecm.repository.plugins.admin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.repository.frontend.model.JcrNodeModel;
import org.hippoecm.repository.frontend.plugin.Plugin;

public class RootPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public RootPlugin(String id, JcrNodeModel model) {
        super(id, model);
    }

    public void update(final AjaxRequestTarget target, final JcrNodeModel model) {
    }

}
