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

import javax.jcr.Binary;
import javax.jcr.Session;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.cms7.services.WhiteboardService;


@SingletonService
@WhiteboardService
@SuppressWarnings("UnusedDeclaration")
public interface WebResourcesService {

    /**
     * @param absPath the absolute path to the nt:file node
     * @return the jcr workspace (trunk) version
     */
    File get(Session session, String absPath);

    // TODO checkin method


    // TODO how should create work?
    // boolean create(Session session, String absPath, Binary binary);

    /**
     * @param absPath to a jcr node of type jcr:content
     * @param binary the binary to store
     * @return true when the content of the nt:file node got changed as a result of this method. This might not be needed
     * in case for example the MD5 of the <code>binary</code> is the same as the already present binary
     */
    boolean update(Session session, String absPath, Binary binary);

    /**
     *
     * @param session
     * @param absPath
     * @return
     */
    boolean delete(Session session, String absPath);

    /**
     * @param absPath to a jcr node of type nt:file
     */
    void restore(Session session, String absPath, String versionName);

}
