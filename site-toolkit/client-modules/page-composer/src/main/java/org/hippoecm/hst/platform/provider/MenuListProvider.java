/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.platform.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ModuleNotFoundException;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuListProvider implements ValueListProvider {

    private final static Logger log = LoggerFactory.getLogger(MenuListProvider.class);

    @Override
    public List<String> getValues() {
        List<String> values;
        try {
            PageComposerContextService pageComposerContextService = HstServices.getComponentManager().getComponent(
                    "pageComposerContextService", "org.hippoecm.hst.pagecomposer");
            values = Optional.ofNullable(pageComposerContextService.getEditingPreviewSite())
                    .map(HstSite::getSiteMenusConfiguration).map(HstSiteMenusConfiguration::getSiteMenuConfigurations)
                    .map(Map::keySet).map(set -> (List<String>) new ArrayList<>(set))
                    .orElseGet(Collections::emptyList);
        } catch (ModuleNotFoundException mnfe) {
            values = Optional.ofNullable(RequestContextProvider.get()).map(HstRequestContext::getHstSiteMenus)
                    .map(HstSiteMenus::getSiteMenus).map(Map::keySet)
                    .map(set -> (List<String>) new ArrayList<>(set)).orElseGet(Collections::emptyList);
        }
        return Collections.unmodifiableList(values);

    }

    @Override
    public String getDisplayValue(final String value) {
        return getDisplayValue(value, null);
    }

    @Override
    public String getDisplayValue(final String value, final Locale locale) {
        return value;
    }
}
