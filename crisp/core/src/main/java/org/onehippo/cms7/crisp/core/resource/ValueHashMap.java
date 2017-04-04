package org.onehippo.cms7.crisp.core.resource;

import java.util.HashMap;

import org.onehippo.cms7.crisp.api.resource.ValueMap;

public class ValueHashMap extends HashMap<String, Object> implements ValueMap {

    private static final long serialVersionUID = 1L;

    @Override
    public <T> T get(String name, Class<T> type) {
        return (T) get(name);
    }

    @Override
    public <T> T get(String name, T defaultValue) {
        if (containsKey(name)) {
            return (T) get(name);
        }

        return null;
    }

}
