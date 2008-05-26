package org.hippoecm.frontend.yui.dragdrop;

import org.apache.wicket.ajax.AjaxRequestTarget;

public class DropBehavior extends AbstractDragDropBehavior {
    private static final long serialVersionUID = 1L;

    public DropBehavior() {
        super();
    }

    public DropBehavior(String... groups) {
        super(groups);
    }

    @Override
    protected String getHeaderContributorFilename() {
        return "DDTarget_init.js";
    }

    @Override
    public void onDrop(AjaxRequestTarget target) {

    }

}
