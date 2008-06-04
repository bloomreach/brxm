/*
 * Copyright 2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.util;

import java.util.Locale;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.jcr.JCRConnector;

/**
 * The default locale factory first tries to read the locale from the repository 
 * base location that the given context points to. If no property is read, a 
 * simple match is done of available locales' language against the start of the 
 * URL.  
 */
public class DefaultLocaleFactory implements LocaleFactory {

    private static final String SESSION_KEY = "DefaultLocaleFactory.Locale";
    
    // javadoc from interface
    public Locale getLocale(HttpServletRequest request, String contextName) {
        
        Locale locale = (Locale) request.getSession().getAttribute(SESSION_KEY + "." + contextName);
        
        if (locale == null) {
        
            // first try to read from repository base location
            Context context = (Context) request.getAttribute(contextName);
            Session jcrSession = JCRConnector.getJCRSession(request.getSession());
            try {
                Item item = JCRConnector.getItem(jcrSession, context.getBaseLocation());
                
                if (item.isNode()) {
                    Node node = (Node) item;
                    if (node.hasProperty(HSTNodeTypes.HST_LOCALE)) {
                        String localeStr = node.getProperty(HSTNodeTypes.HST_LOCALE).getString();
                        String[] parts = localeStr.split("_");
                        
                        // create with language, possible country, possible variant 
                        if (parts.length == 1) {
                            locale = new Locale(parts[0]);
                        }
                        else if (parts.length == 2) {
                            locale = new Locale(parts[0], parts[1]);
                        }
                        else if (parts.length == 3) {
                            locale = new Locale(parts[0], parts[1], parts[2]);
                        }
                    }
                }
            } 
            catch (RepositoryException re) {
                throw new IllegalStateException(re);
            }
            
            // secondly do a simple match to the start of URL
            if (locale == null) {
                Locale[] locales = Locale.getAvailableLocales();
                for (int i = 0; i < locales.length; i++) {
                    
                    if (context.getURLBasePath().startsWith("/" + locales[i].getLanguage())) {
                        // create a new one to omit the country 
                        locale = new Locale(locales[i].getLanguage());
                    }
                }
            }
            
            // third is default
            if (locale == null) {
                locale = Locale.ENGLISH;
            }

            // set in session
            request.getSession().setAttribute(SESSION_KEY + "." + contextName, locale);
        }
        
        return locale;
    }
}
