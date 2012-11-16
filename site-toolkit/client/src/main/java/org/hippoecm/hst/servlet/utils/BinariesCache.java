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

import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides some convenience methods for
 * caching {@link BinaryPage} objects.
 */
public class BinariesCache {

    private static final Logger log = LoggerFactory.getLogger(BinariesCache.class);

    public static final long DEFAULT_MAX_OBJECT_SIZE_BYTES = 256L * 1024L;
    
    /**
     * Default validity check interval set to 3 minutes
     */
    public static final long DEFAULT_VALIDITY_CHECK_INTERVAL_MILLIS = 1000L * 60 * 3; 
    
    private HstCache cache;

    private long maxObjectSizeBytes = DEFAULT_MAX_OBJECT_SIZE_BYTES;
    

    private long validityCheckIntervalMillis = DEFAULT_VALIDITY_CHECK_INTERVAL_MILLIS;
    
    public BinariesCache(HstCache cache) {
        this.cache = cache;
    }
    
    public HstCache getHstCache() {
        return cache;
    }
    
    public void setHstCache(HstCache cache) {
        this.cache = cache;
    }
    
    public long getValidityCheckIntervalMillis() {
        return validityCheckIntervalMillis;
    }
    
    public void setValidityCheckIntervalMillis(long validityCheckIntervalSeconds) {
        validityCheckIntervalMillis = 1000L * validityCheckIntervalSeconds;
    }
    
    public long getMaxObjectSizeBytes() {
        return maxObjectSizeBytes;
    }
    
    public void setMaxObjectSizeBytes(long maxObjectSizeBytes) {
        this.maxObjectSizeBytes = maxObjectSizeBytes;
    }
    
    public BinaryPage getPageFromBlockingCache(String resourcePath) {
        try {
            CacheElement element = cache.get(resourcePath);
            
            if (element != null) {
                BinaryPage page = (BinaryPage) element.getContent();
                if (log.isDebugEnabled()) {
                    log.debug("Cache hit for {} including data: {}", resourcePath, page.containsData());
                }
                return page;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Cache miss for {}", resourcePath);
                }
                return null;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Exception while trying to get {}. Possible threading issue or high load?", resourcePath, e);
            } else {
                log.error("Exception while trying to get {}. Possible threading issue or high load? {}", resourcePath, e.toString());
            }
            return null;
        }
    }

    public void updateNextValidityCheckTime(BinaryPage page) {
        if (log.isDebugEnabled()) {
            log.debug("Update next validity check time for {}", page.getResourcePath());
        }
        long nextValidityCheckTimeStamp = System.currentTimeMillis() + getValidityCheckIntervalMillis();
        page.setNextValidityCheckTime(nextValidityCheckTimeStamp);
        putPage(page);
    }

    public void putPage(BinaryPage page) {
        if (log.isDebugEnabled()) {
            log.debug("Put page for {} including data: {}", page.getResourcePath(), page.containsData());
        }
        CacheElement element = cache.createElement(page.getResourcePath(), page);
        cache.put(element);
    }

    public void clearBlockingLock(BinaryPage page) {
        if (log.isDebugEnabled()) {
            log.debug("Clear lock for {}", page.getResourcePath());
        }
        CacheElement element = cache.createElement(page.getResourcePath(), null);
        cache.put(element);
    }

    public void clearBlockingLock(String resourcePath) {
        if (log.isDebugEnabled()) {
            log.debug("Clear lock for {}", resourcePath);
        }
        CacheElement element = cache.createElement(resourcePath, null);
        cache.put(element);
    }

    
    public void removePage(BinaryPage page) {
        if (log.isDebugEnabled()) {
            log.debug("Remove page for {}", page.getResourcePath());
        }
        cache.remove(page.getResourcePath());
    }

    public boolean isBinaryDataCacheable(BinaryPage page) {
        if (page.getLength() < 0) {
            if (log.isInfoEnabled()) {
                log.info("Binary data not cacheable, length is unkown for {}", page.getResourcePath());
            }
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

    public boolean mustCheckValidity(BinaryPage page) {
        if (System.currentTimeMillis() > page.getNextValidityCheckTimeStamp()) {
            if (log.isDebugEnabled()) {
                log.debug("Page validity must be checked for {}", page.getResourcePath());
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isPageStale(BinaryPage page, long lastModified) {
        if (lastModified < 0) {
            if (log.isInfoEnabled()) {
                log.info("Page is stale, lastModified is unknown for {}", page.getResourcePath());
            }
            return true;
        } else if (lastModified != page.getLastModified()) {
            if (log.isInfoEnabled()) {
                log.info("Page is stale for {}", page.getResourcePath());
            }
            return true;
        } else {
            return false;
        }
    }
}
