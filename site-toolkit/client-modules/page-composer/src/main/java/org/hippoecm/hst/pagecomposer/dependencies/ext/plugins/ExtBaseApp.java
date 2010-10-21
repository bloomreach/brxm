package org.hippoecm.hst.pagecomposer.dependencies.ext.plugins;

import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;

public class ExtBaseApp extends ExtPlugin {

    public ExtBaseApp(String version) {
        super("baseapp", version);
        addDependency(new JsDependency("BaseApp.js"));
    }
}
