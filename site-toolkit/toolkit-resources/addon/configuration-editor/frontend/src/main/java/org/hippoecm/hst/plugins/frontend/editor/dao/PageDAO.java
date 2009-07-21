package org.hippoecm.hst.plugins.frontend.editor.dao;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;

public class PageDAO extends ComponentDAO {
    private static final long serialVersionUID = 1L;

    public PageDAO(IPluginContext context, String namespace) {
        super(context, namespace);
    }

    @Override
    protected String getAbsoluteReferencePath(String name) {
        HstContext ctx = getHstContext();
        String ret = ctx.page.decodeReferenceName(name);
        if (!ret.equals("")) {
            return ctx.page.absolutePath(ret);
        }
        return super.getAbsoluteReferencePath(name);
    }

    @Override
    protected String encodeReference(String name) {
        return getHstContext().page.encodeReferenceName(name);
    }
}
