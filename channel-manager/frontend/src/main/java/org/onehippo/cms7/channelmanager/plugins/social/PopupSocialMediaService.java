/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.plugins.social;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IPopupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to social media by opening medium-specific URLs in a popup window (e.g. to share a URL on
 * that medium). Social media can be configured in social service node under
 * <blockquote>
 * /hippo:configuration/hippo:frontend/cms/cms-services/popupSocialMediaService
 * </blockquote>
 * By default following services are configured:
 * <ul>
 * <li>Facebook</li>
 * <li>LinkedIn</li>
 * <li>Twitter</li>
 * </ul>
 * Additional services can be configured by adding plugin configuration child nodes (see the existing ones for
 * examples). Only media that have the property 'enabled' set to 'true' are actually used.</p>
 * <p>
 * URLs shared via a social medium are opened using a {@link IPopupService}. The service ID of the popup service
 * to use can be configured via the property 'popup.service.id'.</p>
 *
 * @author mdenburger
 * @author vkumar
 */
public class PopupSocialMediaService extends Plugin implements ISocialMediaService {

    private static final Logger log = LoggerFactory.getLogger(PopupSocialMediaService.class);

    private static final String CONFIG_POPUP_SERVICE_ID = "popup.service.id";
    private static final String MEDIUM_DISPLAY_NAME = "display.name";
    private static final String MEDIUM_SHARE_URL = "share.url";
    private static final String MEDIUM_ENABLED = "enabled";

    private final IPopupService popupService;
    private List<ISocialMedium> socialMedia;

    public PopupSocialMediaService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String popupServiceId = config.getString(CONFIG_POPUP_SERVICE_ID, IPopupService.class.getName());
        log.debug("Retrieving popup service with ID '{}'", popupServiceId);
        popupService = context.getService(popupServiceId, IPopupService.class);
        if (popupService == null) {
            throw new IllegalStateException("No popup service configured for the social service plugin. "
                    + "Please set the configuration property " + CONFIG_POPUP_SERVICE_ID);
        }

        this.socialMedia = new LinkedList<ISocialMedium>();
        readSocialMedia(config, socialMedia);

        final String serviceId = config.getString("service.id", ISocialMediaService.DEFAULT_SERVICE_ID);
        context.registerService(this, serviceId);
    }

    protected void readSocialMedia(IPluginConfig config, List<ISocialMedium> socialMedia) {
        for (IPluginConfig socialMediumConfig : config.getPluginConfigSet()) {
            final String displayName = socialMediumConfig.getString(MEDIUM_DISPLAY_NAME);
            final String shareUrl = socialMediumConfig.getString(MEDIUM_SHARE_URL);
            final boolean enabled = socialMediumConfig.getAsBoolean(MEDIUM_ENABLED, false);

            if (StringUtils.isNotEmpty(displayName) && StringUtils.isNotEmpty(shareUrl) && enabled) {
                socialMedia.add(new PopupSocialMedium(displayName, shareUrl, popupService));
            }
        }
    }

    @Override
    public List<ISocialMedium> getAllSocialMedia() {
        return socialMedia;
    }
}
