package org.hippoecm.hst.pagecomposer.dependencies;

public abstract class StringWriter implements DependencyWriter {

    public void write(Dependency dependency) {
        String path = dependency.getPath();
        if (path != null) {
            write(dependency.asString(parse(path)));
        }
    }

    protected abstract void write(String src);

}
