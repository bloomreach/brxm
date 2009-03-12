package org.hippoecm.hst.ocm.manager.cache;

import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;

/**
 * HST Object cache which is expected to use HST Cache Infrastructure
 * 
 * @version $Id$
 */
public class HstRequestObjectCacheImpl implements ObjectCache {
    
    public void cache(String path, Object object) {
    }

    public void clear() {
    }

    public boolean isCached(String path) {
        return false;
    }

    public Object getObject(String path) {
        return null;
    }

}
