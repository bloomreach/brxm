package org.hippoecm.hst.plugins.frontend.editor.dao;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.domain.Page;

public class PageDAO extends EditorDAO<Page> {
    private static final long serialVersionUID = 1L;

    public PageDAO(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public Page load(JcrNodeModel model) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void persist(Page k, JcrNodeModel model) {
        // TODO Auto-generated method stub

    }
}
