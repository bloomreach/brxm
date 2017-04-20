package com.onehippo.cms7.crisp.api.resource;

import java.io.Serializable;
import java.util.Map;

public interface ValueMap extends Map<String, Object>, Serializable {

    <T> T get(String name, Class<T> type);

    <T> T get(String name, T defaultValue);

}
