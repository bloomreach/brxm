/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.service;

public enum ResourceType {
    /**
     * Resources of this type will be loaded by javascript code in a hidden iframe
     * and then communicated back by connecting to the parent via penpal using postmessage.
     */
    IFRAME,

    /**
     * Resources of this type should have an absolute url including a host name and scheme.
     * The resources it provides can be acquired by calling this url directly.
     */
    REST,

    /**
     * Resources of this type should have an absolute url without a host name and scheme.
     * The resources it provides can be acquired by first prefixing it with a scheme,
     * host and optionally a path and then calling this url.
     */
    INTERNAL_REST,
}
