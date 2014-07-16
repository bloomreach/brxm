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
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.wicket.Component;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.util.value.IValueMap;
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
 */
public class StringResourceProviderConsumer implements IStringResourceLoader {

    private static final Logger log = LoggerFactory.getLogger(StringResourceProviderConsumer.class);

    private Cache<CacheKey, Optional<String>> stringResourceCache =
            CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(5, TimeUnit.MINUTES).build();

    @Override
    public String loadStringResource(final Component component, final String key, final Locale locale, final String style, final String variation) {
        IStringResourceProvider provider;
        long start = System.nanoTime();
        if (component instanceof IStringResourceProvider) {
            provider = (IStringResourceProvider) component;
        } else if (component != null) {
            provider = component.findParent(IStringResourceProvider.class);
        } else {
            return null;
        }
        if (provider != null) {

            CacheKey cacheKey =  new CacheKey(provider.getClass().getName(), key, locale);
            final Optional<String> optional = stringResourceCache.getIfPresent(cacheKey);
            if (optional != null) {
                final String s = optional.orNull();
                log.info("Found cached value '{}' for key '{}'",s, cacheKey);
                return s;
            }
            Map<String, String> keys = new TreeMap<>();
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
                if (log.isInfoEnabled()) {
                    log.info("Took {} milliseconds to find resource '{}' for key '{}'.", ((System.nanoTime() - start)/1000000.0), result, cacheKey);
                }
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Took {} milliseconds to not find resource for key '{}'.", ((System.nanoTime() - start)/1000000.0), cacheKey);
                }
            }
            // regardless null or not, cache result
            stringResourceCache.put(cacheKey, Optional.fromNullable(result));
            return result;
        }
        return null;
    }

    @Override
    public String loadStringResource(final Class<?> clazz, final String key, final Locale locale, final String style, final String variation) {
        return null;
    }

    private class CacheKey {

        private final String className;
        private final String key;
        private final Locale locale;

        public CacheKey(final String className, final String key, final Locale locale) {
            this.className = className;
            this.key = key;
            this.locale = locale;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CacheKey cacheKey = (CacheKey) o;

            if (!key.equals(cacheKey.key)) {
                return false;
            }
            if (!locale.equals(cacheKey.locale)) {
                return false;
            }
            if (!className.equals(cacheKey.className)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + key.hashCode();
            result = 31 * result + locale.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "CacheKey{" +
                    "className='" + className + '\'' +
                    ", key='" + key + '\'' +
                    ", locale=" + locale +
                    '}';
        }
    }
}
