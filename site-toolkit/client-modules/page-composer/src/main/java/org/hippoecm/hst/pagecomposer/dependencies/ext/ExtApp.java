package org.hippoecm.hst.pagecomposer.dependencies.ext;

import org.hippoecm.hst.pagecomposer.dependencies.CssDependency;
import org.hippoecm.hst.pagecomposer.dependencies.Dependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;
import org.hippoecm.hst.pagecomposer.dependencies.PathDependency;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtBaseApp;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtBaseGrid;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtFloatingWindow;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtMiframe;

public class ExtApp extends PathDependency implements Dependency {

    public ExtApp() {
        super("/hippo/pagecomposer/sources");
        addDependency(new PathDependency("js",

            new ExtCore("3.3.0"),
            new ExtMiframe("2.1.5"),
            new ExtFloatingWindow("1.0.0"),
            new ExtBaseApp("1.0.0"),
            new ExtBaseGrid("1.0.0"),

            new JsDependency("src/globals.js"),    
            new PathDependency("src/ext",
                new JsDependency("PropertiesPanel.js"),
                new JsDependency("PageModel.js"),
                new JsDependency("PageEditor.js")
            )
        ));
        addDependency(new PathDependency("css",
            new CssDependency("PageEditor.css")
        ));
    }
}
