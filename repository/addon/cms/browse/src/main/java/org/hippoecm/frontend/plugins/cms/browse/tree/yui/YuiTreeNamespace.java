package org.hippoecm.frontend.plugins.cms.browse.tree.yui;

import org.onehippo.yui.YuiNamespace;

public class YuiTreeNamespace implements YuiNamespace {
    private static final long serialVersionUID = 1L;
    
    public static final YuiTreeNamespace  NS = new YuiTreeNamespace ();

    private YuiTreeNamespace () {
    }

    public String getPath() {
        return "js/";
    }

}
