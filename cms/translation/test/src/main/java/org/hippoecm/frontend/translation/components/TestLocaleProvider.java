/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.translation.ILocaleProvider;

public final class TestLocaleProvider implements ILocaleProvider {

    private static final long serialVersionUID = 1L;

    private Map<String, HippoLocale> locales = new TreeMap<String, HippoLocale>();

    public TestLocaleProvider() {
        locales.put("en", createLocale("en", "UK", "English"));
        locales.put("fr", createLocale("fr", "FR", "French"));
        locales.put("nl", createLocale("nl", "NL", "Dutch"));
    }

    private HippoLocale createLocale(final String key, String country, final String name) {
        return new HippoLocale(new Locale(key, country), key) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getDisplayName(Locale locale) {
                return name;
            }

            @Override
            public ResourceReference getIcon(IconSize size, LocaleState type) {
                return new ResourceReference(TestLocaleProvider.class, key + ".png");
            }

        };
    }

    public HippoLocale getLocale(String name) {
        return locales.get(name);
    }

    public List<? extends HippoLocale> getLocales() {
        return new ArrayList<HippoLocale>(locales.values());
    }
}
