package org.hippoecm.hst.pagecomposer.dependencies.ext.plugins;

import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;

public class ExtFloatingWindow extends ExtPlugin {

    public ExtFloatingWindow(String version) {
        super("floatingwindow", version);
        addDependency(new JsDependency("FloatingWindow.js"));
    }
}
