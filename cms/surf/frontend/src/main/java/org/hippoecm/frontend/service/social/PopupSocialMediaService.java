/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.frontend.service.social;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.popup.IPopupService;

/**
 * Provides access to social media by opening medium-specific URLs in a popup window.
 * (e.g. to share a URL on that medium). Currently supported social media are:
 * <ul>
 * <li>Facebook</li>
 * <li>LinkedIn</li>
 * <li>Twitter</li>
 * </ul>
 * The URLs are opened using a {@link IPopupService}. The service ID of the popup service can be configured
 * via the property 'popup.service.id'.
 */
public class PopupSocialMediaService extends Plugin implements ISocialMediaService {

    private static final String CONFIG_POPUP_SERVICE_ID = "popup.service.id";

    private final IPopupService popupService;

    public PopupSocialMediaService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        popupService = context.getService(IPopupService.class.getName(), IPopupService.class);
        if (popupService == null) {
            throw new IllegalStateException("No popup service configured for the social service plugin. "
                    + "Please set the configuration property " + CONFIG_POPUP_SERVICE_ID);
        }

        final String serviceId = config.getString("service.id", ISocialMediaService.DEFAULT_SERVICE_ID);
        context.registerService(this, serviceId);
    }

    @Override
    public List<ISocialMedium> getAllSocialMedia() {
        List<ISocialMedium> media = new ArrayList<ISocialMedium>();

        media.add(new PopupSocialMedium("Facebook", "http://www.facebook.com/sharer.php?u=", popupService));
        media.add(new PopupSocialMedium("LinkedIn", "http://www.linkedin.com/shareArticle?mini=true&url=", popupService));
        media.add(new PopupSocialMedium("Twitter", "http://twitter.com/share?url=", popupService));

        return media;
    }
}
