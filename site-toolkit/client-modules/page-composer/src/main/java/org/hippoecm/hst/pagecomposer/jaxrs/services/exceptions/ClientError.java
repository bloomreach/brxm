/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

    ITEM_NOT_IN_PREVIEW,

    ITEM_NOT_FOUND,

    ITEM_NOT_IN_WORKSPACE,

    ITEM_ALREADY_LOCKED,

    ITEM_NOT_CHILD_OF_PARENT,

    ITEM_NAME_NOT_UNIQUE,

    INVALID_UUID,

    INVALID_NODE_TYPE
}
