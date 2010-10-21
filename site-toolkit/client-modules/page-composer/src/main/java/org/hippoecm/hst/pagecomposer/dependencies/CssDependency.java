package org.hippoecm.hst.pagecomposer.dependencies;

public class CssDependency extends BaseDependency implements Dependency {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    public CssDependency(String path) {
        super(path);
    }

    @Override
    public String asString(String path) {
        return "<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"" + path + "\"/>";
    }
}
