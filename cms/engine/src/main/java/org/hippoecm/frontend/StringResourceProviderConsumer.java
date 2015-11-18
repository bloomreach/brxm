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
package org.hippoecm.frontend;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An IStringResourceLoader that uses the first IStringResourceProvider parent of
 * a Component to translate keys.
 * <p>
 * The keys can be in the format "realKey[,prop1=value1[,prop2=value2[...]]]".
 * In this case, the prop-value pairs are used as additional criteria in the
 * search for a translation.
 * @deprecated since 3.2.0. Use repository-based resource bundle service.
 * See {@link org.hippoecm.frontend.l10n.ResourceBundleModel}
 */
@Deprecated
public class StringResourceProviderConsumer implements IStringResourceLoader {

    private static final Logger log = LoggerFactory.getLogger(StringResourceProviderConsumer.class);

    @Override
    public String loadStringResource(final Component component, final String key, final Locale locale, final String style, final String variation) {
        IStringResourceProvider provider = null;
        if (component instanceof IStringResourceProvider) {
            provider = (IStringResourceProvider) component;
        } else if (component != null) {
            provider = component.findParent(IStringResourceProvider.class);
        }
        if (provider != null) {
            String result = provider.getString(getKeysMap(component, key, locale));
            if (result != null) {
                log.warn("Deprecated translation resolution: key = {}, component = {}. Use ResourceBundleModel instead.", key, component);
            }
            return result;
        }
        return null;
    }

    @Deprecated
    private Map<String, String> getKeysMap(final Component component, final String key, final Locale locale) {
        Map<String, String> keys = new TreeMap<>();
        String realKey;
        if (key.indexOf(',') > 0) {
            realKey = key.substring(0, key.indexOf(','));
            for (Map.Entry<String, Object> entry : new ValueMap(key.substring(key.indexOf(',') + 1)).entrySet()) {
                if (entry.getValue() instanceof String) {
                    keys.put(entry.getKey(), (String) entry.getValue());
                }
            }
        } else {
            realKey = key;
        }
        keys.put(HippoNodeType.HIPPO_KEY, realKey);
        keys.put(HippoNodeType.HIPPO_LANGUAGE, locale.getLanguage());

        String value = locale.getCountry();
        if (value != null) {
            keys.put("country", locale.getCountry());
        }

        value = locale.getVariant();
        if (value != null) {
            keys.put("variant", locale.getVariant());
        }

        value = component.getStyle();
        if (value != null) {
            keys.put("style", value);
        }
        return keys;
    }

    @Override
    public String loadStringResource(final Class<?> clazz, final String key, final Locale locale, final String style, final String variation) {
        return null;
    }

}
