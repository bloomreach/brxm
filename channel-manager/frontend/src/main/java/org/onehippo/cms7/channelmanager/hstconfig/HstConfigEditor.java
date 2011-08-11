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
package org.onehippo.cms7.channelmanager.hstconfig;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.service.render.AbstractRenderService;
import org.hippoecm.hst.plugins.frontend.HstEditorPerspective;
import org.hippoecm.hst.plugins.frontend.ViewController;
import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtBoxComponent;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;

@ExtClass(HstConfigEditor.EXT_CLASS)
public class HstConfigEditor extends ExtPanel {

    public static final String HST_CONFIG_EDITOR_JS = "Hippo.ChannelManager.HstConfigEditor.js";
    public static final String EXT_CLASS = "Hippo.ChannelManager.HstConfigEditor";

    static final String EXTENSION_EDITOR = "extension.editor";
    static final String EXTENSION_NAVIGATOR = "extension.navigator";

    private WebMarkupContainer container;
    private Model<String> mountPointModel;

    public HstConfigEditor(final IPluginContext context, final IPluginConfig config) {
        add(JavascriptPackageResource.getHeaderContribution(HstConfigEditor.class, HST_CONFIG_EDITOR_JS));

        container = new WebMarkupContainer("hst-config-editor-container");
        container.setOutputMarkupId(true);

        // add stuff to container here
        mountPointModel = new Model<String>();
        Label label = new Label("label-mountpoint", mountPointModel);
        label.setRenderBodyOnly(false);
        label.setOutputMarkupId(true);
        container.add(label);

        // TODO: show HST config editor for the mount point in the model
        // HstEditorPerspective perspective = createPerspective(context, "hippogogreen", "/hst:hst/hst:sites/hippogogreen-live");
        // container.add(perspective);

        ExtBoxComponent box = new ExtBoxComponent();
        box.add(container);
        add(box);

        setOutputMarkupId(true);
    }

    private static HstEditorPerspective createPerspective(final IPluginContext context, String channelId, String mountPoint) {
        final String navigator = "${cluster.id}." + channelId + ".navigator";
        final String model = "${cluster.id}.browser." + channelId + ".model";

        IPluginConfig config = new JavaPluginConfig();
        config.put(HstEditorPerspective.NAMESPACES_ROOT, "hippo:namespaces/hst");
        config.put(HstEditorPerspective.EDITOR_ROOT, mountPoint);
        config.put(ViewController.MODEL, model);
        config.put(ViewController.VIEWERS, "hst-editor-views");
        config.put("cluster.name", "hst-editor-navigator");
        config.put(EXTENSION_EDITOR, channelId + ".editor");
        config.put(EXTENSION_NAVIGATOR, navigator);
        config.put(Perspective.TITLE, channelId);
        config.put("plugin.class", HstEditorPerspective.class.getName());
        config.put(ITranslateService.TRANSLATOR_ID, "hstconfigeditor.translator");
        config.put(AbstractRenderService.EXTENSIONS_ID, new String[]{EXTENSION_NAVIGATOR, EXTENSION_EDITOR});
        config.put(AbstractRenderService.WICKET_ID, "${cluster.id}.site");

        IPluginConfig clusterOptions = new JavaPluginConfig();
        clusterOptions.put(AbstractRenderService.MODEL_ID, model);
        clusterOptions.put(AbstractRenderService.WICKET_ID, navigator);
        clusterOptions.put(ITranslateService.TRANSLATOR_ID, "hstconfigeditor.translator");
        config.put("cluster.options", clusterOptions);

        IPluginConfig yuiConfig = new JavaPluginConfig();
        yuiConfig.put("left", "id=hst-perspective-left,body=hst-perspective-left-body,resize=true,scroll=false,minWidth=200,width=200,gutter=0px 0px 0px 0px");
        yuiConfig.put("center", "id=hst-perspective-center,body=hst-perspective-center-body,minWidth=400,scroll=true,gutter=0px 0px 0px 0px");
        yuiConfig.put("linked.with.parent", true);
        yuiConfig.put("root.id", "hst-perspective-wrapper");
        yuiConfig.put("units", new String[]{"left", "center"});
        config.put("yui.config", yuiConfig);

        return new HstEditorPerspective(context, config);
    }

    @Override
    protected void onRenderProperties(final JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
    }

    public void setMountPoint(AjaxRequestTarget target, String mountPoint) {
        mountPointModel.setObject("TODO: show HST config editor for '" + mountPoint + "'");
        target.addComponent(container);
    }

}
