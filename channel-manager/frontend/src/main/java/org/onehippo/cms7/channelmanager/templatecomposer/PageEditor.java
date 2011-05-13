package org.onehippo.cms7.channelmanager.templatecomposer;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;

@ExtClass("Hippo.App.PageEditor")
public class PageEditor extends ExtComponent {

    @ExtProperty
    public String debug = "false";

    @ExtProperty
    public String iframeUrl = "/site/manager";

    public PageEditor(String id) {
        super(id);
        // TODO refactor and cleanup js dependencies
        add(CSSPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PageEditor.css"));

        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/miframe/miframe.js"));
        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/miframe/modules/mifmsg.js"));

        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/floatingwindow/FloatingWindow.js"));

        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/baseapp/BaseApp.js"));

        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/basegrid/BaseGrid.js"));

        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/colorfield/colorfield.js"));

        add(CSSPackageResource.getHeaderContribution(PageEditor.class, "plugins/colorfield/colorfield.css"));

        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/globals-debug.js"));
        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PropertiesPanel-debug.js"));
        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PageModel-debug.js"));
        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PageEditor-debug.js"));

//        if (Application.get().getConfigurationType().equals(Application.DEVELOPMENT)) {
//
//        } else {
//
//        }

    }

    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(" Ext.namespace(\"%s\"); window.%s = new %s(%s); \n", getMarkupId(), getMarkupId(), extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.App.Main";
    }

}
