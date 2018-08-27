/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor;

import java.io.Serializable;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Visits {@link Tag}s that are part of a DOM tree.
 */
public interface TagVisitor extends Serializable {

    /**
     * Called by {@link HtmlProcessor#read(String, List)} for each {@link Tag} in the DOM tree.
     *
     * @param parent The parent of the tag parameter, or null if the tag is the root node of the DOM tree
     * @param tag The visited tag
     * @throws RepositoryException when an error occurred. Visiting will continue for the other tags.
     */
    void onRead(Tag parent, Tag tag) throws RepositoryException;

    /**
     * Called by {@link HtmlProcessor#write(String, List)} for each {@link Tag} in the DOM tree.
     *
     * @param parent The parent of the tag parameter, or null if the tag is the root node of the DOM tree
     * @param tag The visited tag
     * @throws RepositoryException when an error occurred. Visiting will continue for the other tags.
     */
    void onWrite(Tag parent, Tag tag) throws RepositoryException;

    /**
     * Called before the visitor is applied to the DOM tree.
     */
    void before();

    /**
     * Called after the visitor is applied to the DOM tree.
     */
    void after();

}
