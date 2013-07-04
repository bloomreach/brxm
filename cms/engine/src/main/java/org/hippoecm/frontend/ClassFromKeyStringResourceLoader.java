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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.wicket.Component;
import org.apache.wicket.core.util.resource.UrlResourceStream;
import org.apache.wicket.core.util.resource.locator.ResourceNameIterator;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An IStringResourceLoader that does not need a class to resolve the corresponding properties.
 * <p>
 * The keys can be in the format "realKey[,prop1=value1[,prop2=value2[...]]]".
 * In this case, the prop-value pairs are used as additional criteria in the
 * search for a translation.
 * <p>
 * The "class" criterium is used to find the resource from the class path.  When the key
 * "exception" has a criterium "type", that is used when the "class" was not available or
 * did not yield a result.
 */
public class ClassFromKeyStringResourceLoader extends ComponentStringResourceLoader {

    static final Logger log = LoggerFactory.getLogger(ClassFromKeyStringResourceLoader.class);

    private final Map<String, java.util.Properties> cache = new ConcurrentHashMap<String, java.util.Properties>();

    private IResourceStream getResourceStream(final String path) {
        // use context classloader when no specific classloader is set
        // (package resources for instance)
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url != null) {
            return new UrlResourceStream(url);
        }

        // use Wicket classloader when no specific classloader is set
        url = getClass().getClassLoader().getResource(path);
        if (url != null) {
            return new UrlResourceStream(url);
        }
        return null;
    }

    private Properties getProperties(String path) {
        if (!cache.containsKey(path)) {
            java.util.Properties properties = new java.util.Properties();
            IResourceStream resourceStream = getResourceStream(path);
            if (resourceStream != null) {
                try {
                    InputStream in = new BufferedInputStream(resourceStream.getInputStream());
                    properties.load(in);
                } catch (IOException e) {
                    log.error("Error reading " + path, e);
                } catch (ResourceStreamNotFoundException e) {
                    log.error("Could not find resource at " + path, e);
                }
            }
            cache.put(path, properties);
        }
        return cache.get(path);
    }

    private String getStringForClass(String realKey, Locale locale, String style, String clazz) {
        // Create the base path
        String path = clazz.replace('.', '/');

        // Iterator over all the combinations
        ResourceNameIterator iter = new ResourceNameIterator(path, style, null, locale, Arrays.asList("properties", "xml"), false);
        while (iter.hasNext()) {
            String newPath = iter.next();
            Properties properties = getProperties(newPath);
            String value = properties.getProperty(realKey);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String loadStringResource(final Component component, final String key,
                                     final Locale locale, final String style, final String variation) {
        if (key.indexOf(',') > 0) {
            List<String> criteria = new LinkedList<String>();
            for (String subKey : key.split(",")) {
                criteria.add(subKey);
            }

            String realKey = key.substring(0, key.indexOf(','));
            ValueMap map = new ValueMap(key.substring(key.indexOf(',') + 1));
            if (map.containsKey("class")) {
                // remove class key from map and criteria
                String clazz = (String) map.remove("class");
                Iterator<String> iter = criteria.iterator();
                while (iter.hasNext()) {
                    if (iter.next().startsWith("class=")) {
                        iter.remove();
                        break;
                    }
                }

                // iterate while no value is found, dropping the last 
                String value = getStringForClass(Strings.join(",", criteria.toArray(new String[criteria.size()])),
                        locale, style, clazz);
                if (value != null) {
                    return value;
                }
            }
            if ("exception".equals(realKey) && map.containsKey("type")) {
                // remove class key from map and criteria
                String clazz = (String) map.remove("type");
                Iterator<String> iter = criteria.iterator();
                while (iter.hasNext()) {
                    if (iter.next().startsWith("type=")) {
                        iter.remove();
                        break;
                    }
                }

                // Load the properties associated with the path
                String value = getStringForClass(Strings.join(",", criteria.toArray(new String[criteria.size()])),
                        locale, style, clazz);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

}
