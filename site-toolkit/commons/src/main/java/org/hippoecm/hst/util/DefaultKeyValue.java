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
package org.hippoecm.hst.util;

import java.util.Map;

/**
 * An implementation of {@link KeyValue} to provide a simple key value pair.
 * A Map Entry has considerable additional semantics over and above a simple key-value pair.
 * 
 * @version $Id$
 */
public class DefaultKeyValue<K, V> implements KeyValue<K, V>
{
    
    private K key;
    private V value;
    private boolean compareByKeyOnly;

    /**
     * Constructs a new pair with the specified key and given value.
     * 
     * @param key
     *            the key for the entry, may be null
     * @param value
     *            the value for the entry, may be null
     */
    public DefaultKeyValue(final K key, final V value)
    {
        this(key, value, false);
    }
    
    /**
     * Constructs a new pair with the specified key and given value.
     * 
     * @param key
     *            the key for the entry, may be null
     * @param value
     *            the value for the entry, may be null
     * @param compareByKeyOnly
     *            flag if equals() depends on key only 
     */
    public DefaultKeyValue(final K key, final V value, final boolean compareByKeyOnly)
    {
        this.key = key;
        this.value = value;
        this.compareByKeyOnly = compareByKeyOnly;
    }

    /**
     * Constructs a new pair from the specified <code>KeyValue</code>.
     * 
     * @param pair
     *            the pair to copy, must not be null
     * @throws NullPointerException
     *             if the entry is null
     */
    public DefaultKeyValue(final KeyValue<K, V> pair)
    {
        this(pair, false);
    }
    
    /**
     * Constructs a new pair from the specified <code>KeyValue</code>.
     * 
     * @param pair
     *            the pair to copy, must not be null
     * @param compareByKeyOnly
     *            flag if equals() depends on key only 
     * @throws NullPointerException
     *             if the entry is null
     */
    public DefaultKeyValue(final KeyValue<K, V> pair, final boolean compareByKeyOnly)
    {
        this(pair.getKey(), pair.getValue(), compareByKeyOnly);
    }

    /**
     * Constructs a new pair from the specified <code>Map.Entry</code>.
     * 
     * @param entry
     *            the entry to copy, must not be null
     * @throws NullPointerException
     *             if the entry is null
     */
    public DefaultKeyValue(final Map.Entry<K, V> entry)
    {
        this(entry, false);
    }
    
    /**
     * Constructs a new pair from the specified <code>Map.Entry</code>.
     * 
     * @param entry
     *            the entry to copy, must not be null
     * @param compareByKeyOnly
     *            flag if equals() depends on key only 
     * @throws NullPointerException
     *             if the entry is null
     */
    public DefaultKeyValue(final Map.Entry<K, V> entry, final boolean compareByKeyOnly)
    {
        this(entry.getKey(), entry.getValue(), compareByKeyOnly);
    }

    /**
     * Gets the key from the pair.
     * 
     * @return the key
     */
    public K getKey()
    {
        return key;
    }

    /**
     * Gets the value from the pair.
     * 
     * @return the value
     */
    public V getValue()
    {
        return value;
    }

    // -----------------------------------------------------------------------
    /**
     * Sets the key.
     * 
     * @param key
     *            the new key
     * @return the old key
     * @throws IllegalArgumentException
     *             if key is this object
     */
    public K setKey(final K key)
    {
        if (key == this)
        {
            throw new IllegalArgumentException("DefaultKeyValue may not contain itself as a key.");
        }
        
        final K old = this.key;
        this.key = key;
        
        return old;
    }

    /**
     * Sets the value.
     * 
     * @return the old value of the value
     * @param value
     *            the new value
     * @throws IllegalArgumentException
     *             if value is this object
     */
    public V setValue(final V value)
    {
        if (value == this)
        {
            throw new IllegalArgumentException("DefaultKeyValue may not contain itself as a value.");
        }
        
        final V old = this.value;
        this.value = value;
        
        return old;
    }

    // -----------------------------------------------------------------------
    /**
     * Compares this <code>KeyValue</code> with another <code>KeyValue</code>.
     * <p> Returns true if the compared object is also a <code>DefaultKeyValue</code>,
     * and its key and value are equal to this object's key and value.
     * 
     * @param obj
     *            the object to compare to
     * @return true if equal key and value
     */
    @SuppressWarnings("unchecked")
    public boolean equals(final Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        
        if (obj instanceof DefaultKeyValue == false)
        {
            return false;
        }
        
        DefaultKeyValue<K,V> other = (DefaultKeyValue<K,V>) obj;
        
        if (this.compareByKeyOnly)
        {
            return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey()));
        }
        else
        {
            return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey())) &&
                   (getValue() == null ? other.getValue() == null : getValue().equals(other.getValue()));
        }
    }

    /**
     * Gets a hashCode compatible with the equals method.
     * 
     * @return a suitable hash code
     */
    public int hashCode()
    {
        return (getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
    }
    
}
