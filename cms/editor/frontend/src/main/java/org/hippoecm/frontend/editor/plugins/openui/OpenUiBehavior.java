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
package org.hippoecm.frontend.editor.plugins.openui;

import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.util.UserUtils;
import org.onehippo.cms7.openui.extensions.JcrUiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtension;
import org.onehippo.cms7.openui.extensions.UiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtensionPoint;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Loads a {@link UiExtension} in an iframe created by the Penpal library.
 *
 * The parent side of the UI extension must be implemented in a JavaScript class
 * that resides in a file with the same simple name as the provided {@link OpenUiPlugin},
 * located in the same package. For example, an OpenUI plugin 'OpenUiDummy.java'
 * will need an 'OpenUiDummy.js" sibling file that defines a JavaScript class called 'OpenUiDummy'.
 *
 * The JavaScript class must be registered with 'OpenUi.registerClass()' and needs to implement
 * the following methods:
 *
 * - constructor(parameters)
 *   The parameters contain the JavaScript parameters defined by the Open UI plugin
 *   plus the default parameters defined in this OpenUiBehavior class.
 *
 * - onConnect(connection)
 *   Called when the Penpal connection has been created (which is passed)
 *
 * - onDestroy()
 *   Called when the Penpal connection has been destroyed
 *
 * - getMethods()
 *   Returns the object with Penpal method definitions specific to the loaded UI extension.
 */
class OpenUiBehavior extends Behavior {

    private static final Logger log = LoggerFactory.getLogger(OpenUiBehavior.class);

    private final UiExtension extension;
    private final OpenUiPlugin openUiPlugin;

    OpenUiBehavior(final OpenUiPlugin openUiPlugin, final String extensionName, final UiExtensionPoint extensionPoint) {
        this.openUiPlugin = openUiPlugin;
        this.openUiPlugin.setOutputMarkupId(true);

        extension = loadUiExtension(extensionName, extensionPoint).orElse(null);
    }

    private static Optional<UiExtension> loadUiExtension(final String extensionName, final UiExtensionPoint extensionPoint) {
        if (StringUtils.isBlank(extensionName)) {
            return Optional.empty();
        }
        final UiExtensionLoader loader = new JcrUiExtensionLoader(UserSession.get().getJcrSession());
        return loader.loadUiExtension(extensionName, extensionPoint);
    }

    UiExtension getUiExtension() {
        return extension;
    }

    boolean isActive() {
        return extension != null;
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        if (isActive()) {
            response.render(new OpenUiBehaviorHeaderItem());
            response.render(JavaScriptReferenceHeaderItem.forReference(getJavaScriptResourceReference()));
            response.render(OnDomReadyHeaderItem.forScript(createJavaScript(component)));
        }
    }

    private JavaScriptResourceReference getJavaScriptResourceReference() {
        return new JavaScriptResourceReference(openUiPlugin.getClass(), getJavaScriptFileName());
    }

    private String getJavaScriptFileName() {
        return getJavaScriptClassName() + ".js";
    }

    private String getJavaScriptClassName() {
        return openUiPlugin.getClass().getSimpleName();
    }

    private String createJavaScript(final Component component) {
        final ObjectNode parameters = openUiPlugin.getJavaScriptParameters();
        parameters.put("extensionConfig", extension.getConfig());
        parameters.put("extensionUrl", extension.getUrl());

        parameters.put("iframeParentId", component.getMarkupId());

        final UserSession userSession = UserSession.get();

        addCmsVariables(parameters, userSession);
        addUserVariables(parameters, userSession);

        return "OpenUi.showExtension('" + getJavaScriptClassName() + "', " + parameters.toString() + ");";
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
