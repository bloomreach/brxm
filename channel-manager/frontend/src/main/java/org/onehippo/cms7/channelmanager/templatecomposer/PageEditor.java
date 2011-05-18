package org.onehippo.cms7.channelmanager.templatecomposer;

import java.util.Arrays;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtBundle;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;

@ExtClass("Hippo.App.PageEditor")
public class PageEditor extends ExtComponent {

    @ExtProperty
    public String debug = "false";

    @ExtProperty
    public String iframeUrl = "/site/manager";

    @ExtProperty
    public String rootComponentIdentifier;

    @ExtProperty
    public String toolkitIdentifier;

    @ExtProperty
    public String siteIdentifier;

    public PageEditor(final String id, final IPluginConfig config) {
        super(id);

        this.iframeUrl = config.getString("iframeUrl", "/site/manager");
        this.rootComponentIdentifier = config.getString("rootComponentIdentifier", "");
        this.toolkitIdentifier = config.getString("toolkitIdentifier", "");
        this.siteIdentifier = config.getString("siteIdentifier", "");

        // TODO refactor and cleanup js dependencies
        //        if (Application.get().getConfigurationType().equals(Application.DEVELOPMENT)) {
        //
        //        } else {
        //
        //        }
        add(CSSPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PageEditor.css"));

        add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/miframe/miframe-debug.js"));
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

    }

    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(" Ext.namespace(\"%s\"); window.%s = new %s(%s); \n", getMarkupId(), getMarkupId(), extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.App.Main";
    }

    @Override
    protected void onRenderProperties(final JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        // TODO refactor and cleanup js dependencies
        //        if (Application.get().getConfigurationType().equals(Application.DEVELOPMENT)) {
        //
        //        } else {
        //
        //        }
        RequestCycle rc = RequestCycle.get();
        properties.put("iFrameCssHeadContributions", Arrays.asList(
            rc.urlFor(new ResourceReference(PageEditor.class, "pageeditor/PageEditor.css")).toString())
        );
        properties.put("iFrameJsHeadContributions", Arrays.asList(
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/jquery/core/jquery-1.4.3.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/jquery/ui/jquery-ui-1.8.5.min.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/jquery/ui/custom/jquery.ui.sortable.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/jquery/plugins/jquery.class.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/jquery/plugins/jquery.namespace.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "pageeditor/globals-debug.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/util.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/factory.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/manager.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/widgets.js")).toString(),
            rc.urlFor(new ResourceReference(PageEditor.class, "iframe/main.js")).toString()
        ));
    }
}
