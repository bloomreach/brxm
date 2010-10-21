package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class BaseDependency implements Dependency {

    private String path;
    private List<Dependency> dependencies;
    private Dependency parent;

    public BaseDependency(String path) {
        this.path = path;
    }

    protected void addDependency(Dependency dep) {
        if(dependencies == null) {
            dependencies = new LinkedList<Dependency>();
        }
        dependencies.add(dep);
        dep.setParent(this);
    }

    public Collection<Dependency> getDependencies(boolean devMode) {
        List<Dependency> all = new LinkedList<Dependency>();
        addSelf(all, devMode);
        if(dependencies != null) {
            for(Dependency d : dependencies) {
                all.addAll(d.getDependencies(devMode));
            }
        }
        return all;
    }

    protected void addSelf(List<Dependency> all, boolean devMode) {
        all.add(this);
    }

    public String getPath() {
        if(path == null) {
            return null;
        }
        String parentPath = getParentPath();
        if(parentPath != null && path.charAt(0) != '/') {
            return parentPath + path;
        }
        return path;
    }

    protected String getParentPath() {
        if(parent == null) {
            return null;
        }
        String parentPath = parent.getPath();
        if(parentPath != null && !parentPath.endsWith("/")) {
            return parentPath + "/";
        }
        return parentPath;
    }

    public abstract String asString(String path);

    public void setParent(Dependency parent) {
        this.parent = parent;
    }

    protected Dependency getParent() {
        return parent;
    }
}
