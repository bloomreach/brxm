/*
 *  Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
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

    private MessageUtils() {
    }

    /**
     * Replaces the given text by looking up values from the resolved resource bundle by the <code>basename</code>.
     * @param basename
     * @param text
     * @return
     */
    public static String replaceMessages(String basename, String text) {
        HstRequestContext requestContext = RequestContextProvider.get();
        ResourceBundle bundle = null;

        try {
            bundle = ResourceBundleUtils.getBundle(requestContext.getServletRequest(), basename, null);
        } catch (MissingResourceException e) {
            log.warn("Cannot find a resource bundle by the basename, '{}'.", basename);
        }

        if (bundle == null) {
            return text;
        }

        return replaceMessagesByBundle(bundle, text);
    }

    /**
     * Replaces the given text by looking up values from the given resource bundle.
     * @param bundle
     * @param text
     * @return
     */
    public static String replaceMessagesByBundle(ResourceBundle bundle, String text) {
        if (bundle == null) {
            throw new IllegalArgumentException("The bundle must not be null.");
        }

        StrSubstitutor subst = new StrSubstitutor(new ResourceBundleVariableResolver(bundle));

        return subst.replace(text);
    }

    /**
     * Variable Resolver implementation which looks up values from the given resource bundle.
     * If not found, it returns the variable name wrapped by '???'.
     */
    private static class ResourceBundleVariableResolver extends StrLookup {

        private ResourceBundle bundle;

        ResourceBundleVariableResolver(ResourceBundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public String lookup(String key) {
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                return "???" + key + "???";
            }
        }
    }
}
