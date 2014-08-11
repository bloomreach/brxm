/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.webresources;

import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Node;


public interface Content {

    /**
     * @return the specific version or <code>null</code> in case of no versions present or non versionable content
     */
    String getVersion();

    String getEncoding();

    Calendar getLastModified();

    String getMimeType();

    /**
     * @return returns a md5 or sha-1 kind of hash for the binary, useful for checking whether two binaries are (very likely)
     * equal or for cache-busting purposes
     */
    String getHash();

    Binary getBinary();

}
