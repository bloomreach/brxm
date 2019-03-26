/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins;

import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.util.UserUtils;
import org.onehippo.cms.json.Json;
import org.onehippo.cms7.openui.extensions.JcrUiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtension;
import org.onehippo.cms7.openui.extensions.UiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtensionPoint;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class OpenUiStringPlugin extends RenderPlugin<String> {

    private static final String CONFIG_PROPERTY_UI_EXTENSION = "uiExtension";
    private static final String OPEN_UI_STRING_PLUGIN_JS = "OpenUiStringPlugin.js";
    private static final Logger log = LoggerFactory.getLogger(OpenUiStringPlugin.class);

    private UiExtension extension;
    private String iframeParentId;
    private String hiddenValueId;

    public OpenUiStringPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final HiddenField<String> value = new HiddenField<>("value", getModel());
        value.setOutputMarkupId(true);
        queue(value);
        hiddenValueId = value.getMarkupId();

        final String extensionName = config.getString(CONFIG_PROPERTY_UI_EXTENSION);
        final Optional<UiExtension> uiExtension = loadUiExtension(extensionName);

        final WebMarkupContainer iframeParent = new WebMarkupContainer("iframeParent");
        iframeParent.setOutputMarkupId(true);
        queue(iframeParent);
        iframeParentId = iframeParent.getMarkupId();

        extension = uiExtension.orElse(null);

        final Label errorMessage = new Label("errorMessage", 
                new StringResourceModel("load-error", OpenUiStringPlugin.this).setParameters(extensionName));
        errorMessage.setVisible(!uiExtension.isPresent());
        queue(errorMessage);
    }

    private Optional<UiExtension> loadUiExtension(final String uiExtensionName) {
        if (StringUtils.isBlank(uiExtensionName)) {
            return Optional.empty();
        }
        final UiExtensionLoader loader = new JcrUiExtensionLoader(UserSession.get().getJcrSession());
        return loader.loadUiExtension(uiExtensionName, UiExtensionPoint.DOCUMENT_FIELD);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        if (extension == null) {
            return;
        }

        response.render(new PenpalHeaderItem());

        final JavaScriptResourceReference js = new JavaScriptResourceReference(OpenUiStringPlugin.class, OPEN_UI_STRING_PLUGIN_JS);
        response.render(JavaScriptReferenceHeaderItem.forReference(js));

        response.render(OnDomReadyHeaderItem.forScript(createJavaScriptForField()));
    }

    private String createJavaScriptForField() {
        final ObjectNode parameters = Json.object();
        parameters.put("extensionConfig", extension.getConfig());
        parameters.put("extensionUrl", extension.getUrl());
        parameters.put("iframeParentId", iframeParentId);
        parameters.put("hiddenValueId", hiddenValueId);

        final UserSession userSession = UserSession.get();
        addCmsVariables(parameters, userSession);
        addUserVariables(parameters, userSession);

        return "Hippo.OpenUi.createStringField(" + parameters.toString() + ");";
    }

    private static void addCmsVariables(final ObjectNode parameters, final UserSession userSession) {
        parameters.put("cmsLocale", userSession.getLocale().getLanguage());
        parameters.put("cmsTimeZone", userSession.getTimeZone().getID());
        parameters.put("cmsVersion", new SystemInfoDataProvider().getReleaseVersion());
    }

    private static void addUserVariables(final ObjectNode parameters, final UserSession userSession) {
        final Session jcrSession = userSession.getJcrSession();
        final String userId = jcrSession.getUserID();
        parameters.put("userId", userId);

        final HippoWorkspace workspace = (HippoWorkspace) jcrSession.getWorkspace();
        try {
            final SecurityService securityService = workspace.getSecurityService();
            final User user = securityService.getUser(userId);

            parameters.put("userFirstName", user.getFirstName());
            parameters.put("userLastName", user.getLastName());
        } catch (RepositoryException e) {
            log.warn("Unable to retrieve first and last name of user '{}'", userId, e);
        }

        UserUtils.getUserName(userId, jcrSession)
                .ifPresent(displayName -> parameters.put("userDisplayName", displayName));
    }
}
