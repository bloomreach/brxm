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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WicketURLEncoder;
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

    private static final String BINARIES_PREFIX = "binaries";
    public static final String XINHA_SAVED_FLAG = "XINHA_SAVED_FLAG";
    public static final String XINHA_PARAM_PREFIX = "xinha-param-prefix-";

    private static final String[] defaultFormatBlock = { "h1", "h2", "h3", "h4", "h5", "h6", "p", "address", "pre" };

    private static final ResourceReference XINHA_MODAL_JS = new JavascriptResourceReference(XinhaDialogBehavior.class,
            "xinha-modal.js");

    private final String mode;
    private XinhaTextArea editor;
    private Configuration configuration;

    private InternalLinkBehavior linkPickerBehavior;
    private ExternalLinkBehavior externalLinkBehavior;
    private ImagePickerBehavior imagePickerBehavior;
    private XinhaImageService imageService;
    private XinhaLinkService linkService;
    private JcrNodeModel nodeModel;

    public AbstractXinhaPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        mode = config.getString("mode", "view");
        Fragment fragment = new Fragment("fragment", mode, this);
        add(fragment);

        if ("edit".equals(mode)) {

            fragment.add(editor = new XinhaTextArea("value", new XinhaModel(getValueModel())));

            configuration = new Configuration(config);
            configuration.setName(editor.getMarkupId());
            context.registerService(configuration, Configuration.class.getName());

            add(new EditorManagerBehavior(YuiPluginHelper.getManager(context)));

            // dialog functionality for plugins
            add(HeaderContributor.forJavaScript(XINHA_MODAL_JS));

            String nodePath = getValueModel().getJcrPropertymodel().getItemModel().getParentModel().getPath();
            configuration.addProperty("prefix", XinhaUtil.encode(BINARIES_PREFIX + nodePath));

            JcrNodeModel nodeModel = new JcrNodeModel(nodePath);
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

            fragment.add(imagePickerBehavior = new ImagePickerBehavior(context, config
                    .getPluginConfig("Xinha.plugins.InsertImage"), imageService));
            fragment.add(linkPickerBehavior = new InternalLinkBehavior(context, config
                    .getPluginConfig("Xinha.plugins.CreateLink"), linkService));
            fragment.add(externalLinkBehavior = new ExternalLinkBehavior(context, config));

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
        } else {
            final InternalLinkBrowserBehavior il = new InternalLinkBrowserBehavior();
            add(il);

            fragment.add(new WebMarkupContainer("value", getValueModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                    String text = (String) getDefaultModelObject();
                    if (text != null) {
                        JcrPropertyValueModel propertyValueModel = getValueModel();
                        String prefix = XinhaUtil.encode(BINARIES_PREFIX
                                + propertyValueModel.getJcrPropertymodel().getItemModel().getParentModel().getPath())
                                + "/";

                        String processed = XinhaHtmlProcessor.prefixImageLinks(text, prefix);
                        processed = XinhaHtmlProcessor.decorateInternalLinks(text, il);
                        getResponse().write(processed);
                    }
                    renderComponentTagBody(markupStream, openTag);
                }
            });
        }
    }

    protected abstract JcrPropertyValueModel getValueModel();

    /**
     * Callback urls aren't known at construction so set them here
     */
    @Override
    public void onBeforeRender() {
        if (configuration != null) {
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

            add(callback = new AbstractAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                public void onRequest() {
                    Request request = RequestCycle.get().getRequest();
                    boolean save = Boolean.valueOf(request.getParameter("save")).booleanValue();
                    if (save) {
                        processInput();
                    }
                }
            });
        }

        public String getCallbackUrl() {
            return callback.getCallbackUrl().toString();
        }

        @Override
        protected void onComponentTag(final ComponentTag tag) {
            StringBuilder sb = new StringBuilder();
            String width = getPluginConfig().getString("width", "500px");
            sb.append("width: ");
            sb.append(width);
            sb.append(";");

            String height = getPluginConfig().getString("height", "200px");
            sb.append("height: ");
            sb.append(height);
            sb.append(";");

            sb.append("display: none;");
            tag.put("style", sb.toString());
            super.onComponentTag(tag);
        }

    }

    class InternalLinkBrowserBehavior extends AbstractDefaultAjaxBehavior implements ILinkDecorator {
        private static final long serialVersionUID = 1L;

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

        public String decorate(String input) {
            input = WicketURLEncoder.QUERY_INSTANCE.encode(input);
            return "javascript: "
                    + generateCallbackScript("wicketAjaxGet('" + getCallbackUrl(false) + "&link=" + input + "'")
                            .toString();
        }

    }

    class EditorManagerBehavior extends AbstractYuiBehavior {
        private static final long serialVersionUID = 1L;

        protected static final String SINGLE_QUOTE = "'";
        Pattern numbers = Pattern.compile("\\d*");

        public EditorManagerBehavior(IYuiManager manager) {
            super(manager);
        }

        @Override
        public void addHeaderContribution(IYuiContext context) {
            context.addModule(XinhaNamespace.NS, "editormanager");

            context.addTemplate(new DynamicTextTemplate(AbstractXinhaPlugin.class, "xinha_init.js") {
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
            });

            //add register xinha config
            context.addOnDomLoad(new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                //Cache config
                String config;

                @Override
                public Object getObject() {
                    if (config == null) {
                        config = parseConfiguration(configuration);
                    }
                    return "YAHOO.hippo.EditorManager.register(" + config + ");";
                }
            });
        }

        /**
         * Construct a Javascript object literal that represents the configuration of the Xinha editor.
         * An example of the returned object is
         * { 
         *   name: 'xinha1', 
         *   properties: [{key: 'callbackUrl', value: '...'}]},
         *   plugins: ['AutoSave', 'table'],
         *   pluginProperties: [{ name: 'AutoSave', values [{key: 'timeout', value: 200}]}],
         *   toolbars: ['createlink', 'bold'],
         *   styleSheets: ['../../skin/skin.css']
         * }
         * @param configuration
         * @return
         */
        protected String parseConfiguration(Configuration configuration) {
            String plugins = asArray(configuration.getPluginConfigurations());
            String properties = asKeyValueArray(configuration.getProperties());
            String pluginProperties = "";
            for (PluginConfiguration pc : configuration.getPluginConfigurations()) {
                if (pluginProperties.length() > 0) {
                    pluginProperties += ", ";
                }
                pluginProperties += "{ ";
                pluginProperties += "name: '" + pc.getName() + "', ";
                pluginProperties += "values: " + asKeyValueArray(pc.getProperties());
                pluginProperties += "}";
            }
            pluginProperties = "[" + pluginProperties + "]";

            String toolbars = asArray(configuration.getToolbarItems());
            String styleSheets = asArray(configuration.getStyleSheets());
            String formatBlock = asDictionary(configuration.getFormatBlock(), true, true, "xinha.formatblock.");
            return "{" + "name: '" + configuration.getName() + "', properties: " + properties + ", " + "plugins: "
                    + plugins + ", pluginProperties: " + pluginProperties + ", " + "toolbars: " + toolbars
                    + ", styleSheets: " + styleSheets + ", " + "formatBlock: " + formatBlock + "}";
        }

        /**
         * Transforms the List<String> into a Javascript object literal. The values of the input list will be
         * used as keys and their translated values will be used as values. If reversed is set the values will be
         * used as keys and vice-versa (this to support Xinha's formatblock)
         * If addLabel is set, a value identified by translationPrefix + 'identifier' will be inserted as the first element
         * in the object literal. This value will have an empty key so Xinha will know it's the label.
         * If translationPrefix is not null it will be concatenated with the key to retrieve a translation value.   
         * 
         * Example: {'h1': 'Heading 1', 'pre': 'Formatted'}
         * Reversed example: {'Heading 1' : 'h1', 'Formatted' : 'pre'}
         * Example with label: {'': 'Label value', 'pre': 'Formatted', 'h1': 'Heading 1'}
         * 
         * @param keys  List of keys to be translated
         * @param reversed Set this to use the keys as values and values as keys 
         * @return javascript object literal with translated keys
         */
        private String asDictionary(List<String> keys, boolean addLabel, boolean reversed, String translationPrefix) {
            if (translationPrefix == null) {
                translationPrefix = "";
            }

            String ret = "";
            if (addLabel) {
                String value = "'"
                        + new StringResourceModel(translationPrefix + "identifier", AbstractXinhaPlugin.this, null)
                                .getString() + "'";
                if (reversed) {
                    ret += value + " : ''";
                } else {
                    ret += "'' : " + value;
                }
            }
            for (String key : keys) {
                if (ret.length() > 0) {
                    ret += ", ";
                }
                String value = "'"
                        + new StringResourceModel(translationPrefix + key, AbstractXinhaPlugin.this, null).getString()
                        + "'";
                key = "'" + key + "'";
                if (reversed) {
                    ret += value + " : " + key;
                } else {
                    ret += key + " : " + value;
                }
            }
            return "{" + ret + "}";
        }

        /**
         * Transforms the Map<String, String> into a Javascript array with object literals that contain a key/value pair
         * 
         * Example: [{key: 'id', value: 'myId'}, {key: 'index', value: 1}]  
         */
        private String asKeyValueArray(Map<String, String> properties) {
            String ret = "";
            for (String key : properties.keySet()) {
                if (ret.length() > 0) {
                    ret += ", ";
                }
                ret += "{ key : '" + key + "', value : " + serialize2JS(properties.get(key)) + "}";
            }
            return "[" + ret + "]";
        }

        /**
         * Transforms the List<String> into a Javascript array
         * 
         * Example: ['foo', 'bar', true, 1]
         */
        private String asArray(List<String> list) {
            String val = "  [\n";
            for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
                val += "    ";
                val += serialize2JS(iter.next());
                if (iter.hasNext())
                    val += ",";
                val += "\n";
            }
            val += "  ]";
            return val;
        }

        /**
         * Transforms the Set of BaseConfigurations into a Javascript array containing the configuration names
         * 
         * Example: ['config1', 'config2']
         */
        private String asArray(Set<? extends BaseConfiguration> configs) {
            String val = "  [\n";
            for (Iterator<? extends BaseConfiguration> iter = configs.iterator(); iter.hasNext();) {
                val += "    ";
                val += serialize2JS(iter.next().getName());
                if (iter.hasNext())
                    val += ",";
                val += "\n";
            }
            val += "  ]";
            return val;
        }

        /**
         * Serializes a String value into a Javascript value. True/false will be serialized as Javascript booleans,
         * numbers will be serialized as Javascript numbers and String will be escaped by two single quotes. 
         */
        private String serialize2JS(String value) {
            if (value == null)
                return "null";
            else if (value.equalsIgnoreCase("true"))
                return "true";
            else if (value.equalsIgnoreCase("false"))
                return "false";
            else if (numbers.matcher(value).matches())
                return value;

            return SINGLE_QUOTE + value.replaceAll(SINGLE_QUOTE, "\\\\" + SINGLE_QUOTE) + SINGLE_QUOTE;
        }

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

        public Configuration(IPluginConfig config) {
            addProperty("saveSuccessFlag", XINHA_SAVED_FLAG);
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

    class PluginConfiguration extends BaseConfiguration {

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

    class BaseConfiguration implements IClusterable {
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
