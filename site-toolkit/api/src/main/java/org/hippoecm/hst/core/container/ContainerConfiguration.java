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
package org.hippoecm.hst.core.container;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Container Configuration
 * 
 * Retrieve basic data types from the container configuration(s).
 * This is a subset of Commons Configuration functionality.
 * 
 * @version $Id$
 */
public interface ContainerConfiguration {
    
    /**
     * Check if the configuration is empty.
     *
     * @return <code>true</code> if the configuration contains no property,
     *         <code>false</code> otherwise.
     */
    boolean isEmpty();
    
    /**
     * Check if the configuration contains the specified key.
     * @param key the key whose presence in this configuration is to be tested 
     * @return true if the configuration contains a value for this key, false otherwise
     */
    boolean containsKey(String key);
    
    /**
     * Get a boolean associated with the given configuration key.
     * @param key
     * @return
     */
    boolean getBoolean(String key);
    
    /**
     * Get a boolean associated with the given configuration key. 
     * If the key doesn't map to an existing object, the default value is returned.
     * @param key
     * @param defaultValue
     * @return
     */
    boolean getBoolean(String key, boolean defaultValue);
    
    /**
     * Get a string associated with the given configuration key.
     * @param key
     * @return
     */
    String getString(String key);
    
    /**
     * Get a string associated with the given configuration key.
     * @param key
     * @param defaultValue
     * @return
     */
    String getString(String key, String defaultValue);
    
    /**
     * Get a double associated with the given configuration key. 
     * @param key
     * @return
     */
    double getDouble(String key);
    
    /**
     * Get a Double associated with the given configuration key.
     * @param key
     * @param defaultValue
     * @return
     */
    double getDouble(String key, double defaultValue);
    
    /**
     * Get a float associated with the given configuration key.
     * @param key
     * @return
     */
    float getFloat(String key);
    
    /**
     * Get a Float associated with the given configuration key. 
     * @param key
     * @param defaultValue
     * @return
     */
    float getFloat(String key, float defaultValue);
    
    /**
     * Get a int associated with the given configuration key.
     * @param key
     * @return
     */
    int getInt(String key);
    
    /**
     * Get a int associated with the given configuration key. 
     * @param key
     * @param defaultValue
     * @return
     */
    int getInt(String key, int defaultValue);
    
    /**
     * Get a List of strings associated with the given configuration key. 
     * @param key
     * @return {@link List} of {@link String}s for <code>key</code>
     */
    List<String> getList(String key);
    
    /**
     * Get a long associated with the given configuration key. 
     * @param key
     * @return
     */
    long getLong(String key);
    
    /**
     * Get a long associated with the given configuration key.
     * @param key
     * @param defaultValue
     * @return
     */
    long getLong(String key, long defaultValue);
    
    /**
     * Get an array of strings associated with the given configuration key. 
     * @param key
     * @return
     */
    String[] getStringArray(String key);
    
    /**
     * Get the list of the keys contained in the configuration. 
     * @return {@link Iterator} of all the {@link String} keys
     */
    Iterator getKeys();
    
    /**
     * Checks if the container is running under development mode
     * @return
     */
    boolean isDevelopmentMode();
    
    /**
     * Convert a ContainerConfiguration class into a Properties class. List properties
     * are joined into a string using the delimiter of the configuration.
     * 
     * @return Properties created from the Configuration
     */
    Properties toProperties();
    
}
