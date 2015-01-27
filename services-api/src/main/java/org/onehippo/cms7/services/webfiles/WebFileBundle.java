/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles;

import java.util.List;

/**
 * A collection of {@link WebFile}s, i.e. binary data objects. Each {@link WebFile} is identified by an absolute path
 * starting at the {@link WebFile}s root. Paths start with a slash and consist of elements separated by slashes
 * (e.g. "/css/style.css").
 */
public interface WebFileBundle {

    /**
     * @param absPath the absolute path to the {@link WebFile}, starting at the {@link WebFile}s root. The path must start with a slash.
     * @return whether a {@link WebFile} exists at the given path.
     */
    boolean exists(String absPath);

    /**
     * @param absPath the absolute path starting with a slash to the {@link WebFile}, relative to this {@link WebFileBundle}
     * @return the {@link WebFile} <b>head</b> located at the given path.
     * @throws WebFileNotFoundException if no {@link WebFile} exists at the given path.
     */
    WebFile get(String absPath) throws WebFileNotFoundException;

    /**
     * Creates a new {@link WebFile} at the given location.
     * @param absPath the absolute path starting with a slash to the {@link WebFile}, relative to this {@link WebFileBundle}
     * @param content the content of the new {@link WebFile}.
     * @return the created {@link WebFile}.
     * @throws WebFileException if another (web) resource already exists at the given location.
     * @throws java.lang.IllegalArgumentException if the given binary is <code>null</code>.
     * @throws WebFileException in case of insufficient authorization
     */
    WebFile create(String absPath, Binary content) throws WebFileException, IllegalArgumentException;

    /**
     * Updates the content of this {@link WebFile}.
     * @param absPath the absolute path starting with a slash to the {@link WebFile}, relative to this {@link WebFileBundle}
     * @param content the binary data to store.
     * @return the updated {@link WebFile}
     * @throws WebFileNotFoundException if no {@link WebFile} exists at the given path.
     * @throws java.lang.IllegalArgumentException if the given binary is <code>null</code>.
     * @throws WebFileException in case of insufficient authorization
     */
    WebFile update(String absPath, Binary content) throws WebFileNotFoundException, IllegalArgumentException;

    /**
     * Removes the {@link WebFile} at absPath
     * @param absPath the absolute path starting with a slash to the {@link WebFile}, relative to this {@link WebFileBundle}
     * @throws WebFileException in case the delete did not occur, for example because the resource is not there any more,
     * insufficient authorization, locking, etc
     */
    void delete(String absPath) throws WebFileException;

    /**e
     * @return the latest tagName or in case not yet tagged or tagging is not supported, return <code>null</code>
     */
    String getLatestTagName() throws WebFileException;

    /**
     * @return The immutable {@link java.util.List} ordered list of tag names for this {@link WebFileBundle} or empty List in case
     * there are no tags for this {@link WebFileBundle} or tagging is not supported (for this {@link WebFileBundle})
     */
    List<String> getTagNames() throws WebFileException;

    /**
     * @param absPath the absolute path to the {@link WebFile}, starting at the {@link WebFile}s root. The path must start
     *                with a slash when not <code>null</code>.
     * @param tagName the  {@link WebFile} for tagName. When <code>tagName</code> is null, the head
     *                {@link WebFile} is returned, the same as {@link #get(String)}
     * @return the {@link WebFile} <b>tag</b> located at the given path for <code>tagName</code>.
     * @throws WebFileNotFoundException if no {@link WebFile} exists at the given path for <code>tagName</code>.
     */
    WebFile get(String absPath, String tagName) throws WebFileNotFoundException, WebFileTagNotFoundException;

    /**
     * Creates a new tag for this entire {@link WebFileBundle}
     * @return the name of the newly created tag
     * @throws WebFileException
     */
    String createTag() throws WebFileException;

    /**
     * @return the anti-cache value of this {@link WebFileBundle}, useful to be able to cache resources in the
     * client (browser) : Whenever the client needs new resources, the anti cache value changes resulting in different
     * URLs for the resources. Only chars allowed are [a-z] and [0-9] to avoid encoding/decoding problems when used in
     * URLs
     */
    String getAntiCacheValue();
}
