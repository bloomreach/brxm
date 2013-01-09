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

package org.hippoecm.frontend.plugins.xinha.json;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin.BaseConfiguration;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin.PluginConfiguration;

public class JsonParser {
    
    private static final String SINGLE_QUOTE = "'";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d*");
    
    public static String parsePluginConfiguration(Set<PluginConfiguration> pluginConfigurations) {
        StringBuilder sb = new StringBuilder(120);
        for (PluginConfiguration pc : pluginConfigurations) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("{ ");
            sb.append("name: ").append(serialize2JS(pc.getName())).append(", ");
            sb.append("values: ").append(asKeyValueArray(pc.getProperties()));
            sb.append(" }");
        }
        sb.insert(0, '[').append(']');
        return sb.toString();
    }

    /**
     * Transforms the List<String> into a Javascript object literal. The values of the input list will be
     * used as keys and their translated values will be used as values. If reversed is set the values will be
     * used as keys and vice-versa (this to support Xinha's formatblock)
     * If addLabel is set, a value identified by translationPrefix + 'identifier' will be inserted as the first element
     * in the object literal. This value will have an empty key so Xinha will know it's the label.
     * If translationPrefix is not null it will be concatenated with the key to retrieve a translation value.   
     * 
     * Example: {'h1': 'Heading 1', 'pre': 'Formatted'}
     * Reversed example: {'Heading 1' : 'h1', 'Formatted' : 'pre'}
     * Example with label: {'': 'Label value', 'pre': 'Formatted', 'h1': 'Heading 1'}
     * 
     * @param keys  List of keys to be translated
     * @param reversed Set this to use the keys as values and values as keys 
     * @return javascript object literal with translated keys
     */
    public static String asDictionary(List<String> keys, boolean addLabel, boolean reversed, String translationPrefix, Component component) {
        if (translationPrefix == null) {
            translationPrefix = "";
        }

        final StringBuilder result = new StringBuilder();
        result.append('{');
        boolean appended = false;

        if (addLabel) {
            final String value = new StringResourceModel(translationPrefix + "identifier", component, null).getString();

            if (reversed) {
                append(result, value, StringUtils.EMPTY);
            } else {
                append(result, StringUtils.EMPTY, value);
            }
            appended = true;
        }
        for (final String key : keys) {
            if (appended) {
                result.append(", ");
            }
            final String value = new StringResourceModel(translationPrefix + key, component, null).getString();
            if (reversed) {
                append(result, value, key);
            } else {
                append(result, key, value);
            }
            appended = true;
        }

        result.append('}');

        return result.toString();
    }

    private static void append(StringBuilder s, String key, String value) {
        s.append("'");
        s.append(key);
        s.append("' : '");
        s.append(value);
        s.append("'");
    }

    /**
     * Transforms the Map<String, String> into a Javascript array with object literals that contain a key/value pair
     * 
     * Example: [{key: 'id', value: 'myId'}, {key: 'index', value: 1}]  
     */
    public static String asKeyValueArray(Map<String, String> properties) {
        StringBuilder ret = new StringBuilder();
        ret.append('[');
        boolean appended = false;

        for (String key : properties.keySet()) {
            if (appended) {
                ret.append(", ");
            }
            ret.append("{ key : '");
            ret.append(key);
            ret.append("', value : ");
            ret.append(serialize2JS(properties.get(key)));
            ret.append("}");
            appended = true;
        }

        ret.append(']');
        return ret.toString();
    }

    /**
     * Transforms the List<String> into a Javascript array
     * 
     * Example: ['foo', 'bar', true, 1]
     */
    public static String asArray(List<String> list) {
        StringBuilder val = new StringBuilder("  [\n");
        for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
            val.append("    ");
            val.append(serialize2JS(iter.next()));
            if (iter.hasNext()) {
                val.append(',');
            }
            val.append('\n');
        }
        val.append("  ]");
        return val.toString();
    }

    /**
     * Transforms the Set of BaseConfigurations into a Javascript array containing the configuration names
     * 
     * Example: ['config1', 'config2']
     */
    public static String asArray(Set<? extends BaseConfiguration> configs) {
        StringBuilder val = new StringBuilder("  [\n");
        for (Iterator<? extends BaseConfiguration> iter = configs.iterator(); iter.hasNext();) {
            val.append("    ");
            val.append(serialize2JS(iter.next().getName()));
            if (iter.hasNext()) {
                val.append(',');
            }
            val.append('\n');
        }
        val.append("  ]");
        return val.toString();
    }

    /**
     * Serializes a String value into a Javascript value. True/false will be serialized as Javascript booleans,
     * numbers will be serialized as Javascript numbers and String will be escaped by two single quotes. 
     */
    public static String serialize2JS(String value) {
        if (value == null) {
            return "null";
        } else if (value.equalsIgnoreCase("true")) {
            return "true";
        } else if (value.equalsIgnoreCase("false")) {
            return "false";
        } else if (NUMBER_PATTERN.matcher(value).matches()) {
            return value;
        }
        return SINGLE_QUOTE + value.replaceAll(SINGLE_QUOTE, "\\\\" + SINGLE_QUOTE) + SINGLE_QUOTE;
    }    
    
}
