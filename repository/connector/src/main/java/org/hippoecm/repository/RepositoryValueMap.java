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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.ValueMap;

public class RepositoryValueMap extends RepositoryMapImpl implements ValueMap {

    private RepositoryValueMap() {
        super("/");
    }

    public RepositoryValueMap(RepositoryMapImpl map) {
        super(map);
    }

    public RepositoryValueMap(Node node) throws RepositoryException {
        super(node);
    }

    public String getString(Object key, String defaultValue) {
        Object result = get(key);
        if (result == null)
            return defaultValue;
        if (result instanceof String)
            return (String)key;
        else if (result instanceof RepositoryMap)
            return (String)((RepositoryMap)result).get("_path");
        else
            return result.toString();
    }

    public int getInt(Object key, int defaultValue) {
        Object result = get(key);
        if (result == null)
            return defaultValue;
        else if (result instanceof Integer)
            return ((Integer)result).intValue();
        else
            return defaultValue;
    }

    public long getLong(Object key, long defaultValue) {
        Object result = get(key);
        if (result == null)
            return defaultValue;
        else if (result instanceof Long)
            return ((Long)result).longValue();
        else
            return defaultValue;
    }

    public float getFloat(Object key, float defaultValue) {
        Object result = get(key);
        if (result == null)
            return defaultValue;
        else if (result instanceof Float)
            return ((Float)result).floatValue();
        else
            return defaultValue;
    }

    public double getDouble(Object key, double defaultValue) {
        Object result = get(key);
        if (result == null)
            return defaultValue;
        else if (result instanceof Double)
            return ((Double)result).doubleValue();
        else
            return defaultValue;
    }

    public void setString(Object key, String defaultValue) {
        throw new UnsupportedOperationException();
    }

    public void setInt(Object key, int defaultValue) {
        throw new UnsupportedOperationException();
    }

    public void setLong(Object key, long defaultValue) {
        throw new UnsupportedOperationException();
    }
    ;

    public void setFloat(Object key, float defaultValue) {
        throw new UnsupportedOperationException();
    }

    public void setDouble(Object key, double defaultValue) {
        throw new UnsupportedOperationException();
    }

    public String get(Object key, String defaultValue) {
        return getString(key, defaultValue);
    }

    public int get(Object key, int defaultValue) {
        return getInt(key, defaultValue);
    }

    public long get(Object key, long defaultValue) {
        return getLong(key, defaultValue);
    }

    public float get(Object key, float defaultValue) {
        return getFloat(key, defaultValue);
    }

    public double get(Object key, double defaultValue) {
        return getDouble(key, defaultValue);
    }

    public ValueMap get(Object key, ValueMap defaultValue) {
        if (containsKey(key)) {
            Object result = get(key);
            if (result == null)
                return new RepositoryValueMap();
            else if (result instanceof RepositoryValueMap)
                return (RepositoryValueMap)result;
            else if (result instanceof RepositoryMapImpl)
                return new RepositoryValueMap((RepositoryMapImpl)result);
            else
                return defaultValue;
        } else
            return defaultValue;
    }

    public ValueMap getValueMap(Object key) {
        return get(key, new RepositoryValueMap());
    }
}
