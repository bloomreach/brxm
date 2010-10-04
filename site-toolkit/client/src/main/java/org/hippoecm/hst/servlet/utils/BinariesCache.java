/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.servlet.utils;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.management.ManagementService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps the {@link BlockingCache} and adds some convenience methods for
 * caching {@link BinaryPage} objects.
 */
public class BinariesCache {

    private static final Logger log = LoggerFactory.getLogger(BinariesCache.class);

    public static final String DEFAULT_NAME = "binariesCache";
    public static final int DEFAULT_LOCK_TIMEOUT_MILLIS = 30 * 1000;
    public static final int DEFAULT_TTL_MILLIS = 24 * 60 * 60 * 1000; // one day
    public static final int DEFAULT_MAX_OBJECTS_IN_MEM = 256;
    public static final long DEFAULT_MAX_OBJECT_SIZE_BYTES = 256L * 1024L;

    private BlockingCache blockingCache;

    private String name = DEFAULT_NAME;

    private int lockTimeOutMillis = DEFAULT_LOCK_TIMEOUT_MILLIS;
    private int ttlMillis = DEFAULT_TTL_MILLIS;
    private int maxObjectsInMem = DEFAULT_MAX_OBJECTS_IN_MEM;
    private long maxObjectSizeBytes = DEFAULT_MAX_OBJECT_SIZE_BYTES;

    private static boolean mBeansRegistered = false;
    private static CacheManager cacheManager;
    private static Object cacheManagerLock = new Object();
    
    private boolean initialized = false;

    private String ehCacheConfigFile;

    public BinariesCache() {
    }

    public BinariesCache(String name) {
        this.name = name;
    }

    public void setConfigFile(String ehCacheConfigFile) {
        this.ehCacheConfigFile = ehCacheConfigFile;
    }

    public void setLockTimeOutMillis(int lockTimeOutMillis) {
        this.lockTimeOutMillis = lockTimeOutMillis;
    }

    public void setTTLMillis(int ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public void setMaxObjectsInMem(int maxObjectsInMem) {
        this.maxObjectsInMem = maxObjectsInMem;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxObjectSizeBytes = maxFileSizeBytes;
    }

    public long getMaxObjectSizeBytes() {
        return maxObjectSizeBytes;
    }

    public long getTTLMillis() {
        return ttlMillis;
    }

    public void destroy() {
        synchronized (cacheManagerLock) {
            if (cacheManager.cacheExists(name)) {
                log.info("Removing binaries cache: {}.", name);
                cacheManager.removeCache(name);
            }
        }
        initialized = false;
    }

    public void init() {
        createCacheManager();
        createCache();
        registerCacheMBeans();
        log.info("Initialized binaries cache: {}.", name);
        log.debug("Cache config: {}", blockingCache);
        initialized = true;
    }

    private void createCacheManager() {
        synchronized (cacheManagerLock) {
            if (cacheManager != null) {
                return;
            }
            if (ehCacheConfigFile == null) {
                cacheManager = CacheManager.create();
            } else {
                try {
                    cacheManager = CacheManager.create(ehCacheConfigFile);
                } catch (CacheException e) {
                    log.error("Error while setting up cache manager for file " + ehCacheConfigFile
                            + " trying to start cache with defaults.", e);
                    cacheManager = CacheManager.create();
                }
            }
        }
    }

    private void createCache() {
        synchronized (cacheManagerLock) {
            Ehcache cache = cacheManager.getEhcache(name);
            if (cache == null) {
                log.warn("No EhCache configuration found. Create new memory cache '{}' with {} maxObjects.", name,
                        maxObjectsInMem);
                cache = new Cache(name, maxObjectsInMem, false, true, 0, 0);
                cacheManager.addCache(cache);
            }

            if (!(cache instanceof BlockingCache)) {
                //decorate and substitute
                BlockingCache newBlockingCache = new BlockingCache(cache);
                cacheManager.replaceCacheWithDecoratedCache(cache, newBlockingCache);
            }
            
            blockingCache = (BlockingCache) cacheManager.getEhcache(name);
            blockingCache.setTimeoutMillis(lockTimeOutMillis);
        }
        log.info("Setting lock timeout for cache '{}' to {} milliseconds.", name, lockTimeOutMillis);
        log.info("Setting max object size for cache '{}' to {} bytes.", name, maxObjectSizeBytes);
    }

    private void registerCacheMBeans() {
        synchronized (cacheManager) {
            if (!mBeansRegistered) {
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                ManagementService.registerMBeans(cacheManager, mBeanServer, true, true, true, true);
                mBeansRegistered = true;
            }
        }
    }

    private void check() {
        if (!initialized) {
            throw new IllegalStateException("Binaries cache is not initialized.");
        }
    }

    public BinaryPage getPageFromBlockingCache(String resourcePath) {
        check();
        try {
            // sets block
            Element element = blockingCache.get(resourcePath);
            if (element != null) {
                BinaryPage page = (BinaryPage) element.getObjectValue();
                log.debug("Cache hit for {} including data: {}", resourcePath, page.containsData());
                return page;
            } else {
                log.debug("Cache miss for {}", resourcePath);
                return null;
            }
        } catch (LockTimeoutException e) {
            log.error("Lock timeout while trying to get " + resourcePath + ". Possible threading issue or high load?",
                    e);
            return null;
        }
    }

    public void updateExpirationTime(BinaryPage page) {
        log.debug("Update expiration for {}", page.getResourcePath());
        page.setExpirationTime(System.currentTimeMillis() + getTTLMillis());
        putPage(page);
    }

    public void putPage(BinaryPage page) {
        check();
        log.debug("Put page for {} including data: {}", page.getResourcePath(), page.containsData());
        blockingCache.put(new Element(page.getResourcePath(), page));
    }

    public void clearBlockingLock(BinaryPage page) {
        check();
        log.debug("Clear lock for {}", page.getResourcePath());
        blockingCache.put(new Element(page.getResourcePath(), null));
    }

    public void removePage(BinaryPage page) {
        check();
        log.debug("Remove page for {}", page.getResourcePath());
        blockingCache.remove(page.getResourcePath());
    }

    public boolean isBinaryDataCacheable(BinaryPage page) {
        if (page.getLength() < 0) {
            log.info("Binary data not cacheable, length is unkown for {}", page.getResourcePath());
            return false;
        } else if (getMaxObjectSizeBytes() <= 0) {
            return false;
        } else if (page.getLength() > getMaxObjectSizeBytes()) {
            if (log.isInfoEnabled()) {
                log.info("Binary data not cacheable, page size " + page.getLength() + " larger than max "
                        + getMaxObjectSizeBytes() + " for " + page.getResourcePath());
            }
            return false;
        } else {
            return true;
        }
    }

    public boolean hasPageExpired(BinaryPage page) {
        if (System.currentTimeMillis() > page.getExpirationTime()) {
            log.debug("Page has expired for {}", page.getResourcePath());
            return true;
        } else {
            return false;
        }
    }

    public boolean isPageStale(BinaryPage page, long lastModified) {
        if (lastModified < 0) {
            log.info("Page is stale, lastModified is unknown for {}", page.getResourcePath());
            return true;
        } else if (lastModified != page.getLastModified()) {
            log.info("Page is stale for {}", page.getResourcePath());
            return true;
        } else {
            return false;
        }
    }
}
