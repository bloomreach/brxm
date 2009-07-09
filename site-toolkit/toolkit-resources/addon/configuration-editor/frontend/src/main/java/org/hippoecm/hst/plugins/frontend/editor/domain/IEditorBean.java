package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;

public interface IEditorBean extends IClusterable, IDetachable {
    
    JcrNodeModel getModel();
    void setModel(JcrNodeModel model);
}
