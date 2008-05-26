package org.hippoecm.frontend.yui.dragdrop.node;

import org.apache.wicket.ajax.AjaxRequestTarget;

public class DropNodeBehavior extends NodeDragDropBehavior {
    private static final long serialVersionUID = 1L;
    
    @Override
    public void onDrop(AjaxRequestTarget target) {
    }
    
    @Override
    protected String getHeaderContributorFilename() {
        return "DropNode.js";
    }

}
