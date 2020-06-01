/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.utils;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageUtils
 * <P>
 * This utility class provides features to replace texts containing variables
 * by looking up values from the resolved or given resource bundle.
 * </P>
 * <P>
 * If the variable is not found, then it returns the variable name wrapped by '???'.
 * </P>
 */
public class MessageUtils {

    private static Logger log = LoggerFactory.getLogger(MessageUtils.class);

    /**
     * Default escape character to override the default escape character of {@link StrSubstitutor}, '$'.
     * '\\' is more user friendly than '$' in most use cases.
     */
    public static final char DEFAULT_ESCAPE = '\\';

    private MessageUtils() {
    }

    /**
     * Replaces the given text by looking up values from the resolved resource bundle by the <code>basename</code>.
     * @param basename resource bundle base name
     * @param text text to replace
     * @return replaced string by the values found in the given resource bundle
     */
    public static String replaceMessages(String basename, String text) {
        return replaceMessages(basename, text, null, null, null, false);
    }

    /**
     * Replaces the given text by looking up values from the resolved resource bundle by the <code>basename</code>.
     * @param basename resource bundle base name
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String replaceMessages(String basename, String text, String variablePrefix,
                                         String variableSuffix) {
        return replaceMessages(basename, text, variablePrefix, variableSuffix, null, false);
    }

    /**
     * Replaces the given text by looking up values from the resolved resource bundle by the <code>basename</code>.
     * @param basename resource bundle base name
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @param escapeChar escape character which can be put just before a variable reference to ignore the expression.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String replaceMessages(String basename, String text, String variablePrefix,
                                         String variableSuffix, Character escapeChar) {
        return replaceMessages(basename, text, variablePrefix, variableSuffix, escapeChar, false);
    }

    /**
     * Replaces the given text by looking up values from the resolved resource bundle by the <code>basename</code>.
     * @param basename resource bundle base name
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @param escapeChar escape character which can be put just before a variable reference to ignore the expression.
     * @param escapeMessageXml whether or not to escape a message value having &amp;,&gt;,&lt;,", and '.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String replaceMessages(String basename, String text, String variablePrefix,
                                         String variableSuffix, Character escapeChar, boolean escapeMessageXml) {
        ResourceBundle bundle = null;

        try {
            bundle = ResourceBundleUtils.getBundle(basename, null);
        } catch (MissingResourceException e) {
            log.warn("Cannot find a resource bundle by the basename, '{}'.", basename);
        }

        if (bundle == null) {
            return text;
        }

        return replaceMessagesByBundle(bundle, text, variablePrefix, variableSuffix, escapeChar, escapeMessageXml);
    }

    /**
     * Replaces the given text by looking up values from the given resource bundle.
     * @param bundle resource bundle
     * @param text text to replace
     * @return replaced string by the values found in the given resource bundle
     */
    public static String replaceMessagesByBundle(ResourceBundle bundle, String text) {
        return replaceMessagesByBundle(bundle, text, null, null, null, false);
    }

    /**
     * Replaces the given text by looking up values from the given resource bundle.
     * @param bundle resource bundle
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String replaceMessagesByBundle(ResourceBundle bundle, String text, String variablePrefix,
                                                 String variableSuffix) {
        return replaceMessagesByBundle(bundle, text, variablePrefix, variableSuffix, null, false);
    }

    /**
     * Replaces the given text by looking up values from the given resource bundle.
     * @param bundle resource bundle
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @param escapeChar escape character which can be put just before a variable reference to ignore the expression.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String replaceMessagesByBundle(ResourceBundle bundle, String text, String variablePrefix,
                                                 String variableSuffix, Character escapeChar) {
        return replaceMessagesByBundle(bundle, text, variablePrefix, variableSuffix, escapeChar, false);
    }

    /**
     * Replaces the given text by looking up values from the given resource bundle.
     * @param bundle resource bundle
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @param escapeChar escape character which can be put just before a variable reference to ignore the expression.
     * @param escapeMessageXml whether or not to escape a message value having &amp;,&gt;,&lt;,", and '.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String replaceMessagesByBundle(ResourceBundle bundle, String text, String variablePrefix,
                                                 String variableSuffix, Character escapeChar, boolean escapeMessageXml) {
        if (bundle == null) {
            throw new IllegalArgumentException("The bundle must not be null.");
        }

        StrSubstitutor subst = new StrSubstitutor(new ResourceBundleVariableResolver(bundle, escapeMessageXml),
                                                  StrSubstitutor.DEFAULT_PREFIX,
                                                  StrSubstitutor.DEFAULT_SUFFIX,
                                                  DEFAULT_ESCAPE);

        if (StringUtils.isNotBlank(variablePrefix)) {
            subst.setVariablePrefix(variablePrefix);
        }

        if (StringUtils.isNotBlank(variableSuffix)) {
            subst.setVariableSuffix(variableSuffix);
        }

        if (escapeChar != null) {
            subst.setEscapeChar(escapeChar);
        }

        return subst.replace(text);
    }

    /**
     * Variable Resolver implementation which looks up values from the given resource bundle.
     * If not found, it returns the variable name wrapped by '???'.
     */
    private static class ResourceBundleVariableResolver extends StrLookup {

        private final ResourceBundle bundle;
        private final boolean escapeMessageXml;

        ResourceBundleVariableResolver(final ResourceBundle bundle, final boolean escapeMessageXml) {
            this.bundle = bundle;
            this.escapeMessageXml = escapeMessageXml;
        }

        @Override
        public String lookup(String key) {
            try {
                String value = bundle.getString(key);

                if (escapeMessageXml) {
                    value = HstRequestUtils.escapeXml(value);
                }

                return value;
            } catch (MissingResourceException e) {
                return "???" + key + "???";
            }
        }
    }
}

