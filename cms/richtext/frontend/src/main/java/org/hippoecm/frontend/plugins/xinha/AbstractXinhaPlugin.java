/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.xinha;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.RichTextArea;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;
import org.hippoecm.frontend.plugins.xinha.behavior.StateChangeBehavior;
import org.hippoecm.frontend.plugins.xinha.dialog.XinhaDialogBehavior;
import org.hippoecm.frontend.plugins.xinha.dialog.links.ExternalLinkBehavior;
import org.hippoecm.frontend.plugins.xinha.json.JsonParser;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class  AbstractXinhaPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractXinhaPlugin.class);

    private static final String[] defaultFormatBlock = { "h1", "h2", "h3", "h4", "h5", "h6", "p", "address", "pre" };

    private static final ResourceReference XINHA_MODAL_JS = new JavaScriptResourceReference(XinhaDialogBehavior.class,
            "xinha-modal.js");
    private static final ResourceReference XINHA_TOOLS_JS = new JavaScriptResourceReference(AbstractXinhaPlugin.class,
            "xinha-tools.js");
    private static final ResourceReference XINHA_TOOLS_DEV_JS = new JavaScriptResourceReference(AbstractXinhaPlugin.class,
            "xinha-tools-dev.js");

    private static final ResourceReference XINHA_SKIN = new CssResourceReference(AbstractXinhaPlugin.class, "xinha.css");
    private static final CssResourceReference DIFF_SKIN = new CssResourceReference(HtmlDiffModel.class, "diff.css");

    private final PackageTextTemplate XINHA_INIT_GLOBALS = new PackageTextTemplate(AbstractXinhaPlugin.class,
            "xinha_init.js");

    public static final String DISABLE_OPEN_IN_A_NEW_WINDOW_CONFIG = "open.in.new.window.disabled";

    private final IEditor.Mode mode;
    protected RichTextArea editor;
    protected final Configuration configuration;

    //preview behaviors
    private Behavior startEditorBehavior;
    private Behavior previewStyle;
    private Behavior tabIndex;

    //editor behaviors
    private Map<String, Behavior> editorPluginBehaviors;

    private JcrNodeModel nodeModel;

    public AbstractXinhaPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        configuration = new Configuration(config);
        configuration.setName(getMarkupId());

        mode = IEditor.Mode.fromString(config.getString("mode", "view"));
        if (IEditor.Mode.EDIT == mode) {
            configuration.addProperty("previewTooltipText", getString("preview.tooltip", null, "Click to edit"));
            add(new EditorManagerBehavior());
        }
    }

    @Override
    protected void onStart() {
        load();
        super.onStart();
    }

    protected IEditor.Mode getMode() {
        return mode;
    }

    private void load() {
        editorPluginBehaviors = new HashMap<String, Behavior>();

        addOrReplace(IEditor.Mode.EDIT == mode ? configuration.getEditorStarted() ? createEditor("fragment")
                : createEditablePreview("fragment") : createPreview("fragment"));
    }

    protected abstract JcrPropertyValueModel getValueModel();

    protected abstract JcrPropertyValueModel getBaseModel();

    protected IModel<String> newCompareModel() {
        JcrPropertyValueModel baseModel = getBaseModel();
        if (baseModel != null) {
            return new HtmlDiffModel(new StripScriptModel(baseModel), new StripScriptModel(getValueModel()));
        } else {
            return newViewModel();
        }
    }

    protected IModel<String> newViewModel() {
        return new StripScriptModel(getValueModel());
    }

    protected IModel<String> newEditModel() {
        return new RichTextModel(getValueModel());
    }

    protected Fragment createPreview(String fragmentId) {
        Fragment fragment = new Fragment(fragmentId, "view", this);
        IModel<String> model;
        switch (mode) {
        case COMPARE:
            model = newCompareModel();
            break;
        case VIEW:
        case EDIT:
            model = newViewModel();
            break;
        default:
            throw new RuntimeException("No model available for mode " + mode);
        }
        fragment.add(new WebMarkupContainer("value", model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                String text = (String) getDefaultModelObject();
                if (text != null) {
                    replaceComponentTagBody(markupStream, openTag, text);
                } else {
                    super.onComponentTagBody(markupStream, openTag);
                }
            }
        });
        return fragment;
    }

    protected Fragment createEditablePreview(String fragmentId) {
        String event = "onfocus";
        WebClientInfo info = getWebSession().getClientInfo();
        if (info.getProperties().isBrowserInternetExplorer()) {
            event = "onactivate";
        }
        add(startEditorBehavior = new AjaxEventBehavior(event) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                configuration.setEditorStarted(true);
                configuration.setFocusAfterLoad(true);
                target.add(AbstractXinhaPlugin.this);
                target.focusComponent(editor);
                load();
            }
        });
        add(previewStyle = new AttributeAppender("class", true, new Model<String>("rte-preview-area"), " "));
        add(tabIndex = new AttributeAppender("tabindex", true, new Model<String>("0"), " "));
        return createPreview(fragmentId);
    }

    protected Fragment createEditor(String fragmentId) {
        //remove preview behaviors
        if (previewStyle != null) {
            remove(previewStyle);
            previewStyle = null;
        }
        if (tabIndex != null) {
            remove(tabIndex);
            tabIndex = null;
        }
        if (startEditorBehavior != null) {
            remove(startEditorBehavior);
            startEditorBehavior = null;
        }

        Fragment fragment = new Fragment(fragmentId, "edit", this);
        fragment.add(editor = new RichTextArea("value", newEditModel()));
        editor.setWidth(getPluginConfig().getString("width", "1px"));
        editor.setHeight(getPluginConfig().getString("height", "1px"));
        configuration.setTextareaName(editor.getMarkupId());

        createEditorPluginBehaviors();

        for (Behavior behavior : editorPluginBehaviors.values()) {
            editor.add(behavior);
        }

        return fragment;
    }

    protected void createEditorPluginBehaviors() {
        editorPluginBehaviors.put("CreateExternalLink",
                new ExternalLinkBehavior(getPluginContext(), getPluginConfig()));

        editorPluginBehaviors.put("StateChange", new StateChangeBehavior(configuration) {

            @Override
            protected void onStateChanged(final String param, final boolean value, final AjaxRequestTarget target) {
                if (ACTIVATED.equals(param) && !value) {
                    load();
                    String script = String.format("YAHOO.hippo.EditorManager.deactivateEditor('%s');",
                            configuration.getName());
                    target.prependJavaScript(script);
                    target.add(AbstractXinhaPlugin.this);
                }
            }
        });
    }

    /**
     * Callback urls aren't known at construction so set them here
     */
    @Override
    public void onBeforeRender() {
        if (configuration != null && configuration.getEditorStarted()) {
            configuration.addProperty("callbackUrl", editor.getCallbackUrl());

            for (String plugin : editorPluginBehaviors.keySet()) {
                Behavior behavior = editorPluginBehaviors.get(plugin);
                if (behavior instanceof AbstractAjaxBehavior) {
                    PluginConfiguration config = configuration.getPluginConfiguration(plugin);
                    if (config == null) {
                        config = new PluginConfiguration(plugin);
                    }
                    config.addProperty("callbackUrl", ((AbstractAjaxBehavior) behavior).getCallbackUrl().toString());

                    configuration.addPluginConfiguration(config);
                }
            }
        }
        super.onBeforeRender();
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        // dialog functionality for plugins
        response.render(JavaScriptHeaderItem.forReference(XINHA_MODAL_JS));
        response.render(JavaScriptHeaderItem.forReference(XINHA_TOOLS_JS));
        if (getApplication().getConfigurationType().equals(RuntimeConfigurationType.DEPLOYMENT)) {
            response.render(JavaScriptHeaderItem.forReference(XINHA_TOOLS_DEV_JS));
        }

        response.render(CssHeaderItem.forReference(XINHA_SKIN));
        response.render(CssHeaderItem.forReference(DIFF_SKIN));
    }

    @Override
    protected void onDetach() {
        if (nodeModel != null) {
            nodeModel.detach();
        }
        super.onDetach();
    }

    class EditorManagerBehavior extends AbstractYuiBehavior {
        private static final long serialVersionUID = 1L;

        DynamicTextTemplate globals;
        DynamicTextTemplate register;

        public EditorManagerBehavior() {
            globals = new DynamicTextTemplate(XINHA_INIT_GLOBALS) {
                private static final long serialVersionUID = 1L;

                @Override
                protected Map<String, Object> getVariables() {
                    final Page page = getPluginContext().getService(Home.class.getName(), Home.class);
                    String url = RequestCycle.get().getUrlRenderer().renderContextRelativeUrl("xinha/");
                    String lang = page.getLocale().getLanguage();
                    String skin = configuration.getSkin();

                    Map<String, Object> map = super.getVariables();
                    map.put("editorUrl", url);
                    map.put("editorLang", lang);
                    map.put("editorSkin", skin);
                    return map;
                }
            };
        }

        @Override
        public void addHeaderContribution(IYuiContext context) {
            context.addModule(XinhaNamespace.NS, "editormanager");
            context.addTemplate(globals);
            context.addOnDomLoad(new AbstractReadOnlyModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    return "YAHOO.hippo.EditorManager.register(" + parseConfiguration(configuration) + ");";
                }

            });
            context.addOnDomLoad("YAHOO.hippo.EditorManager.render();");
        }
    }

    /**
     * Construct a Javascript object literal that represents the configuration of the Xinha editor.
     * An example of the returned object is
     * {
     *   name: 'XinhaPlugin1',
     *   textarea: 'xinha1',
     *   started: false,
     *   focus: true,
     *   properties: [{key: 'callbackUrl', value: '...'}]},
     *   plugins: ['AutoSave', 'table'],
     *   pluginProperties: [{ name: 'AutoSave', values [{key: 'timeout', value: 200}]}],
     *   toolbars: ['createlink', 'bold'],
     *   styleSheets: ['../../skin/skin.css'],
     *   formatBlock: {'h1': 'Heading 1', 'pre': 'Formatted'}
     * }
     * @param configuration
     * @return
     */
    protected String parseConfiguration(Configuration configuration) {
        StringBuilder sb = new StringBuilder(200);
        sb.append("{ ");
        sb.append("name: ").append(JsonParser.serialize2JS(configuration.getName()));
        sb.append(", ");
        sb.append("textarea: ").append(JsonParser.serialize2JS(configuration.getTextareaName()));
        sb.append(", ");
        sb.append("started: ").append(configuration.getEditorStarted());
        sb.append(", ");
        sb.append("focus: ").append(configuration.getFocusAfterLoad());
        sb.append(", ");
        sb.append("fullscreen: ").append(configuration.isRenderFullscreen());
        sb.append(", ");
        sb.append("properties: ").append(JsonParser.asKeyValueArray(configuration.getProperties()));
        sb.append(", ");
        sb.append("plugins: ").append(JsonParser.asArray(configuration.getPluginConfigurations()));
        sb.append(", ");
        sb.append("pluginProperties: ").append(
                JsonParser.parsePluginConfiguration(configuration.getPluginConfigurations()));
        sb.append(", ");
        sb.append("toolbars: ").append(JsonParser.asArray(configuration.getToolbarItems()));
        sb.append(", ");
        sb.append("styleSheets: ").append(JsonParser.asArray(configuration.getStyleSheets()));
        sb.append(", ");
        sb.append("formatBlock: ").append(
                JsonParser.asDictionary(configuration.getFormatBlock(), true, true, "xinha.formatblock.", this));
        sb.append(", ");
        sb.append("getHtmlMethod: ").append(JsonParser.serialize2JS(configuration.getHtmlMethod()));
        sb.append(", ");
        sb.append("convertUrlsToLinks: ").append(configuration.getConvertUrlsToLinks());
        sb.append(", ");
        sb.append("flowToolbars: ").append(configuration.getFlowToolbars());
        sb.append(", ");
        sb.append("killWordOnPaste: ").append(configuration.getKillWordOnPaste());
        sb.append(", ");
        sb.append("showLoading: ").append(configuration.getShowLoading());
        sb.append(", ");
        sb.append("statusBar: ").append(configuration.getStatusBar());
        sb.append(", ");
        sb.append("only7BitPrintablesInURLs: ").append(configuration.getOnly7BitPrintablesInURLs());
        sb.append(", ");
        sb.append("width: ").append(configuration.getWidth());
        sb.append(", ");
        sb.append("height: ").append(configuration.getHeight());
        sb.append(" }");
        return sb.toString();
    }

    public class Configuration extends BaseConfiguration {
        private static final long serialVersionUID = 1L;

        private static final String XINHA_PLUGINS = "Xinha.plugins";
        private static final String XINHA_TOOLBAR = "Xinha.config.toolbar";
        private static final String XINHA_FORMAT = "Xinha.format";
        private static final String XINHA_CSS = "Xinha.config.css";
        private static final String XINHA_SKIN = "Xinha.skin";
        private static final String XINHA_GET_HTML_METHOD = "Xinha.getHtmlMethod";
        private static final String XINHA_CONVERT_URLS_TO_LINKS = "Xinha.convertUrlsToLinks";
        private static final String XINHA_FLOW_TOOLBARS = "Xinha.flowToolbars";
        private static final String XINHA_KILL_WORD_ON_PASTE = "Xinha.killWordOnPaste";
        private static final String XINHA_SHOW_LOADING = "Xinha.showLoading";
        private static final String XINHA_STATUS_BAR = "Xinha.statusBar";
        private static final String XINHA_ONLY7BIT_PRINTABLE_URLS = "Xinha.only7BitPrintablesInURLs";

        private final Map<String, PluginConfiguration> pluginConfigurations = new HashMap<String, PluginConfiguration>();

        private String textareaName;

        private boolean focusAfterLoad;
        private boolean editorStarted;
        private boolean renderFullscreen;

        //Xinha built-in options
        private final String skin;

        private final List<String> toolbarItems;
        private final List<String> styleSheets;
        private final List<String> formatBlock;

        private final String getHtmlMethod;
        private final boolean convertUrlsToLinks;
        private final boolean flowToolbars;
        private final boolean killWordOnPaste;
        private final boolean showLoading;
        private final boolean statusBar;
        private final boolean only7BitPrintablesInURLs;

        private int width;
        private int height;

        public Configuration(IPluginConfig config) {
            skin = config.getString(XINHA_SKIN);
            getHtmlMethod = config.getString(XINHA_GET_HTML_METHOD, "TransformInnerHTML");
            convertUrlsToLinks = config.getAsBoolean(XINHA_CONVERT_URLS_TO_LINKS, false);
            flowToolbars = config.getAsBoolean(XINHA_FLOW_TOOLBARS, true);
            killWordOnPaste = config.getAsBoolean(XINHA_KILL_WORD_ON_PASTE, true);
            showLoading = config.getAsBoolean(XINHA_SHOW_LOADING, false);
            statusBar = config.getAsBoolean(XINHA_STATUS_BAR, false);
            only7BitPrintablesInURLs = config.getAsBoolean(XINHA_ONLY7BIT_PRINTABLE_URLS, false);
            width = config.getAsInteger("width", -1);
            height = config.getAsInteger("height", -1);

            toolbarItems = new ArrayList<String>();
            String[] values = config.getStringArray(XINHA_TOOLBAR);
            if (values != null) {
                toolbarItems.addAll(Arrays.asList(values));
            }

            styleSheets = new ArrayList<String>();
            values = config.getStringArray(XINHA_CSS);
            if (values != null) {
                styleSheets.addAll(Arrays.asList(values));
            }

            formatBlock = new LinkedList<String>();
            String[] formatInput = config.containsKey(XINHA_FORMAT) ? config.getStringArray(XINHA_FORMAT)
                    : defaultFormatBlock;
            for (String f : formatInput) {
                if (!formatBlock.contains(f)) {
                    formatBlock.add(f);
                }
            }

            values = config.getStringArray(XINHA_PLUGINS);
            if (values != null) {
                for (String pluginName : values) {
                    PluginConfiguration pluginConfig = new PluginConfiguration(pluginName);
                    String[] pluginProperties = config.getStringArray(pluginName);
                    if (pluginProperties != null) {
                        for (String pluginProperty : pluginProperties) {
                            pluginConfig.addProperty(pluginProperty);
                        }
                    }
                    addPluginConfiguration(pluginConfig);
                }
            }
        }

        /**
         * Only after clicking the preview area, should this editor be auto-focused. All other cases, like switching editor-tabs
         * should be handled by state on the client instead of on the server. Because of this true is only returned once.
         *
         * @return If the RTE should be auto-focused when created.
         */
        public boolean getFocusAfterLoad() {
            if (focusAfterLoad) {
                focusAfterLoad = false;
                return true;
            }
            return false;
        }

        public void setFocusAfterLoad(boolean set) {
            focusAfterLoad = set;
        }

        public boolean getEditorStarted() {
            return editorStarted;
        }

        public void setEditorStarted(boolean set) {
            this.editorStarted = set;
        }

        public boolean isRenderFullscreen() {
            return renderFullscreen;
        }

        public void setRenderFullscreen(final boolean renderFullscreen) {
            this.renderFullscreen = renderFullscreen;
        }

        public void setTextareaName(String name) {
            this.textareaName = name;
        }

        public String getTextareaName() {
            return textareaName;
        }

        public List<String> getFormatBlock() {
            return formatBlock;
        }

        public List<String> getToolbarItems() {
            return toolbarItems;
        }

        public List<String> getStyleSheets() {
            return styleSheets;
        }

        public String getSkin() {
            return skin;
        }

        public String getHtmlMethod() {
            return getHtmlMethod;
        }

        public boolean getConvertUrlsToLinks() {
            return convertUrlsToLinks;
        }

        public boolean getFlowToolbars() {
            return flowToolbars;
        }

        public boolean getKillWordOnPaste() {
            return killWordOnPaste;
        }

        public boolean getShowLoading() {
            return showLoading;
        }

        public boolean getStatusBar() {
            return statusBar;
        }

        public boolean getOnly7BitPrintablesInURLs() {
            return only7BitPrintablesInURLs;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public void addPluginConfiguration(PluginConfiguration config) {
            if (!pluginConfigurations.containsKey(config.getName())) {
                pluginConfigurations.put(config.getName(), config);
            } else {
                pluginConfigurations.get(config.getName()).addProperties(config.getProperties());
            }
        }

        public PluginConfiguration getPluginConfiguration(String name) {
            return pluginConfigurations.get(name);
        }

        public Set<PluginConfiguration> getPluginConfigurations() {
            Set<PluginConfiguration> returnSet = new HashSet<PluginConfiguration>();
            returnSet.addAll(pluginConfigurations.values());
            return returnSet;
        }
    }

    public class PluginConfiguration extends BaseConfiguration {

        private static final long serialVersionUID = 1L;

        public PluginConfiguration(String name) {
            setName(name);
        }

        public void addProperty(String keyValue) {
            int equalsIndex = keyValue.indexOf("=");
            if (equalsIndex == -1) {
                throw new IllegalArgumentException("Invalid key/value argument, no separator found: " + keyValue);
            }
            addProperty(keyValue.substring(0, equalsIndex), keyValue.substring(equalsIndex + 1));
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof PluginConfiguration) {
                PluginConfiguration that = (PluginConfiguration) o;
                if (that.getPlugin() == getPlugin()) {
                    String name = getName();
                    if (name != null) {
                        return name.equals(that.getName());
                    } else {
                        return that.getName() == null;
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return getPlugin().hashCode() ^ getName().hashCode();
        }
    }

    public class BaseConfiguration implements IClusterable {
        private static final long serialVersionUID = 1L;

        //id
        private String name = null;

        //properties
        private Map<String, String> properties = new HashMap<String, String>();

        public void setName(String name) {
            if (name != null) {
                this.name = name;
            }
        }

        public String getName() {
            return name;
        }

        //override all properties
        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        //merge properties
        public void addProperties(Map<String, String> properties) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                addProperty(entry.getKey(), entry.getValue());
            }
        }

        public void addProperty(String name, String value) {
            properties.put(name, value);
        }

        public String getProperty(String name) {
            return properties.get(name);
        }

        protected AbstractXinhaPlugin getPlugin() {
            return AbstractXinhaPlugin.this;
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return AbstractXinhaPlugin.this.hashCode();
        }
    }
}
