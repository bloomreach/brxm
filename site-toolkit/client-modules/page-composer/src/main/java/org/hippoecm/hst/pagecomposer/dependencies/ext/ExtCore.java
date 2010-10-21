package org.hippoecm.hst.pagecomposer.dependencies.ext;

import org.hippoecm.hst.pagecomposer.dependencies.CssDependency;
import org.hippoecm.hst.pagecomposer.dependencies.Dependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsScriptDependency;
import org.hippoecm.hst.pagecomposer.dependencies.VersionedDependency;

public class ExtCore extends VersionedDependency implements Dependency {

    public ExtCore() {
        this("3.3.0");
    }

    public ExtCore(String version) {
        super("lib/ext/core", version);

        addDependency(new CssDependency("resources/css/ext-all.css"));
        addDependency(new JsDependency("adapter/ext/ext-base.js", "adapter/ext/ext-base-debug.js"));
        addDependency(new JsDependency("ext-all.js", "ext-all-debug-w-comments.js"));
        addDependency(new JsScriptDependency("resources/images/default/s.gif") {

            @Override
            protected String getScript(String path) {
                return "Ext.BLANK_IMAGE_URL = '" + path + "';";
            }
        });
    }
}
