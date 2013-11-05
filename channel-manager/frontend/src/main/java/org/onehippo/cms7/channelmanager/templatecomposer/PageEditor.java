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
package org.onehippo.cms7.channelmanager.templatecomposer;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.repository.api.HippoNodeType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.onehippo.cms7.channelmanager.channels.ChannelPropertiesWindow;
import org.onehippo.cms7.channelmanager.channels.ChannelStore;
import org.onehippo.cms7.channelmanager.hstconfig.HstConfigEditor;
import org.onehippo.cms7.channelmanager.templatecomposer.iframe.IFrameBundle;
import org.onehippo.cms7.jquery.JQueryBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.ExtPropertyConverter;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.TemplateComposer.PageEditor")
public class PageEditor extends ExtPanel {

    private static final Logger log = LoggerFactory.getLogger(PageEditor.class);
    private static final long serialVersionUID = 1L;

    // default initial connection timeout in milliseconds
    private static final long DEFAULT_INITIAL_CONNECTION_TIMEOUT = 60000L;

    private static final Boolean DEFAULT_PREVIEW_MODE = Boolean.TRUE;
    private static final Boolean DEFAULT_INITIALIZE_HST_CONFIG_EDITOR_WITH_PREVIEW_CONTEXT = Boolean.TRUE;

    private static final PackageResourceReference ERROR_HTML = new PackageResourceReference(IFrameBundle.class, IFrameBundle.ERROR_HTML);
    private static final PackageResourceReference CHANNEL_MANAGER_IFRAME_CSS = new PackageResourceReference(IFrameBundle.class, IFrameBundle.CHANNEL_MANAGER_IFRAME_CSS);

    private static final PackageResourceReference[] DEVELOPMENT_REFERENCES;
    private static final PackageResourceReference[] DEPLOYMENT_REFERENCES;

