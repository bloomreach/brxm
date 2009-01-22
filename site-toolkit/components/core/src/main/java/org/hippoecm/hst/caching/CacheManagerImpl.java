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
package org.hippoecm.hst.caching;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheManagerImpl {

    private static final Logger log = LoggerFactory.getLogger(CacheManagerImpl.class);
    private static final Map<String, Cache> caches = new HashMap<String, Cache>();
    private static boolean newCacheIsEnabled = true;

    public static Map<String, Cache> getCaches() {
        return caches;
    }

    public static boolean isNewCacheIsEnabled() {
        return newCacheIsEnabled;
    }

    public static void setNewCacheIsEnabled(boolean newCacheIsEnabled) {
        CacheManagerImpl.newCacheIsEnabled = newCacheIsEnabled;
    }

    public static Cache getCache(String cacheName) {
        return getCache(cacheName, null);
    }

    public static Cache getCache(String cacheName, String clazz) {
        if (cacheName == null) {
            cacheName = "anonymous";
        }

        if (clazz == null) {
            clazz = LRUMemoryCacheImpl.class.getName();
        }

        cacheName = clazz + "_" + cacheName;

        synchronized (caches) {
            Cache cache = caches.get(cacheName);
            if (cache != null) {
                return cache;
            } else {
                cache = createCache(1000, clazz);
                if (cache == null) {
                    log.warn("Cache instantiation failed for '" + clazz + "'");
                    return null;
                }
                cache.setActive(newCacheIsEnabled);
                caches.put(cacheName, cache);
                return cache;
            }
        }

    }

    private static Cache createCache(int size, String clazz) {
        Object o = null;
        try {
            Class c = Class.forName(clazz);
            Constructor con = c.getConstructor(new Class[] { int.class });
            o = con.newInstance(new Object[] { size });
            if (!Cache.class.isInstance(o)) {
                log.error(clazz + " does not implement the interface " + Cache.class.getName() + ". Return null");
                return null;
            }
            return (Cache) o;
        } catch (InstantiationException e) {
            log.error("InstantiationException : Cannot instantiate cache for '" + clazz + "' :" + e.getMessage());
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException : Cannot instantiate cache for '" + clazz + "' :" + e.getMessage());
        } catch (ClassNotFoundException e) {
            log.error("ClassNotFoundException : Cannot instantiate cache for '" + clazz + "' :" + e.getMessage());
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (InvocationTargetException e) {
        }
        return null;
    }

}
