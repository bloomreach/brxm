/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
     * @return boolean associated for <code>key</code> and <code>false</code> if key is missing
     * @throws java.lang.RuntimeException if key is present but mapped to wrong type
     */
    boolean getBoolean(String key);
    
    /**
     * Get a boolean associated with the given configuration key. 
     * If the key doesn't map to an existing object or to an object that is not a boolean/Boolean, the default value is returned.
     * @param key
     * @param defaultValue
     * @return boolean associated with configuration <code>key</code> and if <code>key</code> maps to object that does not
     * map to a boolean/Boolean or is missing, <code>defaultValue</code> is returned
     */
    boolean getBoolean(String key, boolean defaultValue);
    
    /**
     * Get a string associated with the given configuration key.
     * @param key
     * @return String associated for <code>key</code> and <code>null</code> if key is missing
     * @throws java.lang.RuntimeException if key is present but mapped to wrong type
     */
    String getString(String key);
    
    /**
     * Get a string associated with the given configuration key.
     * If the key doesn't map to an existing object or to an object that is not a String, the default value is returned.
     * @param key
     * @param defaultValue
     * @return String associated with configuration <code>key</code> and if <code>key</code> maps to object that does not
     * map to a string or is missing, <code>defaultValue</code> is returned
     */
    String getString(String key, String defaultValue);
    
    /**
     * Get a double associated with the given configuration key. 
     * @param key
     * @return double associated for <code>key</code> and <code>0D</code> if key is missing
     * @throws java.lang.RuntimeException if key is present but mapped to wrong type
     */
    double getDouble(String key);
    
    /**
     * Get a double associated with the given configuration key.
     * If the key doesn't map to an existing object or to an object that is not a double/Double, the default value is returned.
     * @param key
     * @param defaultValue
     * @return double associated with configuration <code>key</code> and if <code>key</code> maps to object that does not
     * map to a double/Double or is missing, <code>defaultValue</code> is returned
     */
    double getDouble(String key, double defaultValue);
    
    /**
     * Get a float associated with the given configuration key.
     * @param key
     * @return float associated for <code>key</code> and <code>0F</code> if key is missing
     * @throws java.lang.RuntimeException if key is present but mapped to wrong type
     */
    float getFloat(String key);
    
    /**
     * Get a float associated with the given configuration key.
     * If the key doesn't map to an existing object or to an object that is not a float/Float, the default value is returned.
     * @param key
     * @param defaultValue
     * @return double associated with configuration <code>key</code> and if <code>key</code> maps to object that does not
     * map to a float/Float or is missing, <code>defaultValue</code> is returned
     */
    float getFloat(String key, float defaultValue);
    
    /**
     * Get a int associated with the given configuration key.
     * @param key
     * @return int associated for <code>key</code> and <code>0</code> if key is missing
     * @throws java.lang.RuntimeException if key is present but mapped to wrong type
     */
    int getInt(String key);
    
    /**
     * Get a int associated with the given configuration key.
     * If the key doesn't map to an existing object or to an object that is not a int/Int, the default value is returned.
     * @param key
     * @param defaultValue
     * @return int associated with configuration <code>key</code> and if <code>key</code> maps to object that does not
     * map to a int/Int or is missing, <code>defaultValue</code> is returned
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
     * @return long associated for <code>key</code> and <code>0L</code> if key is missing
     * @throws java.lang.RuntimeException if key is present but mapped to wrong type
     */
    long getLong(String key);
    
    /**
     * Get a long associated with the given configuration key.
     * If the key doesn't map to an existing object or to an object that is not a long/Long, the default value is returned.
     * @param key
     * @param defaultValue
     * @return int associated with configuration <code>key</code> and if <code>key</code> maps to object that does not
     * map to a long/Long or is missing, <code>defaultValue</code> is returned
     */
    long getLong(String key, long defaultValue);
    
    /**
     * Get an array of strings associated with the given configuration key. 
     * @param key
     */
    String[] getStringArray(String key);
    
    /**
     * Get the list of the keys contained in the configuration. 
     * @return {@link Iterator} of all the {@link String} keys
     */
    Iterator<String> getKeys();
    
    /**
     * Checks if the container is running under development mode
     */
    boolean isDevelopmentMode();
    
    /**
     * <p>
     * Convert a ContainerConfiguration class into a Properties class. List properties
     * are joined into a string using the delimiter of the configuration.
     * </p>
     * <p>
     *     modifications to the returned Properties object are not reflected in the ContainerConfiguration and
     *     only to the returned Properties: Every new invocation returns a new unique instance
     * </p>
     * @return Properties created from the Configuration
     */
    Properties toProperties();
    
}
