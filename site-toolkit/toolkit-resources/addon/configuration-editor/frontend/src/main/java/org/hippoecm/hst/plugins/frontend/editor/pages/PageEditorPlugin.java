package org.hippoecm.hst.plugins.frontend.editor.pages;

import java.util.List;

import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.components.ComponentEditorPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.PageDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component;

public class PageEditorPlugin extends ComponentEditorPlugin {
    private static final long serialVersionUID = 1L;

    public PageEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected EditorDAO<Component> newDAO() {
        return new PageDAO(getPluginContext(), hstContext.page.getNamespace());
    }

    @Override
    protected Dialog newAddDialog() {
        return new AddPageDialog((PageDAO) dao, this, (JcrNodeModel) getModel());
    }

    @Override
    public List<String> getReferenceableComponents() {
        List<String> refs = hstContext.page.getReferenceables();
        refs.addAll(super.getReferenceableComponents());
        return refs;
    }
}
