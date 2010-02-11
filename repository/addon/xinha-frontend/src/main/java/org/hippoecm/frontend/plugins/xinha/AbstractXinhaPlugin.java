/*
 *  Copyright 2008 Hippo.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WicketURLEncoder;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.XinhaHtmlProcessor.ILinkDecorator;
import org.hippoecm.frontend.plugins.xinha.dialog.XinhaDialogBehavior;
import org.hippoecm.frontend.plugins.xinha.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.xinha.dialog.links.ExternalLinkBehavior;
import org.hippoecm.frontend.plugins.xinha.dialog.links.InternalLinkBehavior;
import org.hippoecm.frontend.plugins.xinha.dragdrop.XinhaDropBehavior;
import org.hippoecm.frontend.plugins.xinha.json.JsonParser;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImageService;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLinkService;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.render.HeaderContributorHelper;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractXinhaPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractXinhaPlugin.class);

    public static final String XINHA_PARAM_PREFIX = "xinha-param-prefix-";
    private static final String BINARIES_PREFIX = "binaries";

    private static final String[] defaultFormatBlock = { "h1", "h2", "h3", "h4", "h5", "h6", "p", "address", "pre" };

    private static final ResourceReference XINHA_MODAL_JS = new JavascriptResourceReference(XinhaDialogBehavior.class,
            "xinha-modal.js");

    private static final PackagedTextTemplate XINHA_INIT_GLOBALS = new PackagedTextTemplate(AbstractXinhaPlugin.class,
            "xinha_init.js");

    private final String mode;
    private XinhaTextArea editor;
    private Configuration configuration;

    //preview behaviors
    private PreviewLinksBehavior previewLinksBehavior;
    private IBehavior startEditorBehavior;
    private IBehavior previewStyle;

    //editor behaviors
    private InternalLinkBehavior linkPickerBehavior;
    private ExternalLinkBehavior externalLinkBehavior;
    private ImagePickerBehavior imagePickerBehavior;

    private XinhaImageService imageService;
    private XinhaLinkService linkService;

    private JcrNodeModel nodeModel;

    public AbstractXinhaPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        configuration = new Configuration(config);
        context.registerService(configuration, Configuration.class.getName());

        String binariesPath = BINARIES_PREFIX + getNodePath();
        configuration.addProperty("prefix", XinhaUtil.encodeResourceURL(XinhaUtil.encode(binariesPath) + "/"));
        configuration.addProperty("isPortletContext", Boolean.toString(XinhaUtil.isPortletContext()));
        configuration.setName(getMarkupId());

        mode = config.getString("mode", "view");
        if (mode.equals("edit")) {
            add(new EditorManagerBehavior(YuiPluginHelper.getManager(context)));
        }
        load();

        // dialog functionality for plugins
        add(JavascriptPackageResource.getHeaderContribution(XINHA_MODAL_JS));
    }

    private void load() {
        addOrReplace("edit".equals(mode) ? configuration.getEditorStarted() ? createEditor("fragment")
                : createEditablePreview("fragment") : createPreview("fragment"));
    }

    private Fragment createPreview(String fragmentId) {
        if (previewLinksBehavior == null) {
            add(previewLinksBehavior = new PreviewLinksBehavior());
        }

        Fragment fragment = new Fragment(fragmentId, "view", this);
        fragment.add(new WebMarkupContainer("value", getValueModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                String text = (String) getDefaultModelObject();
                if (text != null) {
                    String processed = XinhaHtmlProcessor.prefixImageLinks(text, configuration.getProperty("prefix"));
                    processed = XinhaHtmlProcessor.decorateLinks(processed, previewLinksBehavior);
                    replaceComponentTagBody(markupStream, openTag, processed);
                } else {
                    renderComponentTagBody(markupStream, openTag);
                }
            }
        });
        return fragment;
    }

    private Fragment createEditablePreview(String fragmentId) {
        add(startEditorBehavior = new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                configuration.setEditorStarted(true);
                configuration.setFocusAfterLoad(true);
                target.addComponent(AbstractXinhaPlugin.this);
                load();
            }
        });
        add(previewStyle = new AttributeAppender("class", true, new Model<String>("rte-preview-area"), " "));
        return createPreview(fragmentId);
    }

    private Fragment createEditor(String fragmentId) {
        //remove preview behaviors
        if (previewStyle != null) {
            remove(previewStyle);
            previewStyle = null;
        }
        if (startEditorBehavior != null) {
            remove(startEditorBehavior);
            startEditorBehavior = null;
        }
        if (previewLinksBehavior != null) {
            remove(previewLinksBehavior);
            previewLinksBehavior = null;
        }

        Fragment fragment = new Fragment(fragmentId, "edit", this);
        fragment.add(editor = new XinhaTextArea("value", new XinhaModel(getValueModel())));
        configuration.setTextareaName(editor.getMarkupId());

        JcrNodeModel nodeModel = new JcrNodeModel(getValueModel().getJcrPropertymodel().getItemModel().getParentModel());
        imageService = new XinhaImageService(nodeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getXinhaName() {
                return configuration.getName();
            }

        };

        linkService = new XinhaLinkService(nodeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getXinhaName() {
                return configuration.getName();
            }

        };

        IPluginContext context = getPluginContext();
        IPluginConfig config = getPluginConfig();

        editor.add(imagePickerBehavior = new ImagePickerBehavior(context, config
                .getPluginConfig("Xinha.plugins.InsertImage"), imageService));
        editor.add(linkPickerBehavior = new InternalLinkBehavior(context, config
                .getPluginConfig("Xinha.plugins.CreateLink"), linkService));
        editor.add(externalLinkBehavior = new ExternalLinkBehavior(context, config));

        add(new XinhaDropBehavior(context, config) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void insertImage(JcrNodeModel model, AjaxRequestTarget target) {
                String returnScript = imageService.attach(model);
                if (returnScript != null) {
                    target.getHeaderResponse().renderOnDomReadyJavascript(returnScript);
                }
            }

            @Override
            protected void updateImage(JcrNodeModel model, AjaxRequestTarget target) {
                //TODO: check if old image facet select should be deleted
                insertImage(model, target);
            }

            @Override
            protected void insertLink(JcrNodeModel model, AjaxRequestTarget target) {
                String returnScript = linkService.attach(model);
                if (returnScript != null) {
                    target.getHeaderResponse().renderOnDomReadyJavascript(returnScript);
                }
            }

            @Override
            protected void updateLink(JcrNodeModel model, AjaxRequestTarget target) {
                //TODO: check if old link facet select should be deleted
                insertLink(model, target);
            }
        });
        return fragment;
    }

    private String getNodePath() {
        return getValueModel().getJcrPropertymodel().getItemModel().getParentModel().getPath();
    }

    protected abstract JcrPropertyValueModel getValueModel();

    /**
     * Callback urls aren't known at construction so set them here
     */
    @Override
    public void onBeforeRender() {
        if (configuration != null && configuration.getEditorStarted()) {
            configuration.addProperty("callbackUrl", editor.getCallbackUrl());

            //TODO: add enum to distinguish sorts of drops available
            if (configuration.getPluginConfiguration("InsertImage") != null) {
                configuration.getPluginConfiguration("InsertImage").addProperty("callbackUrl",
                        imagePickerBehavior.getCallbackUrl().toString());
            }

            if (configuration.getPluginConfiguration("CreateLink") != null) {
                configuration.getPluginConfiguration("CreateLink").addProperty("callbackUrl",
                        linkPickerBehavior.getCallbackUrl().toString());
            }

            if (configuration.getPluginConfiguration("CreateExternalLink") != null) {
                configuration.getPluginConfiguration("CreateExternalLink").addProperty("callbackUrl",
                        externalLinkBehavior.getCallbackUrl().toString());
            }
        }
        super.onBeforeRender();
    }

    @Override
    protected void onDetach() {
        if (imageService != null) {
            imageService.detach();
        }
        if (linkService != null) {
            linkService.detach();
        }
        if (nodeModel != null) {
            nodeModel.detach();
        }
        super.onDetach();
    }

    protected String clean(final String value) throws Exception {
        if (value != null) {
            IHtmlCleanerService cleaner = getPluginContext().getService(IHtmlCleanerService.class.getName(),
                    IHtmlCleanerService.class);
            if (cleaner != null) {
                return cleaner.clean(value);
            }
        }
        return value;
    }

    private void removeLinks(String text) {
        Set<String> linkNames = XinhaHtmlProcessor.getInternalLinks(text);
        linkService.cleanup(linkNames);
    }

    private class XinhaModel implements IModel {
        private static final long serialVersionUID = 1L;

        private JcrPropertyValueModel valueModel;

        public XinhaModel(JcrPropertyValueModel valueModel) {
            this.valueModel = valueModel;
        }

        public Object getObject() {
            return valueModel.getObject();
        }

        public void setObject(Object value) {
            try {
                String cleanedValue = clean((String) value);
                if (cleanedValue != null) {
                    removeLinks(cleanedValue);
                }
                valueModel.setObject(cleanedValue);
            } catch (Exception e) {
                error(new ResourceModel("error-while-cleaning-conent", "An error occured while cleaning the content"));
                log.error("Exception caught during editor creation while cleaning value: " + value, e);
            }
        }

        public void detach() {
            valueModel.detach();
        }
    }

    private class XinhaTextArea extends TextArea {
        private static final long serialVersionUID = 1L;

        private AbstractAjaxBehavior callback;

        public XinhaTextArea(String id, IModel model) {
            super(id, model);

            setOutputMarkupId(true);
            setVisible(true);
            setMarkupId("xinha" + Integer.valueOf(Session.get().nextSequenceValue()));

            add(callback = new AbstractDefaultAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                public void respond(AjaxRequestTarget target) {
                    processInput();
                }
            });
        }

        public String getCallbackUrl() {
            return callback.getCallbackUrl().toString();
        }

        @Override
        protected void onComponentTag(final ComponentTag tag) {
            StringBuilder sb = new StringBuilder();
            String width = getPluginConfig().getString("width", "1px");
            sb.append("width: ");
            sb.append(width);
            sb.append(";");

            String height = getPluginConfig().getString("height", "1px");
            sb.append("height: ");
            sb.append(height);
            sb.append(";");

            sb.append("display: none;");
            tag.put("style", sb.toString());
            super.onComponentTag(tag);
        }

    }

    class PreviewLinksBehavior extends AbstractDefaultAjaxBehavior implements ILinkDecorator {
        private static final long serialVersionUID = 1L;

        private static final String JS_STOP_EVENT = "Wicket.stopEvent(event);";

        
        @Override
        protected void respond(AjaxRequestTarget target) {
            Request request = RequestCycle.get().getRequest();
            String link = request.getParameter("link");
            if (link != null) {
                IBrowseService browser = getPluginContext().getService(
                        getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
                if (browser != null) {
                    JcrNodeModel model = (JcrNodeModel) getModel();
                    Node node = model.getNode();
                    try {
                        if (node.hasNode(link)) {
                            node = node.getNode(link);
                            if (node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                                String uuid = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                                javax.jcr.Session s = ((UserSession) getSession()).getJcrSession();
                                node = s.getNodeByUUID(uuid);
                                browser.browse(new JcrNodeModel(node));
                            }
                        }
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public String internalLink(String link) {
            String url = getCallbackUrl(false) + "&link=" + WicketURLEncoder.QUERY_INSTANCE.encode(link);
            return "href=\"#\" onclick=\"" + JS_STOP_EVENT + generateCallbackScript("wicketAjaxGet('" + url + "'") + "\"";
        }

        public String externalLink(String link) {
            return "href=\"" + link + "\" onclick=\"" + JS_STOP_EVENT + "\"";
        }
    }

    class EditorManagerBehavior extends AbstractYuiBehavior {
        private static final long serialVersionUID = 1L;

        DynamicTextTemplate globals;
        DynamicTextTemplate register;

        public EditorManagerBehavior(IYuiManager manager) {
            super(manager);

            globals = new DynamicTextTemplate(XINHA_INIT_GLOBALS) {
                private static final long serialVersionUID = 1L;

                @Override
                protected Map<String, Object> getVariables() {
                    final Page page = getPluginContext().getService(Home.class.getName(), Home.class);
                    String url = HeaderContributorHelper.getFixedRelativePathPrefixToContextRoot() + "xinha/xinha/";
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
            context.addOnWinLoad("YAHOO.hippo.EditorManager.renderAll();");
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
        StringBuilder sb = new StringBuilder(120);
        sb.append("{ ");
        sb.append("name: ").append(JsonParser.serialize2JS(configuration.getName()));
        sb.append(", ");
        sb.append("textarea: ").append(JsonParser.serialize2JS(configuration.getTextareaName()));
        sb.append(", ");
        sb.append("started: ").append(configuration.getEditorStarted());
        sb.append(", ");
        sb.append("focus: ").append(configuration.getFocusAfterLoad());
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
        sb.append(" }");
        return sb.toString();
    }

    class Configuration extends BaseConfiguration {
        private static final long serialVersionUID = 1L;

        private static final String XINHA_PLUGINS = "Xinha.plugins";
        private static final String XINHA_TOOLBAR = "Xinha.config.toolbar";
        private static final String XINHA_CSS = "Xinha.config.css";
        private static final String XINHA_SKIN = "Xinha.skin";
        private static final String XINHA_FORMAT = "Xinha.format";

        private final Map<String, PluginConfiguration> pluginConfigurations = new HashMap<String, PluginConfiguration>();
        private final List<String> toolbarItems;
        private final List<String> styleSheets;
        private final String skin;

        private final List<String> formatBlock;

        private String textareaName;

        //flag if editor should be focused after load
        private boolean focusAfterLoad;

        //flag if the editor has started
        private boolean editorStarted;

        public Configuration(IPluginConfig config) {
            addProperty("xinhaParamToken", XINHA_PARAM_PREFIX);

            toolbarItems = new ArrayList<String>();
            String[] values = config.getStringArray(XINHA_TOOLBAR);
            if (values != null) {
                for (String item : values) {
                    toolbarItems.add(item);
                }
            }

            styleSheets = new ArrayList<String>();
            values = config.getStringArray(XINHA_CSS);
            if (values != null) {
                for (String item : values) {
                    styleSheets.add(item);
                }
            }

            formatBlock = new LinkedList<String>();
            String[] formatInput = config.containsKey(XINHA_FORMAT) ? config.getStringArray(XINHA_FORMAT)
                    : defaultFormatBlock;
            for (String f : formatInput) {
                if (!formatBlock.contains(f)) {
                    formatBlock.add(f);
                }
            }

            skin = config.getString(XINHA_SKIN);

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
                        return name.equals(((Configuration) o).getName());
                    } else {
                        return ((Configuration) o).getName() == null;
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
