/*
 * Copyright 2021 Bloomreach (https://www.bloomreach.com/)
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
package org.hippoecm.frontend.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.site.HstServices;

/**
 * Variable interpolation utility, replacing a text with variable references by resolved values.
 * <P>
 * This utility class provides features to replace texts containing variables
 * by looking up values from the platform <code>ContainerConfiguration</code>.
 * </P>
 */
public class InterpolationUtils {

    /**
     * Default escape character to override the default escape character of {@link StringSubstitutor}, '$'.
     * '\\' is more user friendly than '$' in most use cases.
     */
    public static final char DEFAULT_ESCAPE = '\\';

    private InterpolationUtils() {
    }

    /**
     * Replaces the given text by looking up values from the platform configuration.
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @param escapeChar escape character which can be put just before a variable reference to ignore the expression.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String interpolate(final String text) {
        return interpolate(text, null, null, null);
    }

    /**
     * Replaces the given text by looking up values from the platform configuration.
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @param escapeChar escape character which can be put just before a variable reference to ignore the expression.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String interpolate(final String text, final String variablePrefix, final String variableSuffix,
            final Character escapeChar) {
        final ContainerConfiguration containerConfiguration = HstServices.isAvailable()
                ? HstServices.getComponentManager().getContainerConfiguration()
                : null;
        return interpolate(containerConfiguration, text, variablePrefix, variableSuffix, escapeChar);
    }

    /**
     * Replaces the given text by looking up values from the given container configuration.
     * @param containerConfiguration platform configuration
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @param escapeChar escape character which can be put just before a variable reference to ignore the expression.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String interpolate(final ContainerConfiguration containerConfiguration, final String text,
            final String variablePrefix, final String variableSuffix, final Character escapeChar) {
        return interpolate(new ContainerConfigurationStringLookup(containerConfiguration), text, variablePrefix,
                variableSuffix, escapeChar);
    }

    /**
     * Replaces the given text by looking up values from the given <code>StringLookup</code>.
     * @param strLookup string lookup
     * @param text text to replace
     * @param variablePrefix variable reference prefix. "${" by default.
     * @param variableSuffix variable reference suffix. "}" by default.
     * @param escapeChar escape character which can be put just before a variable reference to ignore the expression.
     * @return replaced string by the values found in the given resource bundle
     */
    public static String interpolate(final StringLookup strLookup, final String text, final String variablePrefix,
            final String variableSuffix, final Character escapeChar) {
        final StringSubstitutor subst = new StringSubstitutor(strLookup, StringSubstitutor.DEFAULT_PREFIX,
                StringSubstitutor.DEFAULT_SUFFIX, DEFAULT_ESCAPE);

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
     * Variable Resolver implementation which looks up values from the <code>ContainerConfiguration</code>.
     */
    private static class ContainerConfigurationStringLookup implements StringLookup {

        private final ContainerConfiguration containerConfiguration;

        ContainerConfigurationStringLookup(final ContainerConfiguration containerConfiguration) {
            this.containerConfiguration = containerConfiguration;
        }

        @Override
        public String lookup(String key) {
            return containerConfiguration != null && containerConfiguration.containsKey(key)
                    ? containerConfiguration.getString(key)
                    : null;
        }
    }
}
