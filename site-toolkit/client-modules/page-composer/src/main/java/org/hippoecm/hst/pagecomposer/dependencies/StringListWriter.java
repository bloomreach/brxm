package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.List;

public class StringListWriter extends StringWriter {

    private List<String> dependencies;

    public StringListWriter(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String parse(String path) {
        return path;
    }

    @Override
    protected void write(String src) {
        if(!dependencies.contains(src)) {
            dependencies.add(src);
        }
    }
}
