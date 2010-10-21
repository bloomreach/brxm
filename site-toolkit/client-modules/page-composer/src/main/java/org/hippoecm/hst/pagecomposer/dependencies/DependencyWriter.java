package org.hippoecm.hst.pagecomposer.dependencies;

public interface DependencyWriter {
    final static String SVN_ID = "$Id$";

    void write(Dependency dependency);

    String parse(String path);
}
