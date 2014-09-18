/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.webresources;

import java.util.List;

/**
 * A collection of web resources, i.e. binary data objects. Each web resource is identified by an absolute path
 * starting at the web resources root. Paths start with a slash and consist of elements separated by slashes
 * (e.g. "/css/style.css").
 */
public interface WebResourceBundle {

    /**
     * @param absPath the absolute path to the web resource, starting at the web resources root. The path must start with a slash.
     * @return whether a web resource exists at the given path.
     */
    boolean exists(String absPath);

    /**
     * @param absPath the absolute path starting with a slash to the web resource, relative to this {@link WebResourceBundle}
     * @return the web resource <b>head</b> located at the given path.
     * @throws WebResourceNotFoundException if no web resource exists at the given path.
     */
    WebResource get(String absPath) throws WebResourceNotFoundException;

    /**
     * Creates a new web resource at the given location.
     * @param absPath the absolute path starting with a slash to the web resource, relative to this {@link WebResourceBundle}
     * @param content the content of the new web resource.
     * @return the created web resource.
     * @throws WebResourceException if another (web) resource already exists at the given location.
     * @throws java.lang.IllegalArgumentException if the given binary is <code>null</code>.
     * @throws WebResourceException in case of insufficient authorization
     */
    WebResource create(String absPath, Binary content) throws WebResourceException, IllegalArgumentException;

    /**
     * Updates the content of this web resource.
     * @param absPath the absolute path starting with a slash to the web resource, relative to this {@link WebResourceBundle}
     * @param content the binary data to store.
     * @return the updated {@link WebResource}
     * @throws WebResourceNotFoundException if no web resource exists at the given path.
     * @throws java.lang.IllegalArgumentException if the given binary is <code>null</code>.
     * @throws WebResourceException in case of insufficient authorization
     */
    WebResource update(String absPath, Binary content) throws WebResourceNotFoundException, IllegalArgumentException;

    /**
     * Removes the web resource at absPath
     * @param absPath the absolute path starting with a slash to the web resource, relative to this {@link WebResourceBundle}
     * @throws WebResourceException in case the delete did not occur, for example because the resource is not there any more,
     * insufficient authorization, locking, etc
     */
    void delete(String absPath) throws WebResourceException;

    /**e
     * @return the latest tagName or in case not yet tagged or tagging is not supported, return <code>null</code>
     */
    String getLatestTagName() throws WebResourceException;

    /**
     * @return The immutable {@link java.util.List} ordered list of tag names for this {@link WebResourceBundle} or empty List in case
     * there are no tags for this {@link WebResourceBundle} or tagging is not supported (for this {@link WebResourceBundle})
     */
    List<String> getTagNames() throws WebResourceException;

    /**
     * @param absPath the absolute path to the web resource, starting at the web resources root. The path must start
     *                with a slash when not <code>null</code>.
     * @param tagName the  {@link WebResource} for tagName. When <code>tagName</code> is null, the head
     *                {@link org.onehippo.cms7.services.webresources.WebResource} is returned, the same as {@link #get(String)}
     * @return the web resource <b>tag</b> located at the given path for <code>tagName</code>.
     * @throws WebResourceNotFoundException if no web resource exists at the given path for <code>tagName</code>.
     */
    WebResource get(String absPath, String tagName) throws WebResourceNotFoundException, WebResourceTagNotFoundException;

    /**
     * Creates a new tag for this entire {@link WebResourceBundle}
     * @return the name of the newly created tag
     * @throws WebResourceException
     */
    String createTag() throws WebResourceException;

    /**
     * @return the anti-cache value of this {@link WebResourceBundle}. Useful to be able to cache resources in the
     * client (browser) : Whenever the client needs new resources, the anti cache value changes resulting in different
     * URLs for the resources
     */
    String getAntiCacheValue();
}
