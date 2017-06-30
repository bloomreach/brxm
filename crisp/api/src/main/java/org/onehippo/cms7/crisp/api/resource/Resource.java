/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.resource;

import java.io.Serializable;

/**
 * Common Resource representation interface, reflecting any content such as JSON, XML, JCR, etc.
 */
public interface Resource extends Serializable {

    /**
     * Returns the resource type name of this resource representation if there's any.
     * For example, a JSON data based implementation may return '@type' property based on its domain rules, or
     * a JCR based implementation may simply return <code>javax.jcr.Node#getPrimaryNodeType().getName()</code>.
     * @return the resource type name of this resource representation if there's any
     */
    String getResourceType();

    /**
     * Returns true if this resource representation is typed and of the specific {@code resourceType} name.
     * @param resourceType resource type name
     * @return true if this resource representation is typed and of the specific {@code resourceType} name
     */
    boolean isResourceType(String resourceType);

    /**
     * Returns the name of this resource representation if there's any.
     * For example, a JSON data based implementation may return '@name' property based on its domain rules, or
     * a JCR based implementation may simply return <code>javax.jcr.Item#getName()</code>.
     * @return the name of this resource representation if there's any
     */
    String getName();

    /**
     * Returns the path of this resource representation if there's any.
     * For example, a JSON data based implementation may construct an XPath-like path based on object hierarchy, or
     * a JCR based implementation may simply return <code>javax.jcr.Item#getPath()</code>.
     * @return the path of this resource representation if there's any
     */
    String getPath();

    /**
     * Returns metadata {@link ValueMap} of this resource representation if there's any, or an empty value map if no metadata available.
     * @return metadata {@link ValueMap} of this resource representation if there's any, or an empty value map if no metadata available
     */
    ValueMap getMetadata();

    /**
     * Returns value map (type of {@link ValueMap}) of this resource representation if there's any, or an empty value map if no values available.
     * For example, a JSON data based implementation may construct a value map if the underlying JSON data is an object from its properties.
     * @return value map (type of {@link ValueMap}) of this resource representation if there's any, or an empty value map if no values available
     */
    ValueMap getValueMap();

    /**
     * Resolves a property value of this resource by the given {@code relPath}. Or null if not resolved by the {@code relPath}.
     * <p>If {@code relPath} is a relative value path, like <code>"content/title"</code>, then the return should
     * be equivalent to the result of the call, <code>((Resource) getValueMap().get("content")).getValueMap().get("title")</code>
     * if existing.
     * <p>In addition, a path segment may contain an array index notation like <code>"content/images[1]/title"</code>.
     * In this case, the value at <code>content/images</code> must be an array type <code>Resource</code> object which
     * returns true on <code>Resource.isArray()</code>.</p>
     * @param relPath property or child resource relative path
     * @return a resolved property value of this resource by the given {@code relPath}. Or null if not resolved by the {@code relPath}
     */
    Object getValue(String relPath);

    /**
     * Resolves a property value of this resource by the given {@code relPath} and converts it into the given type.
     * Or null if not resolved by the {@code relPath}.
     * <p>If {@code relPath} is a relative value path, like <code>"content/title"</code>, then the return should
     * be equivalent to the result of the call, <code>((Resource) getValueMap().get("content")).getValueMap().get("title")</code>
     * if existing.
     * <p>In addition, a path segment may contain an array index notation like <code>"content/images[1]/title"</code>.
     * In this case, the value at <code>content/images</code> must be an array type <code>Resource</code> object which
     * returns true on <code>Resource.isArray()</code>.</p>
     * @param relPath property or child resource relative path
     * @return a resolved property value of this resource by the given {@code relPath}. Or null if not resolved by the {@code relPath}
     */
    <T> T getValue(String relPath, Class<T> type);

    /**
     * Resolves the default property value of this resource if available, or null if unavailable.
     * The default value resolution totally depends on the implementations. For example, an XML element based
     * implementation may choose to return the text content of the underlying element.
     * @return the default property value of this resource if available, or null if unavailable
     */
    Object getDefaultValue();

    /**
     * Resolves the default property value of this resource if available and converts it into the given type, or
     * null if unavailable. The default value resolution totally depends on the implementations. For example, an XML element based
     * implementation may choose to return the text content of the underlying element.
     * @return the default property value of this resource if available, or null if unavailable
     */
    <T> T getDefaultValue(Class<T> type);

    /**
     * Returns parent resource representation if there's any.
     * @return parent resource representation if there's any
     */
    Resource getParent();

    /**
     * Returns true if this resource representation contains any child resource representation.
     * @return true if this resource representation contains any child resource representation
     */
    boolean isAnyChildContained();

    /**
     * Returns true if this resource representation is purely for an array (e.g, JSON Array if underlying data is based on JSON).
     * @return true if this resource representation is purely for an array (e.g, JSON Array if underlying data is based on JSON)
     */
    boolean isArray();

    /**
     * Returns child resource count of this resource representation.
     * @return child resource count of this resource representation
     */
    long getChildCount();

    /**
     * Return a {@link ResourceCollection} of child resource representations.
     * @return a {@link ResourceCollection} of child resource representations
     */
    ResourceCollection getChildren();

    /**
     * Return a {@link ResourceCollection} of child resource representations from {@code offset} index with {@code limit}
     * count at max.
     * @param offset offset index to start iteration
     * @param limit limit count of iteration
     * @return a {@link ResourceCollection} of child resource representations
     */
    ResourceCollection getChildren(long offset, long limit);

}
