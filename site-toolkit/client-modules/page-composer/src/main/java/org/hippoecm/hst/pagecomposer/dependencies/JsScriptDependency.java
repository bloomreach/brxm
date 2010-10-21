package org.hippoecm.hst.pagecomposer.dependencies;

public abstract class JsScriptDependency extends BaseDependency implements Dependency {
    final static String SVN_ID = "$Id$";

    public JsScriptDependency(String path) {
        super(path);
    }

    @Override
    public String asString(String path) {
        return "<script type=\"text/javascript\">" + getScript(path) + "</script>";
    }

    protected abstract String getScript(String path);
}
