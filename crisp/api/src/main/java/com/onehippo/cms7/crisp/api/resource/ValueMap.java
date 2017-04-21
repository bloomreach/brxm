/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import java.io.Serializable;
import java.util.Map;

/**
 * <code>ValueMap</code> is an easy way to access properties or metadata of a resource. With resources, you can
 * use <code>Resource#getValueMap()</code> to obtain the value map of a resource. The various getter methods can
 * be used to get the properties of the resource.
 * <p>In addition a value map returned by a resource supports getting of deep values, like <code>get("content/title")</code>
 * which is equivalent to call <code>get("content")</code> on the resource, get the value map of that resource
 * and call <code>get("title")</code>.
 * </p>
 * <p>A ValueMap should be immutable.</p>
 */
public interface ValueMap extends Map<String, Object>, Serializable {

    /**
     * Get a named property and convert it into the given type.
     * This method does not support conversion into a primitive type or an array of a primitive type.
     * It should return null in this case.
     * @param name The name of the property
     * @param type The class of the type
     * @return named value converted to type T or null if non existing or can't be converted
     */
    <T> T get(String name, Class<T> type);

    /**
     * Get a named property and convert it into the given type.
     * This method does not support conversion into a primitive type or an array of a primitive type.
     * It should return the default value in this case.
     * @param name The name of the property
     * @param defaultValue The default value to use if the named property does not exist or cannot be converted
     *        to the requested type. The default value is also used to define the type to convert the value to.
     *        If this is null any existing property is not converted.
     * @return named value converted to type T or the default value if non existing or can't be converted
     */
    <T> T get(String name, T defaultValue);

}
