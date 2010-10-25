package org.hippoecm.hst.pagecomposer.dependencies.ext;

import org.hippoecm.hst.pagecomposer.dependencies.JsScriptDependency;

public class ExtAppBootstrap extends JsScriptDependency {

    private String editableUrl;
    private boolean debug;

    public ExtAppBootstrap(String editableUrl) {
        this(editableUrl, false);
    }

    public ExtAppBootstrap(String editableUrl, boolean debug) {
        super("");
        this.editableUrl = editableUrl;
        this.debug = debug;
    }

    @Override
    protected String getScript(String path) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ext.onReady(function() {");
        sb.append("\n\t var config = {");
        sb.append("\n\t\t debug: ").append(debug).append(",");
        sb.append("\n\t\t iframeUrl: '").append(editableUrl).append("',");
        sb.append("\n\t };");
        sb.append("\n\t Ext.namespace('Hippo.App');");
        sb.append("\n\t Hippo.App.Main = new Hippo.App.PageEditor(config);");
        sb.append("\n });");
        return sb.toString();
    }
}
