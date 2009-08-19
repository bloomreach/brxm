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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
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
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
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

    // hardcoded ignore path set
    private final static Set<String> ignorePaths = new HashSet<String>(Arrays.asList(new String[] { "/jcr:system",
            "/hippo:configuration", "/hippo:namespaces" }));

    private final String mode;
    private TextArea editor;
    private Configuration configuration;
    private AbstractAjaxBehavior postBehavior;

    private InternalLinkBehavior linkPickerBehavior;
    private ExternalLinkBehavior externalLinkBehavior;
    private ImagePickerBehavior imagePickerBehavior;
    private XinhaImageService imageService;
    private XinhaLinkService linkService;

    public AbstractXinhaPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        mode = config.getString("mode", "view");
        Fragment fragment = new Fragment("fragment", mode, this);
        add(fragment);

        if ("edit".equals(mode)) {
            fragment.add(createEditor(config));
            configuration = new Configuration(config);
            context.registerService(configuration, Configuration.class.getName());

            JcrPropertyValueModel propertyValueModel = getValueModel();
            String nodePath = propertyValueModel.getJcrPropertymodel().getItemModel().getParentModel().getPath();
            JcrNodeModel nodeModel = new JcrNodeModel(nodePath);

            // dialog functionality for plugins
            add(HeaderContributor.forJavaScript(new JavascriptResourceReference(XinhaDialogBehavior.class,
                    "xinha-modal.js")));

            imageService = new XinhaImageService(nodeModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected String getXinhaName() {
                    return configuration.getName();
                }

            };
            fragment.add(imagePickerBehavior = new ImagePickerBehavior(context, config
                    .getPluginConfig("Xinha.plugins.InsertImage"), imageService));

            linkService = new XinhaLinkService(nodeModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected String getXinhaName() {
                    return configuration.getName();
                }

            };
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
            fragment.add(new WebMarkupContainer("value", getValueModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                    String text = (String) getModelObject();
                    if (text != null) {
                        JcrPropertyValueModel propertyValueModel = getValueModel();
                        String prefix = XinhaUtil.encode(BINARIES_PREFIX
                                + propertyValueModel.getJcrPropertymodel().getItemModel().getParentModel().getPath())
                                + "/";

                        String processed = XinhaHtmlProcessor.prefixImageLinks(text, prefix);
                        getResponse().write(processed);
                    }
                    renderComponentTagBody(markupStream, openTag);
                }
            });
        }
    }

    protected abstract JcrPropertyValueModel getValueModel();

    @Override
    public void onBeforeRender() {
        if (configuration != null) {
            // FIXME: add logic to clean up on the client (delete Xinha.config)
            editor.setMarkupId("xinha" + Integer.valueOf(Session.get().nextSequenceValue()));
            configuration.setName(editor.getMarkupId());
            configuration.addProperty("callbackUrl", postBehavior.getCallbackUrl().toString());
            configuration.addProperty("saveSuccessFlag", XINHA_SAVED_FLAG);
            configuration.addProperty("xinhaParamToken", XINHA_PARAM_PREFIX);

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

            JcrPropertyValueModel propertyValueModel = getValueModel();
            // getValueModel regularly returns null, if so, skip the image/binary link
            if (propertyValueModel != null) {
                String nodePath = propertyValueModel.getJcrPropertymodel().getItemModel().getParentModel().getPath();
                configuration.addProperty("prefix", XinhaUtil.encode(BINARIES_PREFIX + nodePath));
            }

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

    protected String clean(final String value) throws Exception {
        if(value != null) {
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
    
    private Component createEditor(final IPluginConfig config) {
        final JcrPropertyValueModel valueModel = getValueModel();
        editor = new TextArea("value", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return valueModel.getObject();
            }

            public void setObject(Object value) {
                try {
                    String cleanedValue = clean((String) value);
                    removeLinks(cleanedValue);
                    valueModel.setObject(cleanedValue);
                } catch (Exception e) {
                    error(new ResourceModel("error-while-cleaning-conent",
                            "An error occured while cleaning the content"));
                    log.error("Exception caught during editor creation while cleaning value: " + value, e);
                }
            }

            public void detach() {
                valueModel.detach();
            }

        }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                StringBuilder sb = new StringBuilder();
                String width = config.getString("width", "500px");
                sb.append("width: ");
                sb.append(width);
                sb.append(";");

                String height = config.getString("height", "200px");
                sb.append("height: ");
                sb.append(height);
                sb.append(";");

                tag.put("style", sb.toString());
                super.onComponentTag(tag);
            }
        };

        postBehavior = new AbstractAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            public void onRequest() {
                IRequestTarget requestTarget = new IRequestTarget() {
                    public void respond(RequestCycle requestCycle) {
                        WebResponse r = (WebResponse) requestCycle.getResponse();

                        // Determine encoding
                        final String encoding = Application.get().getRequestCycleSettings()
                                .getResponseRequestEncoding();
                        r.setCharacterEncoding(encoding);
                        r.setContentType("application/json; charset=" + encoding);

                        // Make sure it is not cached
                        r.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                        r.setHeader("Cache-Control", "no-cache, must-revalidate");
                        r.setHeader("Pragma", "no-cache");

                        handleRequest(requestCycle.getRequest(), r);
                    }

                    public void detach(RequestCycle requestCycle) {
                    }

                };
                RequestCycle.get().setRequestTarget(requestTarget);
            }

            protected void handleRequest(Request request, Response response) {
                boolean save = Boolean.valueOf(request.getParameter("save")).booleanValue();
                if (save) {
                    editor.processInput();
                    //target.appendJavascript(XINHA_SAVED_FLAG);
                }

                String browse = request.getParameter("browse");
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

                    response.write("[" + sb.toString() + "]");
                }
            }
        };

        editor.setOutputMarkupId(true);
        editor.setVisible(true);
        editor.add(postBehavior);

        add(this.new XinhaHeaderContributor());
        add(new EditorManagerBehavior(YuiPluginHelper.getManager(getPluginContext())));

        return editor;
    }

    class EditorManagerBehavior extends AbstractYuiBehavior {
        private static final long serialVersionUID = 1L;

        public EditorManagerBehavior(IYuiManager manager) {
            super(manager);
        }

        @Override
        public void addHeaderContribution(IYuiContext context) {
            context.addModule(XinhaNamespace.NS, "editormanager");
        }

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

        private static final String XINHA_PLUGINS = "Xinha.plugins";
        private static final String XINHA_TOOLBAR = "Xinha.config.toolbar";
        private static final String XINHA_CSS = "Xinha.config.css";
        private static final String XINHA_SKIN = "Xinha.skin";

        private final Map<String, PluginConfiguration> pluginConfigurations = new HashMap<String, PluginConfiguration>();
        private final List<String> toolbarItems;
        private final List<String> styleSheets;
        private final String skin;

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
            return AbstractXinhaPlugin.this.hashCode();
        }
    }
}
