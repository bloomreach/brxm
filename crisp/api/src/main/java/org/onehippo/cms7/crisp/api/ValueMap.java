package org.onehippo.cms7.crisp.api;

import java.util.Map;

public interface ValueMap extends Map<String, Object> {

    <T> T get(String name, Class<T> type);

    <T> T get(String name, T defaultValue);

}
