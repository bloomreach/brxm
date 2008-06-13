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
package org.hippoecm.hst.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class HSTConfiguration implements ServletContextListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static final String PARAM_CONFIGURATION_FILES = "configurationFiles";

    public static final String KEY_REPOSITORY_ADRESS = "repository.address";
    public static final String KEY_REPOSITORY_USERNAME = "repository.user.name";
    public static final String KEY_REPOSITORY_PASSWORD = "repository.password";
    public static final String KEY_REPOSITORY_URLMAPPING_LOCATION = "repository.urlmapping.location";

    private final List<ResourceBundle> resourceBundles = new ArrayList<ResourceBundle>();
    private final Map<String, String> cache = new HashMap<String, String>();

    /**
     * Default constructor.
     */
    public HSTConfiguration() {
    }

    // javadoc from interface
    public void contextDestroyed(ServletContextEvent arg0) {
        resourceBundles.clear();
        cache.clear();
    }

    // javadoc from interface
    public void contextInitialized(ServletContextEvent event) {

        String configFiles = event.getServletContext().getInitParameter(PARAM_CONFIGURATION_FILES);
        if (configFiles == null) {
            throw new IllegalStateException("Missing init-parameter " + PARAM_CONFIGURATION_FILES);
        }

        this.setConfigurationFiles(event.getServletContext(), configFiles.split(","));

        // store in servlet context
        event.getServletContext().setAttribute(HSTConfiguration.class.getName(), this);
    }

    /**
     * Get a HST configuration item by key, with an indication if it is required or not.
     */
    public static String get(final ServletContext servletContext, final String key, final boolean required) {

        HSTConfiguration instance = (HSTConfiguration) servletContext.getAttribute(HSTConfiguration.class.getName());

        if (instance == null) {
            throw new IllegalStateException("No HSTConfiguration instance found");
        }

        return instance.get(key, required);
    }

    /**
     * Get a required HST configuration item by key.
     *
     * @throws IllegalStateException if an item cannot be found
     */
    public static String get(final ServletContext servletContext, final String key) {
        return get(servletContext, key, true/*required*/);
    }

    private void setConfigurationFiles(final ServletContext servletContext,
                                        final String[] configurationFiles) {

        // load files if there
        for (int i = 0; i < configurationFiles.length; i++) {

            InputStream stream = servletContext.getResourceAsStream(configurationFiles[i].trim());
            try {
                resourceBundles.add(new PropertyResourceBundle(stream));
            } catch (IOException io) {
                throw new IllegalArgumentException("Configuration file " + configurationFiles[i].trim()
                            + " cannot be loaded");
            }
        }
    }

    private String get(final String key, final boolean required) {

        String value = cache.get(key);

        if (value == null) {
            Iterator<ResourceBundle> iterator = resourceBundles.iterator();
            while (iterator.hasNext() && (value == null)) {
                ResourceBundle bundle = (ResourceBundle) iterator.next();

                try {
                    value = bundle.getString(key);
                }
                catch (MissingResourceException ignore) {
                    // next bundle
                }
            }

            // not found
            if (required) {
                if ((value == null) || value.equals("")) {
                    throw new MissingResourceException("Can't find required configuration value by key " + key,
                                    this.getClass().getName(), key);
                }
            }

            cache.put(key, value);
        }

        return value;
    }
}
