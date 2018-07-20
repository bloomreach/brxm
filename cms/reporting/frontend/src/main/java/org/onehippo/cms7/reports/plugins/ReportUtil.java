/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.reports.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.frontend.session.UserSession;

/**
 * Utility methods used by reports
 */
public final class ReportUtil {

    private static final String VAR_USER = "$user";

    private ReportUtil() {
        // prevent instantiation
    }

    public static String getTranslation(final String reportName, final String key, final String defaultValue) {
        return new ResourceBundleModel("hippo:reports."+ reportName, key, defaultValue).getObject();
    }

    /**
     * Translates a key of a Wicket component using the current locale.
     *
     * @see StringResourceModel
     * @param component the Wicket component
     * @param key the key to translate
     * @param defaultValue the value to return when no translation can be found
     *
     * @return the translation of a key, or the default value if no translation can be found.
     */
    public static String getTranslation(Component component, String key, String defaultValue) {
        if (StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        return new StringResourceModel(key, component)
                .setDefaultValue(defaultValue)
                .getObject();
    }

    /**
     * Substitutes all occurrences of the following variables in a string:
     * <ul>
     * <li><code>$user</code> is replaced with the name of the user currently logged in
     * </ul>
     *
     * @param s the string to substitute variable in
     *
     * @return the string with substituted variables
     */
    public static String substituteVariables(String s) {
        if (StringUtils.contains(s, VAR_USER)) {
            final String currentUserId = UserSession.get().getJcrSession().getUserID();
            return StringUtils.replace(s, VAR_USER, currentUserId);
        }
        return s;
    }

}
