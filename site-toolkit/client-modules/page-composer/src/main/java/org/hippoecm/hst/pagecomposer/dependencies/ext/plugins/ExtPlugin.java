package org.hippoecm.hst.pagecomposer.dependencies.ext.plugins;

import org.hippoecm.hst.pagecomposer.dependencies.VersionedDependency;

public class ExtPlugin extends VersionedDependency {

    public ExtPlugin(String name, String version) {
        super("lib/ext/plugins/" + name, version);
    }

}