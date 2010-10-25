package org.hippoecm.hst.pagecomposer.dependencies.ext.plugins;

import org.hippoecm.hst.pagecomposer.dependencies.CssDependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;
import org.hippoecm.hst.pagecomposer.dependencies.PathDependency;

public class ExtColorField extends ExtPlugin {
    public ExtColorField(String version) {
        super("colorfield", version);
        addDependency(new JsDependency("colorfield.js", "colorfield.js"));
        addDependency(new PathDependency("css",
                new CssDependency("colorfield.css")
        ));

    }
}
