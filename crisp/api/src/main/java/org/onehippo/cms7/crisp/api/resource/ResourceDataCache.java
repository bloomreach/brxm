package org.onehippo.cms7.crisp.api.resource;

public interface ResourceDataCache {

    public Object getData(Object key);

    public void putData(Object key, Object data);

    public Object putDataIfAbsent(Object key, Object data);

    public void evictData(Object key);

    public void clear();

}
