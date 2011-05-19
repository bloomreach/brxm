/**
  * Copyright 2011 Hippo
  *
  * Licensed under the Apache License, Version 2.0 (the  "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
*/
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
import org.onehippo.cms7.jquery.JQueryBundle;
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

    public PageEditor(final String id, final IPluginConfig config) {
        super(id);
        this.iframeUrl = config.getString("iframeUrl", "/site/manager");

        add(CSSPackageResource.getHeaderContribution(PageEditor.class, "plugins/colorfield/colorfield.css"));

        if (Application.get().getConfigurationType().equals(Application.DEVELOPMENT)) {
            this.debug = "true";
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/miframe/miframe-debug.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/miframe/modules/mifmsg.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/floatingwindow/FloatingWindow.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/baseapp/BaseApp.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/basegrid/BaseGrid.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/colorfield/colorfield.js"));

            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/globals-debug.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PropertiesPanel-debug.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PageModel-debug.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PageEditor-debug.js"));
         } else {
            this.debug = "false";
            // TODO minification
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/miframe/miframe-debug.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/miframe/modules/mifmsg.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/floatingwindow/FloatingWindow.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/baseapp/BaseApp.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/basegrid/BaseGrid.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "plugins/colorfield/colorfield.js"));

            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/globals-debug.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PropertiesPanel-debug.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PageModel-debug.js"));
            add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "pageeditor/PageEditor-debug.js"));
        }
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
        RequestCycle rc = RequestCycle.get();
        properties.put("iFrameCssHeadContributions", Arrays.asList(
            rc.urlFor(new ResourceReference(PageEditor.class, "pageeditor/PageEditor.css")).toString())
        );
        if (Application.get().getConfigurationType().equals(Application.DEVELOPMENT)) {
            properties.put("iFrameJsHeadContributions", Arrays.asList(
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CORE)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CLASS_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_NAMESPACE_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_HEAD_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_UI)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_UI_SORTABLE)).toString(),

                rc.urlFor(new ResourceReference(PageEditor.class, "pageeditor/globals-debug.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/util.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/factory.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/manager.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/widgets.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/main.js")).toString()
            ));
        } else {
            properties.put("iFrameJsHeadContributions", Arrays.asList(
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CORE_MIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CLASS_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_NAMESPACE_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_HEAD_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_UI_MIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_UI_SORTABLE)).toString(),

                // TODO minification
                rc.urlFor(new ResourceReference(PageEditor.class, "pageeditor/globals-debug.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/util.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/factory.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/manager.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/widgets.js")).toString(),
                rc.urlFor(new ResourceReference(PageEditor.class, "iframe/main.js")).toString()
            ));
        }
    }
}
