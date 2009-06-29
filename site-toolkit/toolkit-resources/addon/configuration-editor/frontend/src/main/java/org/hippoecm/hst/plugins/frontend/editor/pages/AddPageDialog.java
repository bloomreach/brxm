package org.hippoecm.hst.plugins.frontend.editor.pages;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.components.AddComponentDialog;
import org.hippoecm.hst.plugins.frontend.editor.dao.PageDAO;

public class AddPageDialog extends AddComponentDialog {
    private static final long serialVersionUID = 1L;

    public AddPageDialog(PageDAO dao, RenderPlugin plugin, JcrNodeModel parent) {
        super(dao, plugin, parent);
    }

}
