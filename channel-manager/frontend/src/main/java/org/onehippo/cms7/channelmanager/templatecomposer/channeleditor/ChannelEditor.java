/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.channeleditor;

import java.security.AccessControlException;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.onehippo.cms7.channelmanager.templatecomposer.TemplateComposerHeaderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.ExtPropertyConverter;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.TemplateComposer.ChannelEditor")
public class ChannelEditor extends ExtPanel {
    public static final Logger log = LoggerFactory.getLogger(ChannelEditor.class);

    private static final long DEFAULT_INITIAL_CONNECTION_TIMEOUT = 60000L;
    private static final long DEFAULT_EXT_AJAX_TIMEOUT = 30000L;

    private static final Boolean DEFAULT_PREVIEW_MODE = Boolean.TRUE;

    @ExtProperty
    private Boolean debug = false;

    @ExtProperty
    @SuppressWarnings("unused")
    private String locale;

    @ExtProperty
    @SuppressWarnings("unused")
    private String apiUrlPrefix;

    @ExtProperty
    @SuppressWarnings("unused")
    private String channelId;

    @ExtProperty
    @SuppressWarnings("unused")
    private String channelPath;

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsUser;

    @ExtProperty
    @SuppressWarnings("unused")
    private Boolean canManageChanges = Boolean.FALSE;

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
    private Boolean previewMode = DEFAULT_PREVIEW_MODE;

    private ExtStoreFuture<Object> channelStoreFuture;
    private boolean redraw = false;

    public ChannelEditor(final IPluginContext context, final IPluginConfig config, final String apiUrlPrefix,
                         final ExtStoreFuture<Object> channelStoreFuture) {

        this.channelStoreFuture = channelStoreFuture;
        this.apiUrlPrefix = apiUrlPrefix;
        //this.contextPath = defaultContextPath;
        this.locale = Session.get().getLocale().toString();
        this.debug = Application.get().getDebugSettings().isAjaxDebugModeEnabled();
        this.locale = Session.get().getLocale().toString();
        this.cmsUser = UserSession.get().getJcrSession().getUserID();

        String variantsPath = null;
        if (config != null) {
            variantsPath = config.getString("variantsPath");
            this.initialHstConnectionTimeout = config.getLong("initialHstConnectionTimeout", DEFAULT_INITIAL_CONNECTION_TIMEOUT);
            this.extAjaxTimeout = config.getLong("extAjaxTimeoutMillis", DEFAULT_EXT_AJAX_TIMEOUT);
            this.previewMode = config.getAsBoolean("previewMode", DEFAULT_PREVIEW_MODE);
            this.canManageChanges = canManageChanges(this.cmsUser, config);
        }
        this.variantsUuid = getVariantsUuidOrNull(variantsPath);
        // TODO: decide how to show hide hst-config-editor. Probably a config option in ChannelEditor constructor
        // and a message from the ng app (a click) to show the hst-config-editor card
        this.hideHstConfigEditor = true;

        //TODO: register event listener (like edit-document)
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(TemplateComposerHeaderItem.get());
    }

    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(
                " try { Ext.namespace(\"%s\"); window.%s = new %s(%s); } catch (e) { Ext.Msg.alert('Error', 'Error creating channel editor. '+e); }; \n",
                getMarkupId(), getMarkupId(), extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.ChannelManager.TemplateComposer.Instance";
    }

    @Override
    protected void onRenderProperties(final JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("channelStoreFuture", new JSONIdentifier(this.channelStoreFuture.getJsObjectId()));
    }

    public void render(final PluginRequestTarget target) {
        if (redraw) {
            JSONObject update = new JSONObject();
            ExtPropertyConverter.addProperties(this, getClass(), update);
            target.appendJavaScript("Ext.getCmp('" + getMarkupId() + "').loadChannel(" + update.toString() + ");");
            redraw = false;
        }
    }

    public void setChannelId(final String channelId) {
        this.channelId = channelId;
        redraw();
    }

    public void setChannelPath(final String channelPath) {
        this.channelPath = channelPath;
        redraw();
    }

    public void setPreviewMode(final Boolean previewMode) {
        this.previewMode = previewMode;
        redraw();
    }

    public void redraw() {
        redraw = true;
    }

    private boolean canManageChanges(final String user, final IPluginConfig config) {
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
                log.warn("Problems while checking if user '{}' is allowed to {}, assuming user is not allowed.", user, description, e);
            } else {
                log.warn("Problems while checking if user '{}' is allowed to {}, assuming user is not allowed. {}",
                        user, description, e);
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
}
