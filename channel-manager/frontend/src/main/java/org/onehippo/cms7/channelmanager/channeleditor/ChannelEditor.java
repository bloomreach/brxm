/*
 *  Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channeleditor;

import java.io.Serializable;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IEditorOpenListener;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.util.UserUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.onehippo.cms7.channelmanager.channeleditor.pickers.ImagePicker;
import org.onehippo.cms7.channelmanager.channeleditor.pickers.LinkPicker;
import org.onehippo.cms7.channelmanager.channeleditor.pickers.RichTextImageVariantPicker;
import org.onehippo.cms7.channelmanager.channeleditor.pickers.RichTextLinkPicker;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.onehippo.cms7.openui.extensions.JcrUiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtensionLoader;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

@ExtClass("Hippo.ChannelManager.ChannelEditor.ChannelEditor")
public class ChannelEditor extends ExtPanel {
    public static final Logger log = LoggerFactory.getLogger(ChannelEditor.class);

    private static final long DEFAULT_INITIAL_CONNECTION_TIMEOUT = 60000L;
    private static final long DEFAULT_EXT_AJAX_TIMEOUT = 30000L;

    private static final String OPEN_DOCUMENT_EVENT = "open-document";
    private static final String CLOSE_DOCUMENT_EVENT = "close-document";

    private static final ObjectMapper JSON_ORG_OBJECT_MAPPER = new ObjectMapper();
    static {
        JSON_ORG_OBJECT_MAPPER.registerModule(new JsonOrgModule());
    }

    @ExtProperty
    @SuppressWarnings("unused")
    private Boolean debug = false;

    @ExtProperty
    @SuppressWarnings("unused")
    private String locale;

    @ExtProperty
    @SuppressWarnings("unused")
    private String timeZone;

    @ExtProperty
    @SuppressWarnings("unused")
    private String apiUrlPrefix;

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsUser;

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsUserFirstName;

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsUserLastName;

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsUserDisplayName;

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsVersion;

    @ExtProperty
    @SuppressWarnings("unused")
    private Long initialHstConnectionTimeout = DEFAULT_INITIAL_CONNECTION_TIMEOUT;

    @ExtProperty
    @SuppressWarnings("unused")
    private Long extAjaxTimeout = DEFAULT_EXT_AJAX_TIMEOUT;

    @ExtProperty
    @SuppressWarnings("unused")
    private String variantsUuid;

    @ExtProperty
    @SuppressWarnings("unused")
    private Boolean projectsEnabled;

    @ExtProperty
    @SuppressWarnings("unused")
    private Boolean relevancePresent;

    @ExtProperty
    @SuppressWarnings("unused")
    private final String ckeditorUrl;

    @ExtProperty
    @SuppressWarnings("unused")
    private final String ckeditorTimestamp;

    @ExtProperty
    @SuppressWarnings("unused")
    private static final String ANTI_CACHE = WebApplicationHelper.APPLICATION_HASH;

    private ExtStoreFuture<Object> channelStoreFuture;

    private final EditorOpenListener EDITOR_OPEN_LISTENER = new EditorOpenListener();

    private final LinkPicker linkPicker;
    private final ImagePicker imagePicker;
    private final RichTextLinkPicker richTextLinkPicker;
    private final RichTextImageVariantPicker richTextImagePicker;

    public ChannelEditor(final IPluginContext context, final IPluginConfig config, final String apiUrlPrefix,
                         final ExtStoreFuture<Object> channelStoreFuture) {

        this.channelStoreFuture = channelStoreFuture;
        this.apiUrlPrefix = apiUrlPrefix;
        this.locale = Session.get().getLocale().toString();

        final UserSession userSession = UserSession.get();

        final TimeZone timeZone = userSession.getClientInfo().getProperties().getTimeZone();
        if (timeZone != null) {
            this.timeZone = timeZone.getID();
        }

        setUserData(userSession.getJcrSession());
        
        this.cmsVersion = new SystemInfoDataProvider().getReleaseVersion();

        this.debug = Application.get().getDebugSettings().isAjaxDebugModeEnabled();

        this.ckeditorUrl = CKEditorConstants.getCKEditorJsReference().getUrl().toString();
        this.ckeditorTimestamp = CKEditorConstants.CKEDITOR_TIMESTAMP;

        final String channelEditorId = getMarkupId();

        String variantsPath = null;
        if (config != null) {
            variantsPath = config.getString("variantsPath");
            this.initialHstConnectionTimeout = config.getLong("initialHstConnectionTimeout", DEFAULT_INITIAL_CONNECTION_TIMEOUT);
            this.extAjaxTimeout = config.getLong("extAjaxTimeoutMillis", DEFAULT_EXT_AJAX_TIMEOUT);
            registerEditorOpenListener(context, config);
            getUuid(config.getString("projectsPath")).ifPresent(uuid -> this.projectsEnabled = true);
            addEventListener(OPEN_DOCUMENT_EVENT, new OpenDocumentEditorEventListener(config, context));
            addEventListener(CLOSE_DOCUMENT_EVENT, new CloseDocumentEditorEventListener(config, context, channelEditorId));
        }
        getUuid(variantsPath).ifPresent(uuid -> { 
            this.variantsUuid = uuid; 
            this.relevancePresent = true;
        });

        imagePicker = new ImagePicker(context, channelEditorId);
        add(imagePicker.getBehavior());

        linkPicker = new LinkPicker(context, channelEditorId);
        add(linkPicker.getBehavior());

        richTextLinkPicker = new RichTextLinkPicker(context, channelEditorId);
        add(richTextLinkPicker.getBehavior());

        richTextImagePicker = new RichTextImageVariantPicker(context, channelEditorId);
        add(richTextImagePicker.getBehavior());
    }

    private void setUserData(final javax.jcr.Session session) {
        this.cmsUser = session.getUserID();
        final HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
        try {
            final User user = workspace.getSecurityService().getUser(session.getUserID());
            this.cmsUserFirstName = user.getFirstName();
            this.cmsUserLastName = user.getLastName();
        } catch (RepositoryException e) {
            log.error("Unable to retrieve information of user '{}'.", session.getUserID(), e);
        }

        UserUtils.getUserName(this.cmsUser, session)
                 .ifPresent(userName -> this.cmsUserDisplayName = userName);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(ChannelEditor.class, "plugins/vtabs/VerticalTabPanel.css")));
        response.render(ChannelEditorHeaderItem.get());
    }

    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(
                " try { Ext.namespace(\"%s\"); window.%s = new %s(%s); } catch (e) { Ext.Msg.alert('Error', 'Error creating channel editor. '+e); }; \n",
                getMarkupId(), getMarkupId(), extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.ChannelManager.ChannelEditor.Instance";
    }

    @Override
    protected void onRenderProperties(final JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("channelStoreFuture", new JSONIdentifier(channelStoreFuture.getJsObjectId()));
        properties.put("imagePickerWicketUrl", imagePicker.getBehavior().getCallbackUrl().toString());
        properties.put("linkPickerWicketUrl", linkPicker.getBehavior().getCallbackUrl().toString());
        properties.put("richTextImagePickerWicketUrl", richTextImagePicker.getBehavior().getCallbackUrl().toString());
        properties.put("richTextLinkPickerWicketUrl", richTextLinkPicker.getBehavior().getCallbackUrl().toString());
        properties.put("extensions", loadExtensions());
    }

    private JSONArray loadExtensions() {
        final UiExtensionLoader loader = new JcrUiExtensionLoader(UserSession.get().getJcrSession());
        return JSON_ORG_OBJECT_MAPPER.convertValue(loader.loadUiExtensions(), JSONArray.class);
    }

    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        switch (event) {
            case OPEN_DOCUMENT_EVENT:
                return OpenDocumentEditorEventListener.getExtEventBehavior();
            case CLOSE_DOCUMENT_EVENT:
                return CloseDocumentEditorEventListener.getExtEventBehavior();
            default:
                return super.newExtEventBehavior(event);
        }
    }

    public void viewChannel(final String channelId, final String initialPath, final String branchId) {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            final String loadChannelScript = String.format("Ext.getCmp('%s').initChannel('%s', '%s', '%s');",
                    getMarkupId(), channelId, initialPath, branchId);
            target.appendJavaScript(loadChannelScript);
            // N.B. actually loading the channel is triggered by the activation of the ChannelManagerPerspective
        }
    }

    private static Optional<String> getUuid(final String path) {
        String result = null;
        if (StringUtils.isNotEmpty(path)) {
            final javax.jcr.Session session = UserSession.get().getJcrSession();
            try {
                if (session.nodeExists(path)) {
                    result =  session.getNode(path).getIdentifier();
                } else {
                    log.info("No node at '{}': variants will not be available.", path);
                }
            } catch (RepositoryException e) {
                log.error("Failed to retrieve variants node '" + path + "'", e);
            }
        } else {
            log.info("Variants path not configured. Only the default variant will be available.");
        }
        return Optional.ofNullable(result);
    }

    @Override
    public void detachModels() {
        super.detachModels();
        imagePicker.detach();
        linkPicker.detach();
        richTextLinkPicker.detach();
        richTextImagePicker.detach();
    }

    /**
     * Due to the ordering of the CMS perspectives (Channel Manager before Content), the ChannelEditor is
     * instantiated before the EditorManagerPlugin. We can therefore not use its registration service
     * immediately. Instead, we register a service tracker, which catches the event that the EditorManagerPlugin
     * is registered as a service, so we can register our listener.
     */
    private void registerEditorOpenListener(final IPluginContext context, final IPluginConfig config) {
        final String editorManagerServiceId = config.getString(IEditorManager.EDITOR_ID, "service.edit");

        context.registerTracker(new IServiceTracker<IClusterable>() {
            @Override
            public void addService(final IClusterable service, final String name) {
                if (service instanceof IEditorManager) {
                    final IEditorManager editorManager = (IEditorManager) service;
                    editorManager.registerOpenListener(EDITOR_OPEN_LISTENER);
                }
            }

            @Override
            public void removeService(final IClusterable service, final String name) {
                if (service instanceof IEditorManager) {
                    final IEditorManager editorManager = (IEditorManager) service;
                    editorManager.unregisterOpenListener(EDITOR_OPEN_LISTENER);
                }
            }
        }, editorManagerServiceId);
    }

    private class EditorOpenListener implements IEditorOpenListener, Serializable {
        @Override
        public void onOpen(final IModel<Node> model) {
            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                try {
                    final String killEditorScript = String.format("Ext.getCmp('%s').killEditor('%s');",
                            getMarkupId(), model.getObject().getIdentifier());
                    target.appendJavaScript(killEditorScript);
                } catch (RepositoryException e) {
                    log.warn("Failed to retrieve UUID from editor's node model", e);
                }
            }
        }
    }
}
