package org.hippoecm.frontend.editor.layout;

public class LayoutHelper {

    public final static String getWicketId(ILayoutPad pad) {
        return "${cluster.id}." + pad.getName();
    }

}
