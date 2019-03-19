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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.openui.extensions.JcrUiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtension;
import org.onehippo.cms7.openui.extensions.UiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtensionPoint;

public class OpenUiStringPlugin extends RenderPlugin<String> {

    private static final String UI_EXTENSION = "uiExtension";
    private static final String ERROR_MESSAGE = "Cannot load extension '%s'.";
    private String extensionUrl = null;
    private String iframeParentId;
    
    // penpal.js is fetched by npm
    private static final ResourceReference PENPAL_JS = new JavaScriptResourceReference(OpenUiStringPlugin.class, 
            "penpal.js");
    private final PackageTextTemplate PENPAL_CONNECT_TEMPLATE = new PackageTextTemplate(OpenUiStringPlugin.class,
            "penpalConnect.js");

    public OpenUiStringPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        
        final String extensionName = config.getString(UI_EXTENSION);
        final Optional<UiExtension> uiExtension = loadUiExtension(extensionName);

        final WebMarkupContainer iframeParent = new WebMarkupContainer("iframeParent");
        iframeParent.setOutputMarkupId(true);
        queue(iframeParent);
        iframeParentId = iframeParent.getMarkupId();

        uiExtension.ifPresent(extension -> extensionUrl = extension.getUrl());

        final Label errorMessage = new Label("errorMessage", Model.of(String.format(ERROR_MESSAGE, extensionName)));
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
        if (extensionUrl == null) {
            return;
        }
        
        response.render(JavaScriptReferenceHeaderItem.forReference(PENPAL_JS));

        final Map<String, String> variables = new HashMap<>();
        variables.put("extensionUrl", extensionUrl);
        variables.put("iframeParentId", iframeParentId);
        variables.put("userLocale", UserSession.get().getLocale().getLanguage());
        variables.put("userTimezone", UserSession.get().getTimeZone().getID());
        
        final String script = PENPAL_CONNECT_TEMPLATE.asString(variables);
                
        response.render(OnDomReadyHeaderItem.forScript(script));
    }

}
