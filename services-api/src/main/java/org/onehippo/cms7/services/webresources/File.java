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


import java.util.Map;

import javax.jcr.Node;

public interface File {

    /**
     * @return the path to the nt:file node
     */
    Node getNode();

    String getFileName();

    /**
     * @return the jcr workspace (trunk) version
     */
    Content getWorkspace();

    /**
     * @return the most recent checked in version
     */
    Content getBase();

    /**
     * @param versionName the name of the version to fetch
     * @return the <code>WebResource</code> for <code>versionName</code> and <code>null</code> if no such version present
     */
    Content get(String versionName);

    /**
     * @return all versions
     */
    Map<String, Content> getAll();


}
