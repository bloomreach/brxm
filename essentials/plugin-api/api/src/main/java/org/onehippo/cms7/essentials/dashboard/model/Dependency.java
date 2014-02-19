/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model;

/**
 * @version "$Id$"
 */
public interface Dependency {

    String getGroupId();

    void setGroupId(String groupId);

    String getArtifactId();

    void setArtifactId(String artifactId);

    String getRepositoryId();

    void setRepositoryId(String repositoryId);

    String getRepositoryUrl();

    void setRepositoryUrl(String repositoryUrl);

    String getVersion();

    void setVersion(String version);

    String getScope();

    String getType();

    void setType(String type);

    void setScope(String scope);
}
