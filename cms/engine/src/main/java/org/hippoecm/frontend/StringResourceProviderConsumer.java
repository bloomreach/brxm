/*
 *  Copyright 2010 Hippo.
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
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * An IStringResourceLoader that uses the first IStringResourceProvider parent of
 * a Component to translate keys.
 * <p>
 * The keys can be in the format "realKey[,prop1=value1[,prop2=value2[...]]]".
 * In this case, the prop-value pairs are used as additional criteria in the
 * search for a translation.
 */
public class StringResourceProviderConsumer implements IStringResourceLoader {

    public String loadStringResource(Component component, String key) {
        IStringResourceProvider provider;
        if (component instanceof IStringResourceProvider) {
            provider = (IStringResourceProvider) component;
        } else if (component != null) {
            provider = component.findParent(IStringResourceProvider.class);
        } else {
            return null;
        }
        if (provider != null) {
            Map<String, String> keys = new TreeMap<String, String>();
            String realKey;
            if (key.indexOf(',') > 0) {
                realKey = key.substring(0, key.indexOf(','));
                IValueMap map = new ValueMap(key.substring(key.indexOf(',') + 1));
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        keys.put(entry.getKey(), (String) entry.getValue());
                    }
                }
            } else {
                realKey = key;
            }
            keys.put(HippoNodeType.HIPPO_KEY, realKey);

            Locale locale = component.getLocale();
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

            String result = provider.getString(keys);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public String loadStringResource(Class clazz, String key, Locale locale, String style) {
        return null;
    }
}
