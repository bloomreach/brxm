package org.hippoecm.hst.pagecomposer.dependencies.ext.plugins;

import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;

public class ExtBaseGrid extends ExtPlugin {

    public ExtBaseGrid(String version) {
        super("basegrid", version);
        addDependency(new JsDependency("BaseGrid.js"));
    }
}