    static {
        List<PackageResourceReference> developmentRefs = new ArrayList<PackageResourceReference>();
        developmentRefs.add(new PackageResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CORE));
        developmentRefs.add(new PackageResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CLASS_PLUGIN));
        developmentRefs.add(new PackageResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_NAMESPACE_PLUGIN));
        developmentRefs.add(new PackageResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_UI));

        developmentRefs.add(new PackageResourceReference(Home.class, "js/global.js"));
        developmentRefs.add(new PackageResourceReference(TemplateComposerGlobalBundle.class, TemplateComposerGlobalBundle.GLOBALS));
        developmentRefs.add(new PackageResourceReference(IFrameBundle.class, IFrameBundle.UTIL));
        developmentRefs.add(new PackageResourceReference(IFrameBundle.class, IFrameBundle.DRAG_DROP));
        developmentRefs.add(new PackageResourceReference(IFrameBundle.class, IFrameBundle.FACTORY));
        developmentRefs.add(new PackageResourceReference(IFrameBundle.class, IFrameBundle.PAGE));
        developmentRefs.add(new PackageResourceReference(IFrameBundle.class, IFrameBundle.WIDGETS));
        developmentRefs.add(new PackageResourceReference(IFrameBundle.class, IFrameBundle.SURFANDEDIT));
        developmentRefs.add(new PackageResourceReference(IFrameBundle.class, IFrameBundle.LAST));
        DEVELOPMENT_REFERENCES = developmentRefs.toArray(new PackageResourceReference[developmentRefs.size()]);

        List<PackageResourceReference> deploymentRefs = new ArrayList<PackageResourceReference>();
        deploymentRefs.add(new PackageResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_ALL_MIN));
        deploymentRefs.add(new PackageResourceReference(Home.class, "js/global.js"));
        deploymentRefs.add(new PackageResourceReference(IFrameBundle.class, IFrameBundle.ALL));
        DEPLOYMENT_REFERENCES = deploymentRefs.toArray(new PackageResourceReference[deploymentRefs.size()]);
    }

    @ExtProperty
    private Boolean debug = false;

    @ExtProperty
    private String composerRestMountPath = "/_rp";

    @ExtProperty
    @SuppressWarnings("unused")
    private String renderPathInfo = "";

    @ExtProperty
    private String contextPath = "/site";

    @ExtProperty
    private String cmsPreviewPrefix;

    @ExtProperty
    private String templateComposerContextPath = "/site";

    @ExtProperty
    @SuppressWarnings("unused")
    private String renderHostParameterName = ContainerConstants.RENDERING_HOST;

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsUser;

    @ExtProperty
    @SuppressWarnings("unused")
    private Boolean canManageChanges = Boolean.FALSE;

    @ExtProperty
    private Boolean previewMode = DEFAULT_PREVIEW_MODE;

    @ExtProperty
    @SuppressWarnings("unused")
    private Long initialHstConnectionTimeout = DEFAULT_INITIAL_CONNECTION_TIMEOUT;

    @ExtProperty
    @SuppressWarnings("unused")
    private String locale;

    @ExtProperty
    @SuppressWarnings("unused")
    private String variantsUuid;

    private IPluginContext context;
    private ExtStoreFuture<Object> channelStoreFuture;
    private ChannelPropertiesWindow channelPropertiesWindow;
    private boolean redraw = false;
    private List<Observer<JcrNodeModel>> observers = new ArrayList<Observer<JcrNodeModel>>();

    @ExtProperty
    @SuppressWarnings("unused")
    private String channelId;
    private boolean refreshIFrame;
    private List<ToolbarPlugin> toolbarPlugins;

    public PageEditor(final IPluginContext context, final IPluginConfig config, final HstConfigEditor hstConfigEditor, final ExtStoreFuture<Object> channelStoreFuture) {
        this.context = context;
        this.channelStoreFuture = channelStoreFuture;
        this.debug = Application.get().getDebugSettings().isAjaxDebugModeEnabled();
        this.locale = Session.get().getLocale().toString();
        this.cmsUser = UserSession.get().getJcrSession().getUserID();

        String variantsPath = null;
        if (config != null) {
            variantsPath = config.getString("variantsPath");
            this.composerRestMountPath = config.getString("composerRestMountPath", composerRestMountPath);
            this.templateComposerContextPath = config.getString("templateComposerContextPath", templateComposerContextPath);
            this.contextPath = config.getString("contextPath", contextPath);
            this.initialHstConnectionTimeout = config.getLong("initialHstConnectionTimeout", DEFAULT_INITIAL_CONNECTION_TIMEOUT);
            this.previewMode = config.getAsBoolean("previewMode", DEFAULT_PREVIEW_MODE);
            this.canManageChanges = canManageChanges(this.cmsUser, config);
        }
        this.variantsUuid = getVariantsUuidOrNull(variantsPath);

        this.channelPropertiesWindow = new ChannelPropertiesWindow(context, (ChannelStore) channelStoreFuture.getStore());
        add(this.channelPropertiesWindow);

        addEventListener("edit-document", new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                JSONArray values = parameters.get("uuid");
                if (values == null || values.length() == 0) {
                    return;
                }
                try {
                    final Object value = values.get(0);
                    String editorManagerServiceId = config != null ? config.getString(IEditorManager.EDITOR_ID,
                                                                                      "service.edit") : "service.edit";
                    IEditorManager editorManager = context.getService(editorManagerServiceId, IEditorManager.class);
                    JcrNodeModel model = new JcrNodeModel(
                            UserSession.get().getJcrSession().getNodeByIdentifier(value.toString()));
                    IEditor<?> editor = editorManager.getEditor(model);
                    if (editor == null) {
                        editor = editorManager.openEditor(model);
                    }
                    editor.setMode(IEditor.Mode.EDIT);
                    editor.focus();
                } catch (JSONException e) {
                    throw new WicketRuntimeException("Invalid JSON parameters", e);
                } catch (ItemNotFoundException e) {
                    log.warn("Could not find document to browse to", e);
                } catch (RepositoryException e) {
                    log.error("Internal error when browsing to document", e);
                } catch (EditorException e) {
                    log.error("Could not open editor for document", e);
                } catch (ServiceException e) {
                    log.error("Opening editor failed", e);
                }

            }
        });

        addEventListener("edit-hst-config", new ExtEventListener() {

            private Object getValue(final Map<String, JSONArray> parameters, final String key) throws JSONException {
                JSONArray values = parameters.get(key);
                if (values == null || values.length() == 0) {
                    return null;
                }
                return values.get(0);
            }

            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                try {
                    final String paramChannelId = (String) getValue(parameters, "channelId");
                    final String paramHstMountPoint = (String) getValue(parameters, "hstMountPoint");
                    target.prependJavaScript("Ext.getCmp('Hippo.ChannelManager.HstConfigEditor.Instance').initEditor();");
                    hstConfigEditor.setMountPoint(target, paramChannelId, paramHstMountPoint);
                } catch (JSONException e) {
                    throw new WicketRuntimeException("Invalid JSON parameters", e);
                }
            }
        });

        addEventListener("documents", new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                clearObservers();

                JSONArray values = parameters.get("documents");
                if (values == null || values.length() == 0) {
                    return;
                }
                try {
                    for (int i = 0; i < values.length(); i++) {
                        String uuid = values.getString(i);
                        if (Strings.isEmpty(uuid)) {
                            continue;
                        }
                        addDocumentObservers(uuid);
                    }
                } catch (JSONException e) {
                    throw new WicketRuntimeException("Invalid JSON parameters", e);
                }
            }
        });
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(new CssResourceReference(PageEditor.class, "plugins/colorfield/colorfield.css")));
        response.render(CssHeaderItem.forReference(new CssResourceReference(PageEditor.class, "plugins/vtabs/VerticalTabPanel.css")));
        response.render(TemplateComposerHeaderItem.get());
    }

    private static boolean canManageChanges(final String user, final IPluginConfig config) {
        final String manageChangesPrivilege = config.getString("manage.changes.privileges", "hippo:admin");
        final String manageChangesPathToCheck = config.getString("manage.changes.privileges.path", "/hst:hst/hst:channels");
        return isAllowedTo(user, "manage changes", manageChangesPrivilege, manageChangesPathToCheck);
    }

    private static boolean isAllowedTo(final String user, final String description, final String privileges, final String pathToCheck) {
        boolean isAllowed = false;
        try {
            UserSession.get().getJcrSession().checkPermission(pathToCheck, privileges);
            isAllowed = true;
            log.info("User '{}' is allowed to {}.", user, description);
        } catch(AccessControlException e) {
            log.info("User '{}' is not allowed to {}.", user, description);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Problems while checking if user '" + user + "' is allowed to " + description
                        + ", assuming user is not allowed.", e);
            } else {
                log.warn("Problems while checking if user '{}' is allowed to {}, assuming user is not allowed. {}",
                        new Object[]{user, description, e});
            }
        }
        return isAllowed;
    }

    private static String getVariantsUuidOrNull(final String variantsPath) {
        if (StringUtils.isNotEmpty(variantsPath)) {
            final javax.jcr.Session session = UserSession.get().getJcrSession();
            try {
                if (session.nodeExists(variantsPath)) {
                    return session.getNode(variantsPath).getIdentifier();
                } else {
                    log.info("No node at '{}': variants will not be available.", variantsPath);
                }
            } catch (RepositoryException e) {
                log.error("Failed to retrieve variants node '" + variantsPath + "'", e);
            }
        } else {
            log.info("Variants path not configured. Only the default variant will be available.");
        }
        return null;
    }

    private void addDocumentObservers(final String uuid) {
        try {
            Node node = UserSession.get().getJcrSession().getNodeByIdentifier(uuid);
            addObserver(node);
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (Node child : new NodeIterable(node.getNodes())) {
                    if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        if (child.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)) {
                            Value[] availabilities = child.getProperty(HippoNodeType.HIPPO_AVAILABILITY).getValues();
                            for (Value availability : availabilities) {
                                if ("preview".equals(availability.getString())) {
                                    addObserver(child);
                                    break;
                                }
                            }
                        } else {
                            addObserver(child);
                        }
                    }
                }
            }
        } catch (RepositoryException re) {
            log.warn("Unable to construct node model for document {}", uuid);
        }
    }

    private void addObserver(final Node node) {
        JcrNodeModel nodeModel = new JcrNodeModel(node);
        Observer<JcrNodeModel> observer = new DocumentObserver(nodeModel);
        context.registerService(observer, IObserver.class.getName());
        observers.add(observer);
    }

    private void clearObservers() {
        for (Observer observer : observers) {
            context.unregisterService(observer, IObserver.class.getName());
        }
        observers.clear();
    }

    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(
                " try { Ext.namespace(\"%s\"); window.%s = new %s(%s); } catch (e) { Ext.Msg.alert('Error', 'Error instantiating template composer. '+e); }; \n",
                getMarkupId(), getMarkupId(), extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.ChannelManager.TemplateComposer.Instance";
    }

    @Override
    protected void onBeforeRender() {
        if (toolbarPlugins == null) {
            toolbarPlugins = context.getServices(ToolbarPlugin.SERVICE_ID, ToolbarPlugin.class);
            for (ToolbarPlugin plugin : toolbarPlugins) {
                add(plugin);
            }
        }
        super.onBeforeRender();
    }

    @Override
    protected void onRenderProperties(final JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        RequestCycle rc = RequestCycle.get();
        properties.put("channelPropertiesWindowId", this.channelPropertiesWindow.getMarkupId());
        properties.put("channelStoreFuture", new JSONIdentifier(this.channelStoreFuture.getJsObjectId()));
        properties.put("iFrameErrorPage", Arrays.asList(
                rc.urlFor(new ResourceReferenceRequestHandler(ERROR_HTML)).toString()));
        properties.put("iFrameCssHeadContributions", Arrays.asList(
                rc.urlFor(new ResourceReferenceRequestHandler(CHANNEL_MANAGER_IFRAME_CSS)).toString()));
        properties.put("toolbarPlugins", createToolbarPluginConfigs());

        PackageResourceReference[] references;
        if (debug) {
            references = DEVELOPMENT_REFERENCES;
        } else {
            references = DEPLOYMENT_REFERENCES;
        }

        List<String> headContributionUrls = new ArrayList<String>();
        for (PackageResourceReference reference : references) {
            headContributionUrls.add(rc.urlFor(new ResourceReferenceRequestHandler(reference)).toString());
        }
        properties.put("iFrameJsHeadContributions", headContributionUrls);
    }

    private JSONArray createToolbarPluginConfigs() throws JSONException {
        final JSONArray configs = new JSONArray();

        for (ToolbarPlugin plugin : toolbarPlugins) {
            JSONObject config = new JSONObject();
            config.put("xtype", plugin.getXType());

            JSONObject positions = new JSONObject();
            positions.put("view", plugin.getPositionView());
            positions.put("edit", plugin.getPositionEdit());
            config.put("positions", positions);

            configs.put(config);
        }

        return configs;
    }

    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        if ("edit-document".equals(event)) {
            return new ExtEventAjaxBehavior("uuid");
        } else if ("edit-hst-config".equals(event)) {
            return new ExtEventAjaxBehavior("channelId", "hstMountPoint");
        } else if ("documents".equals(event)) {
            return new ExtEventAjaxBehavior("documents");
        }
        return super.newExtEventBehavior(event);
    }

    public void redraw() {
        redraw = true;
        clearObservers();
    }

    public void render(PluginRequestTarget target) {
        if (redraw) {
            JSONObject update = new JSONObject();
            ExtPropertyConverter.addProperties(this, getClass(), update);
            target.appendJavaScript("Ext.getCmp('" + getMarkupId() + "').browseTo(" + update.toString() + ");");
            redraw = false;
        } else if (refreshIFrame) {
            refreshIFrame = false;
            target.appendJavaScript("Ext.getCmp('" + getMarkupId() + "').refreshIframe();");
        }
    }

    public void setChannel(final String channelId) {
        this.channelId = channelId;
        redraw();
    }

    public void setChannelName(String name) {
        setTitle(new Model<String>(name));
        redraw();
    }

    public Boolean getPreviewMode() {
        return previewMode;
    }

    public void setPreviewMode(final Boolean previewMode) {
        this.previewMode = previewMode;
        redraw();
    }

    public void setRenderPathInfo(String pathInfo) {
        this.renderPathInfo = pathInfo;
        redraw();
    }
    
    public void setRenderContextPath(String contextPath) {
        this.contextPath = contextPath;
        redraw();
    }
    
    public void setTemplateComposerContextPath(String templateComposerContextPath) {
        this.templateComposerContextPath = templateComposerContextPath;
        redraw();
    }

    public String getCmsPreviewPrefix() {
        return cmsPreviewPrefix;
    }

    public void setCmsPreviewPrefix(final String cmsPreviewPrefix) {
        this.cmsPreviewPrefix = cmsPreviewPrefix;
    }

    @Override
    protected void onDetach() {
        super.onDetach();

        for (Observer<JcrNodeModel> observer : observers) {
            observer.getObservable().detach();
        }
    }

    private class DocumentObserver extends Observer<JcrNodeModel> {

        public DocumentObserver(final JcrNodeModel nodeModel) {
            super(nodeModel);
        }

        @Override
        public void onEvent(final Iterator events) {
            refreshIFrame();
        }
    }

    private void refreshIFrame() {
        refreshIFrame = true;
    }
}
