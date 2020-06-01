/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.util;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * PropertyParser
 * 
 * @version $Id$
 */
public class PropertyParser {

    public final static String DEFAULT_PLACEHOLDER_PREFIX = "${";
    public final static String DEFAULT_PLACEHOLDER_SUFFIX = "}";
    public final static String DEFAULT_VALUE_SEPARATOR = null;
    // we want an exception for unresolved properties, this use 'false'
    public final static boolean DEFAULT_IGNORE_UNRESOLVABLE_PLACEHOLDERS = false;

    private final static Logger log = LoggerFactory.getLogger(PropertyParser.class);

    private final static PropertyPlaceholderHelper DEFAULT_PROPERTY_PLACE_HOLDER_HELPER = new PropertyPlaceholderHelper(
            DEFAULT_PLACEHOLDER_PREFIX, DEFAULT_PLACEHOLDER_SUFFIX, DEFAULT_VALUE_SEPARATOR,
            DEFAULT_IGNORE_UNRESOLVABLE_PLACEHOLDERS);

    private Properties properties;

    private String placeHolderPrefix;
    private String placeHolderSuffix;
    private String valueSeparator;
    private boolean ignoreUnresolvablePlaceholders;

    private PropertyPlaceholderHelper propertyPlaceHolderHelper;

    private PlaceholderResolver placeholderResolver;

    public PropertyParser(Properties properties) {
        this(properties, DEFAULT_PLACEHOLDER_PREFIX, DEFAULT_PLACEHOLDER_SUFFIX, DEFAULT_VALUE_SEPARATOR,
                DEFAULT_IGNORE_UNRESOLVABLE_PLACEHOLDERS);
    }

    public PropertyParser(Properties properties, String placeHolderPrefix, String placeHolderSuffix, String valueSeparator, boolean ignoreUnresolvablePlaceholders) {
        this.properties = properties;
        this.placeHolderPrefix = placeHolderPrefix;
        this.placeHolderSuffix = placeHolderSuffix;
        this.valueSeparator = valueSeparator;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
        
        if (StringUtils.equals(DEFAULT_PLACEHOLDER_PREFIX, this.placeHolderPrefix)
                && StringUtils.equals(DEFAULT_PLACEHOLDER_SUFFIX, this.placeHolderSuffix)
                && StringUtils.equals(DEFAULT_VALUE_SEPARATOR, this.valueSeparator)
                && DEFAULT_IGNORE_UNRESOLVABLE_PLACEHOLDERS == this.ignoreUnresolvablePlaceholders) {
            propertyPlaceHolderHelper = DEFAULT_PROPERTY_PLACE_HOLDER_HELPER;
        } else {
            propertyPlaceHolderHelper = new PropertyPlaceholderHelper(this.placeHolderPrefix, this.placeHolderSuffix,
                    this.valueSeparator, this.ignoreUnresolvablePlaceholders);
        }
    }

    public void setPlaceholderResolver(PlaceholderResolver placeholderResolver) {
        this.placeholderResolver = placeholderResolver;
    }

    public Object resolveProperty(String name, Object o) {
        if (o == null) {
            return null;
        }

        if (placeholderResolver == null) {
            if (properties != null) {
                placeholderResolver = new PlaceholderResolver() {
                    public String resolvePlaceholder(String placeholderName) {
                        return properties.getProperty(placeholderName);
                    }
                };
            }
        }

        if (placeholderResolver == null) {
            return o;
        }

        if (o instanceof String) {

            String s = (String) o;
            
            // replace possible expressions
            try {
                s = propertyPlaceHolderHelper.replacePlaceholders((String) o, placeholderResolver);
            } catch (IllegalArgumentException e) {
                log.debug("Unable to replace property expression for property '" + name + "'. Return null.", e);
                return null;
            }
            
            return s;

        } else if (o instanceof String[]) {

            // Replace possible expressions in every String
            String[] unparsed = (String[]) o;
            String[] parsed = new String[unparsed.length];

            for (int i = 0 ; i < unparsed.length ; i++) {
                String s = unparsed[i];

                try {
                    s = propertyPlaceHolderHelper.replacePlaceholders(unparsed[i], placeholderResolver);
                } catch (IllegalArgumentException e ) {
                    log.debug("Unable to replace property expression for property '" + name + "'. Return null.", e);
                    s = null;
                }

                parsed[i] = s;
            }

            return parsed;
        }

        return o;
    }
}
