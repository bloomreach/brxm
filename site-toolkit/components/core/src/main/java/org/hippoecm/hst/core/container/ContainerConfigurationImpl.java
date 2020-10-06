/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container Configuration
 * 
 * Retrieve basic data types from the container configuration.
 * This is a subset of Commons Configuration functionality.
 * 
 * @version $Id$
 */
public class ContainerConfigurationImpl implements ContainerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ContainerConfigurationImpl.class);

    protected Configuration configuration;
    private final Properties properties;

    public ContainerConfigurationImpl(Configuration configuration) {
        this.configuration = configuration;
        properties = ConfigurationConverter.getProperties(configuration);

    }

    public boolean isEmpty() {
        return (configuration == null || configuration.isEmpty());
    }

    public boolean containsKey(String key) {
        return configuration.containsKey(key);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return configuration.getBoolean(key, defaultValue);
        } catch (Exception e) {
            log.info("Return default value '{}' for '{}' because of '{}'", defaultValue, key , e.toString());
            return defaultValue;
        }
    }

    public boolean getBoolean(String key) {
        try {
            return configuration.getBoolean(key);
        } catch (NoSuchElementException e) {
            log.info("Return 'false' for '{}' because of '{}'", key , e.toString());
            return false;
        }
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return configuration.getDouble(key, defaultValue);
        } catch (Exception e) {
            log.info("Return default value '{}' for '{}' because of '{}'", defaultValue,  key , e.toString());
            return defaultValue;
        }
    }

    public double getDouble(String key) {
        try {
            return configuration.getDouble(key);
        } catch (NoSuchElementException e) {
            log.info("Return '0D' for '{}' because of '{}'", key , e.toString());
            return 0D;
        }

    }

    public float getFloat(String key, float defaultValue) {
        try {
            return configuration.getFloat(key, defaultValue);
        } catch (Exception e) {
            log.info("Return default value '{}' for '{}' because of '{}'", defaultValue, key , e.toString());
            return defaultValue;
        }
    }

    public float getFloat(String key) {
        try {
            return configuration.getFloat(key);
        } catch (NoSuchElementException e) {
            log.info("Return '0F' for '{}' because of '{}'", key , e.toString());
            return 0F;
        }
    }

    public int getInt(String key, int defaultValue) {
        try {
            return configuration.getInt(key, defaultValue);
        } catch (Exception e) {
            log.info("Return default value '{}' for '{}' because of '{}'", defaultValue, key , e.toString());
            return defaultValue;
        }
    }

    public int getInt(String key) {
        try {
            return configuration.getInt(key);
        } catch (NoSuchElementException e) {
            log.info("Return '0' for '{}' because of '{}'", key , e.toString());
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getList(String key) {
        return Arrays.asList(configuration.getStringArray(key));
    }

    public long getLong(String key, long defaultValue) {
        return configuration.getLong(key, defaultValue);
    }

    public long getLong(String key) {
        return configuration.getLong(key);
    }

    public String getString(String key, String defaultValue) {
        try {
            return configuration.getString(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getString(String key) {
        return configuration.getString(key);
    }

    public String[] getStringArray(String key) {
        return configuration.getStringArray(key);
    }

    @SuppressWarnings("unchecked")
    public Iterator<String> getKeys() {
        return configuration.getKeys();
    }

    public boolean isDevelopmentMode() {
        return getBoolean("development.mode", false);
    }

    public void setProperty(String key, Object value) {
        configuration.setProperty(key, value);
    }
    
    public Properties toProperties() {
        // ConfigurationConverter.getProperties(configuration) is fairly slow, hence instead do that once and
        // return new Properties(properties) : we do not return 'properties' since that object is modifiable
        return new Properties(properties);
    }

}
