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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoSession;

public class MountResourceAccessor {

    public static Set<String> findUsersWithLockedMainConfigNodes(final HippoSession session, String previewConfigurationPath) throws RepositoryException {
        return MountResource.findUsersWithLockedMainConfigNodes(session, previewConfigurationPath);
    }

    public static Set<String> findUsersWithLockedContainers(final HippoSession session, String previewConfigurationPath) throws RepositoryException {
        return MountResource.findUsersWithLockedContainers(session, previewConfigurationPath);
    }

    public static String buildXPathQueryToFindLockedMainConfigNodesForUsers(String previewConfigurationPath) {
        return MountResource.buildXPathQueryToFindLockedMainConfigNodesForUsers(previewConfigurationPath);
    }

    public static String buildXPathQueryToFindLockedContainersForUsers(String previewConfigurationPath) {
        return MountResource.buildXPathQueryToFindLockedContainersForUsers(previewConfigurationPath);
    }


    public static String buildXPathQueryToFindContainersForUsers(String previewConfigurationPath, List<String> userIds) {
        return MountResource.buildXPathQueryToFindContainersForUsers(previewConfigurationPath, userIds);
    }

    public static String buildXPathQueryToFindMainfConfigNodesForUsers(String previewConfigurationPath, List<String> userIds) {
        return MountResource.buildXPathQueryToFindMainfConfigNodesForUsers(previewConfigurationPath, userIds);
    }

}
