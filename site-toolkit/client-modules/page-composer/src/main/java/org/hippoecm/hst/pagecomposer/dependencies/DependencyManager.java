package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class DependencyManager {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private boolean devMode;
    private List<Dependency> dependencies;

    public DependencyManager() {
        this(false);
    }

    public DependencyManager(boolean devMode) {
        dependencies = new LinkedList<Dependency>();
        this.devMode = devMode;
    }

    public void add(Dependency dependency) {
        dependencies.add(dependency);
    }

    public void write(DependencyWriter writer) {
        for(Dependency dependency : dependencies) {
            Collection<Dependency> col = dependency.getDependencies(devMode);
            for(Dependency inner : col) {
                writer.write(inner);
            }
        }
    }

}
