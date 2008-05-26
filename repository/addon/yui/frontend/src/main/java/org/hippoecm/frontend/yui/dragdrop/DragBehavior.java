package org.hippoecm.frontend.yui.dragdrop;

public abstract class DragBehavior extends AbstractDragDropBehavior {
    private static final long serialVersionUID = 1L;

    public DragBehavior() {
        super();
    }

    public DragBehavior(String... groups) {
        super(groups);
    }

    @Override
    protected String getHeaderContributorFilename() {
        return "DD_init.js";
    }
}
