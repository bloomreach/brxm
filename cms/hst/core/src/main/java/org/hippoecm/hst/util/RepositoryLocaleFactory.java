/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.util;

import java.util.Locale;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.repository.api.HippoNodeType;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Locale factory that tries to read the locale from the repository base
 * location that the current context points to.
 *
 * If no locale is found, the locale is set to Locale.ENGLISH.
 *
 * It keeps the found locale in session.
 */
public class RepositoryLocaleFactory implements LocaleFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryLocaleFactory.class);

    private static final String HIPPOSTD_LANGUAGE = "hippostd:language";

    // javadoc from interface
    public Locale getLocale(HttpServletRequest request, String contextName) {

        // context will be there by argument
        Context context = (Context) request.getAttribute(contextName);

        // locales may be different per repository base location
        String sessionKey = this.getClass().getName() + "." + context.getBaseLocation();

        Locale locale = (Locale) request.getSession().getAttribute(sessionKey);
        if (locale == null) {

            // try to read from repository base location
            Session jcrSession = JCRConnector.getJCRSession(request.getSession());
            try {
                Item item = JCRConnector.getItem(jcrSession, context.getBaseLocation());

                if (item.isNode()) {

                    Node node = (Node) item;
                    if (node.isNodeType(HippoNodeType.NT_FACETSELECT)) {

                        Value[] facets = node.getProperty(HippoNodeType.HIPPO_FACETS).getValues();

                        int localeIndex = -1;
                        for (int i = 0; i < facets.length; i++) {

                            if (facets[i].getString().equals(HIPPOSTD_LANGUAGE)) {
                                localeIndex = i;
                                break;
                            }
                        }

                        Value[] facetValues = node.getProperty(HippoNodeType.HIPPO_VALUES).getValues();
                        if (localeIndex == -1) {
                            LOGGER.warn("No value " + HIPPOSTD_LANGUAGE + " found in property " +
                                    HippoNodeType.HIPPO_FACETS + " of facetselect node " + node.getPath());
                        }

                        else if (localeIndex >= facetValues.length) {
                            LOGGER.warn("Value index " + localeIndex +
                                    " of property " + HIPPOSTD_LANGUAGE + " is too high for property "
                                    + HippoNodeType.HIPPO_VALUES + " with length " + facetValues.length);
                        }

                        // OK
                        else {

                            String localeStr = facetValues[localeIndex].getString();
                            locale = parseLocaleString(localeStr);
                        }
                    }

                    // no facetselect
                    else {

                        // direct property
                        String localeStr = node.getProperty(HIPPOSTD_LANGUAGE).getString();
                        locale = parseLocaleString(localeStr);
                    }
                }
            }
            catch (RepositoryException re) {
                throw new IllegalStateException(re);
            }

             // default
            if (locale == null) {
                locale = Locale.ENGLISH;
            }

            // set in session
            request.getSession().setAttribute(sessionKey, locale);
        }

        return locale;
    }

    private Locale parseLocaleString(String localeStr) {

        if ((localeStr != null) && (!localeStr.equals(""))) {

            String[] parts = localeStr.split("_");

            // create with language, possible country, possible variant
            if (parts.length == 1) {
                return new Locale(parts[0]);
            }
            else if (parts.length == 2) {
                return new Locale(parts[0], parts[1]);
            }
            else if (parts.length == 3) {
                return new Locale(parts[0], parts[1], parts[2]);
            }
        }

        return null;
    }
}
