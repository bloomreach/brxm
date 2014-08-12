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


import java.util.List;
import java.util.Map;

public interface WebResource {

    /**
     * @return the path to this web resource, relative to to web resources root location.
     * The path always starts with a slash, and the path elements are also separated by slashes.
     */
    String getPath();

    /**
     * @return the name of this web resource, i.e. the last element of the path.
     */
    String getName();

    /**
     *
     * @return
     */
    List<String> getRevisionIds();

    /**
     * @return the jcr workspace (trunk) version of the content
     */
    Content getCurrent();

    /**
     * @return the most recent checked in version, or if never checked in yet or in case the node is not versionable,
     * the workspace content {@link #getTrunk()} is returned
     */
    Content getLatestRevision();

    /**
     * @param versionName the name of the version (tag) to fetch
     * @return the <code>WebResource</code> for <code>versionName</code> and <code>null</code> if no such version present or
     * if the content is not versionable
     */
    Content getRevision(String revisionId) throws RevisionNotFoundException;

    /**
     * Creates a new revision of this web resource.
     * @return the ID of the created revision.
     */
    String createRevision();

    /**
     * @param binary the binary to store
     * @return true when the content of the nt:file node got changed as a result of this method. This might not be needed
     * in case for example the MD5 of the <code>binary</code> is the same as the already present binary
     */
    boolean update(Binary binary);

    /**
     *
     * @param session
     * @param absPath
     * @return
     */
    boolean delete();

    /**
     * @param revisionId the ID of the revision to restore.
     */
    void restore(String revisionId);

}
