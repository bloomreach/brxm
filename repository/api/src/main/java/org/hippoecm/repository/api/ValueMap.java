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
package org.hippoecm.repository.api;

import java.util.Map;

/**
 * <b>This class is not (yet) part of the API, but under evaluation.</b><p/>
 * A {@see java.util.Map} enhanced with a number of methods to obtain more type-safe information from the Map.
 */
public interface ValueMap extends Map {

    /**
     * Returns the string representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to a string.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the string representation of the indicated map entry
     */
    public String getString(Object key, String defaultValue);

    /**
     * Returns the integer representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to a integer.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the integer representation of the indicated map entry
     */
    public int getInt(Object key, int defaultValue);

    /**
     * Returns the long integer representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to an long integer.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the integer representation of the indicated map entry
     */
    public long getLong(Object key, long defaultValue);

    /**
     * Returns the floating point number representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to a floating point number.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the integer representation of the indicated map entry
     */
    public float getFloat(Object key, float defaultValue);

    /**
     * Returns the double precision floating point number representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to a double precision floating point number.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the integer representation of the indicated map entry
     */
    public double getDouble(Object key, double defaultValue);

    /**
     * Sets or adds an entry in the Map, indicated by the key to the indicated value.
     * @param key the entry for which to set the value
     * @param value the (new) value the entry should get, the implementation may reject null values
     */
    public void setString(Object key, String value);

    /**
     * Sets or adds an entry in the Map, indicated by the key to the indicated value.
     * @param key the entry for which to set the value
     * @param value the (new) value the entry should get
     */
    public void setInt(Object key, int value);

    /**
     * Sets or adds an entry in the Map, indicated by the key to the indicated value.
     * @param key the entry for which to set the value
     * @param value the (new) value the entry should get
     */
    public void setLong(Object key, long value);

    /**
     * Sets or adds an entry in the Map, indicated by the key to the indicated value.
     * @param key the entry for which to set the value
     * @param value the (new) value the entry should get
     */
    public void setFloat(Object key, float value);

    /**
     * Sets or adds an entry in the Map, indicated by the key to the indicated value.
     * @param key the entry for which to set the value
     * @param value the (new) value the entry should get
     */
    public void setDouble(Object key, double value);

    /**
     * Returns the string representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to a string.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the string representation of the indicated map entry
     */
    public String get(Object key, String defaultValue);

    /**
     * Returns the integer representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to a integer.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the integer representation of the indicated map entry
     */
    public int get(Object key, int defaultValue);

    /**
     * Returns the long integer representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to an long integer.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the integer representation of the indicated map entry
     */
    public long get(Object key, long defaultValue);

    /**
     * Returns the floating point number representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to a floating point number.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the integer representation of the indicated map entry
     */
    public float get(Object key, float defaultValue);

    /**
     * Returns the double precision floating point number representation of the entry in the map, or the defaultValue parameter if the key is not present or cannot be converted to a double precision floating point number.
     * @param key the key with which to search the Map.  As the ValueMap can be backed by a hierarchical storage, this key may contain a path or pseudo path elements.
     * @param defaultValue the value to return if the key cannot be located in the map, or cannot be converted to the right return type
     * @return the integer representation of the indicated map entry
     */
    public double get(Object key, double defaultValue);

    /**
     * Returns a new Map representation of a part of the map based on the selection given as the key value.  When such a sub-selection
     * is not available in the map, the indicated default value should be returned.
     * Since the implementation may be backed by a hierarchical storage, this method can be considered obtaining a subtree from the
     * original tree, if supported by the backing storage.
     * Note that the subselection returned is not a new copy of the map.  Changing values in the returned map will also change the
     * values in the original map from which the subsection was obtained.
     * @param key a key into the indicated selection which should be returned
     * @param defaultValue the map to return if the indicated selection cannot be returned
     * @return the subselection as a part of the original map
     */
    public ValueMap get(Object key, ValueMap defaultValue);

    /**
     * Returns a new Map representation of a part of the map based on the selection given as the key value.  When such a sub-selection
     * is not available in the map, an empty ValueMap will be returned,
     * Since the implementation may be backed by a hierarchical storage, this method can be considered obtaining a subtree from the
     * original tree, if supported by the backing storage.
     * Note that the subselection returned is not a new copy of the map.  Changing values in the returned map will also change the
     * values in the original map from which the subsection was obtained.
     * @param key a key into the indicated selection which should be returned
     * @return the subselection as a part of the original map, or an empty map but never null
     */
    public ValueMap getValueMap(Object key);
}
