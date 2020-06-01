/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.plugins.social;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.service.IPopupService;

/**
 * Provides access to a social medium. URLs are shared by opening a medium-specific URL in a popup window.
 */
public class PopupSocialMedium implements ISocialMedium {

    private final String displayName;
    private final String shareUrlPrefix;
    private final IPopupService popupService;

    public PopupSocialMedium(String displayName, String shareUrlPrefix, IPopupService popupService) {
        this.displayName = displayName;
        this.shareUrlPrefix = shareUrlPrefix;
        this.popupService = popupService;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void shareUrl(final String url) {
        popupService.openPopupWindow(new PopupSettings(IPopupService.DEFAULT_POPUP_SETTINGS), shareUrlPrefix + url);
    }

    @Override
    public ResourceReference getIcon16() {
        final String iconName = StringUtils.lowerCase(displayName) + "-icon-16.png";
        return new PackageResourceReference(getClass(), iconName);
    }

    @Override
    public int compareTo(final ISocialMedium medium) {
        return displayName.compareTo(medium.getDisplayName());
    }
}
