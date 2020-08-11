/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions;

public enum ClientError {

    /**
     * Represents a situation where a client tries to add a child node to a parent that does not allow same name
     * siblings.
     */
    ITEM_NAME_NOT_UNIQUE,

    /**
     * Represents a situation where a client tries to add a child node to a parent that does not allow same name
     * siblings, where the parent represents the root of a certain node hierarchy.
     */
    ITEM_NAME_NOT_UNIQUE_IN_ROOT,

    /**
     * Represents a situation where a client tries to mutate an item that has already been locked by another user
     */
    ITEM_ALREADY_LOCKED,
    /**
     * Represents a situation where a client tries to mutate an item that has been changed by another user
     */
    ITEM_CHANGED,

    /**
     * Represents a situation where a client tries to add an item that already exist outside of the workspace with the
     * same name.
     */
    ITEM_EXISTS_OUTSIDE_WORKSPACE,

    /**
     * Represents a situation where a client tries to add an item that has no name (title in case of menu)
     */
    ITEM_NO_NAME,

    INVALID_URL,

    INVALID_PATH_INFO,

    ITEM_CANNOT_BE_CLONED,

    ITEM_NOT_IN_PREVIEW,

    NO_PREVIEW_CONFIGURATION,

    ITEM_NOT_CORRECT_LOCATION,

    ITEM_NOT_FOUND,

    ITEM_NOT_CHILD_OF_PARENT,

    INVALID_UUID,

    INVALID_MOVE_TO_SELF_OR_DESCENDANT,

    INVALID_NODE_TYPE,

    /**
     * When trying to lock a node that has in the mean time been locked, changed and published by someone else, this
     * error can be used to return.
     */
    INVALID_VERSION_STAMP,

    DOCUMENT_CREATION_FAILED,

    INVALID_NAME,

    /**
     * Client requests to create a new item with the given Id that already exists
     */
    ITEM_EXISTS,

    /**
     * Client requests to delete a hst:mount node, but it contains children node(s) of type hst:mount
     */
    CHILD_MOUNT_EXISTS,

    FORBIDDEN,

    UNKNOWN

}
