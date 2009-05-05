package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.hippoecm.frontend.model.JcrNodeModel;

public class Page extends EditorBean {
    private static final long serialVersionUID = 1L;

    public Page(JcrNodeModel model) {
        super(model);
    }

    String path;

}
