package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.List;

public class PathDependency extends BaseDependency {

    public PathDependency(String path) {
        super(path);
    }

    public PathDependency(String path, Dependency... dependencies) {
        super(path);
        for(Dependency dependency : dependencies) {
            addDependency(dependency);
        }
    }

    @Override
    public String asString(String path) {
        return null;
    }

    @Override
    protected void addSelf(List<Dependency> all, boolean devMode) {
    }
}
