package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;

//TODO: probably clearer to extend JcrNodeModel
public class EditorBean implements IDetachable {
    private static final long serialVersionUID = 1L;

    private JcrNodeModel model; //KEY

    public EditorBean(JcrNodeModel model) {
        this.model = model;
    }

    public JcrNodeModel getModel() {
        return model;
    }

    public void setModel(JcrNodeModel model) {
        this.model = model;
    }

    public void detach() {
        if (model != null) {
            model.detach();
        }
    }

}
