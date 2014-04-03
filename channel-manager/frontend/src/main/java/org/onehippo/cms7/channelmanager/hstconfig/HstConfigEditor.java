/**
  * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.AbstractRenderService;
import org.hippoecm.hst.plugins.frontend.HstEditorPerspective;
import org.hippoecm.hst.plugins.frontend.ViewController;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ChannelManagerPerspective;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;

@ExtClass(HstConfigEditor.EXT_CLASS)
public class HstConfigEditor extends ExtPanel {

    public static final String EXT_CLASS = "Hippo.ChannelManager.HstConfigEditor.Container";
    private static final long serialVersionUID = 1L;

    static final String EXTENSION_EDITOR = "extension.editor";
    static final String EXTENSION_NAVIGATOR = "extension.navigator";
    static final String TRANSLATOR_SERVICE_ID = "service.hsteditor.translator";

    private IPluginContext context;
    private WebMarkupContainer container;
    private Model<String> mountPointModel;
    private IClusterControl configEditorControl;
    private final boolean lockInheritedConfig;

    public HstConfigEditor(final IPluginContext context, final boolean lockInheritedConfig) {
        this.context = context;
        this.lockInheritedConfig = lockInheritedConfig;
        final String title = getLocalizer().getString("edit-hst-configuration", this);
        setTitle(new Model<String>(title));

        add(new HstConfigEditorResourceBehaviour());

        container = new WebMarkupContainer("hst-config-editor-container");
        container.setOutputMarkupId(true);

        // add stuff to container here
        mountPointModel = new Model<String>();
        Label label = new Label("label-mountpoint", mountPointModel);
        label.setRenderBodyOnly(false);
        label.setOutputMarkupId(true);
        container.add(label);

        context.registerTracker(new ServiceTracker<IRenderService>(IRenderService.class) {
            @Override
            protected void onServiceAdded(final IRenderService service, final String name) {
                ChannelManagerPerspective cmp = getChannelManagerPerspective();
                cmp.addRenderService(service);
                service.bind(cmp, "label-mountpoint");
                container.replace(service.getComponent());
            }

            @Override
            protected void onRemoveService(final IRenderService service, final String name) {
                container.replace(new Label("label-mountpoint", mountPointModel));
                ChannelManagerPerspective cmp = getChannelManagerPerspective();
                cmp.removeRenderService(service);
                service.unbind();
            }
        }, HstConfigEditor.class.getName() + ".hst-editor");

        add(container);

        setOutputMarkupId(true);
    }

    private ChannelManagerPerspective getChannelManagerPerspective() {
        MarkupContainer parent = getParent();
        while (!(parent instanceof ChannelManagerPerspective) && parent != null) {
            parent = parent.getParent();
        }
        return (ChannelManagerPerspective) parent;
    }

    private static IClusterControl createPerspective(final IPluginContext context,
                                                     String channelId,
                                                     String mountPoint,
                                                     final boolean lockInheritedConfig) {
        JavaClusterConfig jcc = new JavaClusterConfig();

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
        config.put("lockInheritedConfig", lockInheritedConfig);
        config.put("plugin.class", HstEditorPerspective.class.getName());
        config.put(ITranslateService.TRANSLATOR_ID, TRANSLATOR_SERVICE_ID);
        config.put(AbstractRenderService.EXTENSIONS_ID, new String[]{EXTENSION_NAVIGATOR, EXTENSION_EDITOR});
        config.put(AbstractRenderService.WICKET_ID, HstConfigEditor.class.getName() + ".hst-editor");

        IPluginConfig clusterOptions = new JavaPluginConfig();
        clusterOptions.put(AbstractRenderService.MODEL_ID, model);
        clusterOptions.put(AbstractRenderService.WICKET_ID, navigator);
        clusterOptions.put(ITranslateService.TRANSLATOR_ID, TRANSLATOR_SERVICE_ID);
        config.put("cluster.options", clusterOptions);

        IPluginConfig yuiConfig = new JavaPluginConfig();
        yuiConfig.put("left", "id=hst-perspective-left,body=hst-perspective-left-body,resize=true,scroll=false,minWidth=200,width=200,gutter=0px 0px 0px 0px");
        yuiConfig.put("center", "id=hst-perspective-center,body=hst-perspective-center-body,minWidth=400,scroll=true,gutter=0px 0px 0px 0px");
        yuiConfig.put("linked.with.parent", false);
        yuiConfig.put("root.id", "hst-perspective-wrapper");
        yuiConfig.put("client.class.name", "Hippo.ChannelManager.ExtWireframe");
        yuiConfig.put("units", new String[]{"left", "center"});
        config.put("yui.config", yuiConfig);
        jcc.addPlugin(config);

        return context.newCluster(jcc, new JavaPluginConfig());
    }


    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(
            " try { " +
                "Ext.namespace(\"Hippo.ChannelManager.HstConfigEditor\"); "+
                "window.Hippo.ChannelManager.HstConfigEditor.Instance = new %s(%s); "+
            "} catch (e) { Ext.Msg.alert(e); }; \n",
                extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.ChannelManager.HstConfigEditor.Instance";
    }

    public void setMountPoint(AjaxRequestTarget target, String channelId, String mountPoint) {
        if (configEditorControl != null) {
            configEditorControl.stop();
        }
        configEditorControl = createPerspective(context, channelId, mountPoint, lockInheritedConfig);
        configEditorControl.start();
        target.add(container);
    }

}
