/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.navitems;

import java.util.Locale;

import org.hippoecm.frontend.navigation.NavigationItem;
import org.onehippo.repository.l10n.LocalizationService;

final class NavigationItemLocalizerImpl implements NavigationItemLocalizer {

    private static final String NAVIGATION_ITEM_DISPLAY_NAME_LOCALES_RESOURCE_BUNDLE = "hippo:navigation.navigationitem.displayName";
    private final LocalizationService localizationService;

    NavigationItemLocalizerImpl(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @Override
    public void localize(NavigationItem navigationItem, final Locale locale) {
        navigationItem.setDisplayName(getDisplayName(navigationItem.getAppPath(), locale));
    }

    private String getDisplayName(String id, Locale locale) {
        return localizationService.getResourceBundle(NAVIGATION_ITEM_DISPLAY_NAME_LOCALES_RESOURCE_BUNDLE, locale).getString(id);
    }

}
