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

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.Context;

/**
 * Locale factory that matches the language of available locales against the 
 * URL base path of the current context, i.e. against the first part of the URL.
 * 
 * If no match is found, the locale is set to Locale.ENGLISH.
 * 
 * It keeps the found locale in session.   
 */
public class URLBasePathLocaleFactory implements LocaleFactory {

    private static final String SESSION_KEY = URLBasePathLocaleFactory.class.getName() + ".locale";
    
    // javadoc from interface
    public Locale getLocale(HttpServletRequest request, String contextName) {
        
        Locale locale = (Locale) request.getSession().getAttribute(SESSION_KEY + "." + contextName);
        
        if (locale == null) {
        
            // first try to read from repository base location
            Context context = (Context) request.getAttribute(contextName);
            
            // do a match against the start of URL
            if (locale == null) {
                Locale[] locales = Locale.getAvailableLocales();
                for (int i = 0; i < locales.length; i++) {
                    
                    if (context.getURLBasePath().toLowerCase().startsWith(
                            "/" + locales[i].getLanguage().toLowerCase())) {
                        // create a new one to omit the country 
                        locale = new Locale(locales[i].getLanguage());
                        break;
                    }
                }
            }
            
            // default
            if (locale == null) {
                locale = Locale.ENGLISH;
            }

            // set in session
            request.getSession().setAttribute(SESSION_KEY + "." + contextName, locale);
        }
        
        return locale;
    }
}
