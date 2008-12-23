package org.hippoecm.frontend.plugins.xinha.dialog;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;

public interface IDialogModel extends IModel {

    boolean isSubmittable();
    
    boolean isAttacheable();
    
    boolean isDetacheable();
    
    boolean isReplacing();

    void reset();

    String toJsString();

    IModel getPropertyModel(String key);
    
    void setNodeModel(JcrNodeModel model);
    
    JcrNodeModel getNodeModel();
    
    JcrNodeModel getInitialModel();
    
}
