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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;
import org.hippoecm.frontend.plugins.xinha.modal.imagepicker.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.xinha.modal.imagepicker.ImageItemFactory.ImageItem;
import org.hippoecm.frontend.plugins.xinha.modal.linkpicker.LinkPickerBehavior;
import org.hippoecm.frontend.plugins.yui.dragdrop.DropBehavior;
import org.hippoecm.frontend.service.PluginRequestTarget;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(XinhaPlugin.class);

    private static final String BINARIES_PREFIX = "binaries";
    public static final String XINHA_SAVED_FLAG = "XINHA_SAVED_FLAG";
    public static final String XINHA_PARAM_PREFIX = "xinha-param-prefix-";

    // hardcoded ignore path set
    private final static Set<String> ignorePaths = new HashSet<String>(Arrays.asList(new String[] { "/jcr:system",
            "/hippo:configuration", "/hippo:namespaces" }));

    private static Pattern IMG_PATTERN = Pattern.compile("<img[^>]+>", Pattern.CASE_INSENSITIVE);
    private static Pattern SRC_PATTERN = Pattern.compile("src=\"[^\"]+\"", Pattern.CASE_INSENSITIVE);

    private String mode;
    private TextArea editor;
    private Configuration configuration;
    private AbstractDefaultAjaxBehavior postBehavior;

    private XinhaModalWindow modalWindow;
    private LinkPickerBehavior linkPickerBehavior;
    private ImagePickerBehavior imagePickerBehavior;

    public XinhaPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        mode = config.getString("mode", "view");
        Fragment fragment = new Fragment("fragment", mode, this);
        add(fragment);

        if ("edit".equals(mode)) {
            fragment.add(createEditor(config));
            configuration = new Configuration(config);
            context.registerService(configuration, Configuration.class.getName());

            JcrPropertyValueModel propertyValueModel = (JcrPropertyValueModel) getModel();
            String nodePath = propertyValueModel.getJcrPropertymodel().getItemModel().getParentModel().getPath();
            JcrNodeModel nodeModel = new JcrNodeModel(nodePath);

            fragment.add(modalWindow = new XinhaModalWindow("modalwindow"));
            fragment.add(imagePickerBehavior = new ImagePickerBehavior(modalWindow, nodeModel));
            fragment.add(linkPickerBehavior = new LinkPickerBehavior(modalWindow, nodeModel));
        } else {
            fragment.add(new WebMarkupContainer("value", getModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                    String text = (String) getModelObject();
                    if (text != null) {
                        JcrPropertyValueModel propertyValueModel = (JcrPropertyValueModel) getModel();
                        String prefix = BINARIES_PREFIX + propertyValueModel.getJcrPropertymodel().getItemModel().getParentModel().getPath() + "/";

                        StringBuffer processed = new StringBuffer();
                        Matcher m = IMG_PATTERN.matcher(text);
                        while (m.find()) {
                            StringBuffer src = new StringBuffer();
                            Matcher s = SRC_PATTERN.matcher(m.group());
                            while (s.find()) {
                                String path = s.group().substring(5, s.group().length() - 1);
                                s.appendReplacement(src, "src=\"" + prefix + path + "\"");
                            }
                            m.appendReplacement(processed, src.toString());
                        }
                        m.appendTail(processed);
                        getResponse().write(processed);
                    }
                    renderComponentTagBody(markupStream, openTag);
                }
            });
        }
        
        add(new DropBehavior(context, config) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onDrop(IModel model, AjaxRequestTarget target) {
                if(model instanceof JcrNodeModel) {
                    JcrNodeModel nodeModel = (JcrNodeModel) model;
                    //test for nodetype to select picker
                    ImageItem item = imagePickerBehavior.getImageItemDAO().insertImageModel(nodeModel); 
                    if(item != null) {
                        target.getHeaderResponse().renderOnDomReadyJavascript("xinha_editors." + configuration.getName() + ".plugins.ImagePicker.instance.insertImage('" + item.getUrl() + "');");
                    }
                }
            }
        });
    }

    @Override
    public void onBeforeRender() {
        if (configuration != null) {
            // FIXME: add logic to clean up on the client (delete Xinha.config)
            editor.setMarkupId("xinha" + Integer.valueOf(Session.get().nextSequenceValue()));
            configuration.setName(editor.getMarkupId());
            configuration.addProperty("callbackUrl", postBehavior.getCallbackUrl().toString());
            configuration.addProperty("saveSuccessFlag", XINHA_SAVED_FLAG);
            configuration.addProperty("xinhaParamToken", XINHA_PARAM_PREFIX);

            if (configuration.getPluginConfiguration("ImagePicker") != null) {
                configuration.getPluginConfiguration("ImagePicker").addProperty("callbackUrl",
                        imagePickerBehavior.getCallbackUrl().toString());
            }
            if (configuration.getPluginConfiguration("CustomLinker") != null) {
                configuration.getPluginConfiguration("CustomLinker").addProperty("callbackUrl",
                        linkPickerBehavior.getCallbackUrl().toString());
            }

            JcrPropertyValueModel propertyValueModel = (JcrPropertyValueModel) getModel();
            String nodePath = propertyValueModel.getJcrPropertymodel().getItemModel().getParentModel().getPath();
            configuration.addProperty("jcrNodePath", BINARIES_PREFIX + nodePath);

            IPluginContext context = getPluginContext();
            XinhaEditorBehavior sharedBehavior = context.getService(XinhaEditorBehavior.class.getName(),
                    XinhaEditorBehavior.class);
            if (sharedBehavior == null) {
                sharedBehavior = new XinhaEditorBehavior(context);
                Page page = context.getService(Home.class.getName(), Home.class);
                String serviceId = context.getReference(page).getServiceId();
                context.registerService(sharedBehavior, serviceId);
                context.registerService(sharedBehavior, XinhaEditorBehavior.class.getName());
            }
        }
        super.onBeforeRender();
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (configuration != null) {
            Page page = (Page) findParent(Page.class);
            if (page == null) {
                configuration.setName(null);
            }
        }
        super.render(target);
    }

    private Component createEditor(final IPluginConfig config) {
        JcrPropertyValueModel valueModel = (JcrPropertyValueModel) getModel();
        editor = new TextArea("value", valueModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                StringBuilder sb = new StringBuilder();
                String width = config.getString("width");
                if (width == null) {
                    width = "500px";
                }
                sb.append("width: ");
                sb.append(width);
                sb.append(";");

                String height = config.getString("height");
                if (height == null) {
                    height = "200px";
                }
                sb.append("height: ");
                sb.append(height);
                sb.append(";");

                tag.put("style", sb.toString());
                super.onComponentTag(tag);
            }
        };

        postBehavior = new AbstractDefaultAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget target) {
                RequestCycle requestCycle = RequestCycle.get();
                boolean save = Boolean.valueOf(requestCycle.getRequest().getParameter("save")).booleanValue();
                if (save) {
                    editor.processInput();
                    target.appendJavascript(XINHA_SAVED_FLAG);
                }

                String browse = requestCycle.getRequest().getParameter("browse");
                if (browse != null) {

                    if ("".equals(browse)) {
                        browse = "/";
                    }

                    StringBuilder sb = new StringBuilder();

                    //custom browsing
                    try {
                        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                        Item item = session.getItem(browse);
                        if (item.isNode()) {
                            Node itemNode = (Node) item;
                            NodeIterator iterator = itemNode.getNodes();
                            while (iterator.hasNext()) {
                                Node childNode = iterator.nextNode();
                                Node canonicalNode = ((HippoNode) childNode).getCanonicalNode();
                                if (canonicalNode != null && canonicalNode.isSame(childNode)
                                        && !ignorePaths.contains(canonicalNode.getPath())) {
                                    sb.append("{");
                                    sb.append("title: '").append(childNode.getName()).append("'");
                                    boolean isHandle = childNode.isNodeType(HippoNodeType.NT_HANDLE);
                                    sb.append(", clickable: ").append(isHandle);
                                    sb.append(", url: '").append(childNode.getPath()).append("'");
                                    if (childNode.getNodes().hasNext() && !isHandle) {
                                        sb.append(", children: []");
                                    }
                                    sb.append("}");
                                    if (iterator.hasNext()) {
                                        sb.append(",");
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException e) {
                        log.error(e.getMessage());
                    }

                    target.appendJavascript("[" + sb.toString() + "]");

                }
            }
        };

        editor.setOutputMarkupId(true);
        editor.setVisible(true);
        editor.add(postBehavior);

        add(this.new XinhaHeaderContributor());
        return editor;
    }

    //Move this to editorBehaviour?
    class XinhaHeaderContributor extends AbstractHeaderContributor {
        private static final long serialVersionUID = 1L;

        @Override
        public final IHeaderContributor[] getHeaderContributors() {
            IPluginContext context = getPluginContext();

            // make sure a shared behavior exists
            XinhaEditorBehavior sharedBehavior = context.getService(XinhaEditorBehavior.class.getName(),
                    XinhaEditorBehavior.class);
            return sharedBehavior.getHeaderContributorsPartly();
        }
    }

    class Configuration extends BaseConfiguration {
        private static final long serialVersionUID = 1L;

        //        private static final String XINHA_PREFIX = "Xinha.";
        //        private static final String XINHA_CONFIG_PREFIX = "Xinha.config.";
        private static final String XINHA_PLUGINS = "Xinha.plugins";
        private static final String XINHA_TOOLBAR = "Xinha.config.toolbar";
        private static final String XINHA_CSS = "Xinha.config.css";
        private static final String XINHA_SKIN = "Xinha.skin";

        private Map<String, PluginConfiguration> pluginConfigurations = new HashMap<String, PluginConfiguration>();
        private List<String> toolbarItems;
        private List<String> styleSheets;
        private String skin;

        public Configuration(IPluginConfig config) {
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

            /*
             * FIXME: there is no way to obtain the keys from the IPluginConfig
             *
            for (String paramKey : config.keySet()) {
                if (paramKey.startsWith(XINHA_PREFIX)) {
                    List<String> paramValues = parameters.get(paramKey).getStrings();
                    for (String propertyValue : paramValues) {
                        int equalsIndex = propertyValue.indexOf("=");
                        if (equalsIndex > -1) {
                            addProperty(propertyValue.substring(0, equalsIndex), propertyValue
                                    .substring(equalsIndex + 1));
                        } else {
                            String propertyKey = paramKey.substring(XINHA_CONFIG_PREFIX.length());
                            if (getProperties().containsKey(propertyKey)) {
                                addProperty(propertyKey, getProperty(propertyKey) + "," + propertyValue);
                            } else {
                                addProperty(propertyKey, propertyValue);
                            }
                        }
                    }
                }
            } */
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

        public void setPluginConfigurations(Set<PluginConfiguration> plugins) {
            for (PluginConfiguration conf : plugins) {
                addPluginConfiguration(conf);
            }
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
                throw new IllegalArgumentException("Invalid key/value argument, no seperator found: " + keyValue);
            }
            addProperty(keyValue.substring(0, equalsIndex), keyValue.substring(equalsIndex + 1));
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

        @Override
        public boolean equals(Object o) {
            if (o instanceof Configuration) {
                if (name != null) {
                    return name.equals(((Configuration) o).getName());
                } else {
                    return ((Configuration) o).getName() == null;
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return XinhaPlugin.this.hashCode();
        }
    }
}
