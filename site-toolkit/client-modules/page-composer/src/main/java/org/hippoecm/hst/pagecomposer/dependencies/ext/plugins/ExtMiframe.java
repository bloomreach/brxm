package org.hippoecm.hst.pagecomposer.dependencies.ext.plugins;

import org.hippoecm.hst.pagecomposer.dependencies.Dependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;

public class ExtMiframe extends ExtPlugin implements Dependency {

    public ExtMiframe(String version) {
        this(version, true);
    }

    public ExtMiframe(String version, boolean enableMessaging) {
        super("miframe", version);
        addDependency(new JsDependency("miframe.js", "miframe-debug.js"));
        if(enableMessaging) {
            addDependency(new JsDependency("modules/mifmsg.js"));
        }
    }
}
