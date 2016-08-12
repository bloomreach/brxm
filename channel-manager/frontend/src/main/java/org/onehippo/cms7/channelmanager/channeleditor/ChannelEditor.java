/*
 *  Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
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

    private static final String OPEN_DOCUMENT_EDITOR_EVENT = "open-document-editor";

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
    private Boolean hideHstConfigEditor;

    @ExtProperty
    @SuppressWarnings("unused")
    private static final String ANTI_CACHE = WebApplicationHelper.APPLICATION_HASH;

    private ExtStoreFuture<Object> channelStoreFuture;

    public ChannelEditor(final IPluginContext context, final IPluginConfig config, final String apiUrlPrefix,
                         final ExtStoreFuture<Object> channelStoreFuture, final String[] contextPaths) {

        this.channelStoreFuture = channelStoreFuture;
        this.apiUrlPrefix = apiUrlPrefix;
        this.contextPaths = contextPaths;
        this.locale = Session.get().getLocale().toString();
        this.debug = Application.get().getDebugSettings().isAjaxDebugModeEnabled();
        this.locale = Session.get().getLocale().toString();
        this.cmsUser = UserSession.get().getJcrSession().getUserID();

        String variantsPath = null;
        if (config != null) {
            variantsPath = config.getString("variantsPath");
            this.initialHstConnectionTimeout = config.getLong("initialHstConnectionTimeout", DEFAULT_INITIAL_CONNECTION_TIMEOUT);
            this.extAjaxTimeout = config.getLong("extAjaxTimeoutMillis", DEFAULT_EXT_AJAX_TIMEOUT);
        }
        this.variantsUuid = getVariantsUuidOrNull(variantsPath);
        // TODO: decide how to show hide hst-config-editor. Probably a config option in ChannelEditor constructor
        // and a message from the ng app (a click) to show the hst-config-editor card
        this.hideHstConfigEditor = true;

        addEventListener(OPEN_DOCUMENT_EDITOR_EVENT, new OpenDocumentEditorEventListener(config, context));
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
    }

    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        if (OPEN_DOCUMENT_EDITOR_EVENT.equals(event)) {
            return OpenDocumentEditorEventListener.getExtEventBehavior();
        }
        return super.newExtEventBehavior(event);
    }

    public void viewChannel(final String channelId, final String initialPath) {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            final String loadChannelScript = String.format("Ext.getCmp('%1s').loadChannel('%2s', '%3s');",
                    getMarkupId(), channelId, initialPath);
            target.appendJavaScript(loadChannelScript);
        }
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
}
