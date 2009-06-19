package org.hippoecm.frontend.plugins.yui.tree;

import org.onehippo.yui.YuiNamespace;

public class TreeNamespace implements YuiNamespace {
    private static final long serialVersionUID = 1L;

    public static final YuiNamespace NS = new TreeNamespace();

    private TreeNamespace() {
    }

    public String getPath() {
        return "inc/";
    }

}
