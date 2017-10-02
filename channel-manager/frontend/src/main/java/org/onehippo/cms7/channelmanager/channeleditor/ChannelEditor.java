/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IEditorOpenListener;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.ChannelEditor.ChannelEditor")
public class ChannelEditor extends ExtPanel {
    public static final Logger log = LoggerFactory.getLogger(ChannelEditor.class);

    private static final long DEFAULT_INITIAL_CONNECTION_TIMEOUT = 60000L;
    private static final long DEFAULT_EXT_AJAX_TIMEOUT = 30000L;

    private static final String OPEN_DOCUMENT_EVENT = "open-document";
    private static final String CLOSE_DOCUMENT_EVENT = "close-document";

    @ExtProperty
    @SuppressWarnings("unused")
    private Boolean debug = false;

    @ExtProperty
    @SuppressWarnings("unused")
    private String locale;

    @ExtProperty
    @SuppressWarnings("unused")
    private String apiUrlPrefix;

    @ExtProperty
    @SuppressWarnings("unused")
    private final String[] contextPaths;

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsUser;

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
    private final String ckeditorUrl;

    @ExtProperty
    @SuppressWarnings("unused")
    private final String ckeditorTimestamp;

    @ExtProperty
    @SuppressWarnings("unused")
    private static final String ANTI_CACHE = WebApplicationHelper.APPLICATION_HASH;

    private ExtStoreFuture<Object> channelStoreFuture;
    private final LinkPickerManager linkPickerManager;
    private final ImageVariantPickerManager imageVariantPickerManager;
    private final ImagePickerManager imagePickerManager;
    private final EditorOpenListener EDITOR_OPEN_LISTENER = new EditorOpenListener();

    public ChannelEditor(final IPluginContext context, final IPluginConfig config, final String apiUrlPrefix,
                         final ExtStoreFuture<Object> channelStoreFuture, final String[] contextPaths) {

        this.channelStoreFuture = channelStoreFuture;
        this.apiUrlPrefix = apiUrlPrefix;
        this.contextPaths = contextPaths;
        this.locale = Session.get().getLocale().toString();
        this.debug = Application.get().getDebugSettings().isAjaxDebugModeEnabled();
        this.cmsUser = UserSession.get().getJcrSession().getUserID();
        this.ckeditorUrl = CKEditorConstants.getCKEditorJsReference().getUrl().toString();
        this.ckeditorTimestamp = CKEditorConstants.CKEDITOR_TIMESTAMP;

        String variantsPath = null;
        if (config != null) {
            variantsPath = config.getString("variantsPath");
            this.initialHstConnectionTimeout = config.getLong("initialHstConnectionTimeout", DEFAULT_INITIAL_CONNECTION_TIMEOUT);
            this.extAjaxTimeout = config.getLong("extAjaxTimeoutMillis", DEFAULT_EXT_AJAX_TIMEOUT);
            registerEditorOpenListener(context, config);
        }
        getUuid(variantsPath).ifPresent(uuid -> this.variantsUuid = uuid);
        Optional.of(config).ifPresent(
                (c -> getUuid(c.getString("projectsPath"))
                        .ifPresent(uuid -> this.projectsEnabled = true))
        );

        addEventListener(OPEN_DOCUMENT_EVENT, new OpenDocumentEditorEventListener(config, context));
        addEventListener(CLOSE_DOCUMENT_EVENT, new CloseDocumentEditorEventListener(config, context, getMarkupId()));

        linkPickerManager = new LinkPickerManager(context, getMarkupId());
        add(linkPickerManager.getBehavior());

        imageVariantPickerManager = new ImageVariantPickerManager(context, getMarkupId());
        add(imageVariantPickerManager.getBehavior());

        imagePickerManager = new ImagePickerManager(context, getMarkupId());
        add(imagePickerManager.getBehavior());
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(ChannelEditor.class, "plugins/colorfield/colorfield.css")));
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
        properties.put("channelStoreFuture", new JSONIdentifier(this.channelStoreFuture.getJsObjectId()));
        properties.put("linkPickerWicketUrl", this.linkPickerManager.getBehavior().getCallbackUrl().toString());
        properties.put("imageVariantPickerWicketUrl", this.imageVariantPickerManager.getBehavior().getCallbackUrl().toString());
        properties.put("imagePickerWicketUrl", this.imagePickerManager.getBehavior().getCallbackUrl().toString());
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
            final String loadChannelScript = String.format("Ext.getCmp('%s').loadChannel('%s', '%s', '%s');",
                    getMarkupId(), channelId, initialPath, branchId);
            target.appendJavaScript(loadChannelScript);
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

            @Override
            public void updateService(final IClusterable service, final String name) {

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
