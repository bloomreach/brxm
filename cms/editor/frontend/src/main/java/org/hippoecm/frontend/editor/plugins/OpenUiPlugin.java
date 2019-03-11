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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.openui.extensions.JcrUiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtension;
import org.onehippo.cms7.openui.extensions.UiExtensionLoader;
import org.onehippo.cms7.openui.extensions.UiExtensionPoint;
import org.onehippo.cms7.openui.extensions.UiExtensionValidator;

public class OpenUiPlugin extends RenderPlugin<String> {

    private static final String UI_EXTENSION = "uiExtension";
    private static final String ERROR_MESSAGE = "Cannot load extension '%s'.";

    public OpenUiPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        
        final String extensionName = config.getString(UI_EXTENSION);
        final Optional<UiExtension> uiExtension = loadUiExtension(extensionName);

        final WebMarkupContainer iframe = new WebMarkupContainer("iframe");
        uiExtension.ifPresent(extension -> iframe.add(new AttributeAppender("src", Model.of(extension.getUrl()))));
        queue(iframe);
        iframe.setVisible(uiExtension.isPresent());

        final Label errorMessage = new Label("errorMessage", Model.of(String.format(ERROR_MESSAGE, extensionName)));
        errorMessage.setVisible(!iframe.isVisible());
        queue(errorMessage);

    }

    private Optional<UiExtension> loadUiExtension(final String uiExtensionName) {
        if (StringUtils.isBlank(uiExtensionName)) {
            return Optional.empty();
        }

        final UiExtensionLoader loader = new JcrUiExtensionLoader(UserSession.get().getJcrSession());
        final UiExtensionValidator validator = new UiExtensionValidator();

        return loader.loadUiExtension(uiExtensionName, UiExtensionPoint.DOCUMENT_FIELD).filter(validator::validate);
    }
}
