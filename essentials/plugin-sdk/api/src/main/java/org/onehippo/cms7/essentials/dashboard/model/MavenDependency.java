/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

public interface MavenDependency {
    String GROUP_ID = "groupId";
    String ARTIFACT_ID = "artifactId";
    String VERSION = "version";
    String TYPE = "type";
    String SCOPE = "scope";

    String getGroupId();

    String getArtifactId();

    String getVersion();

    String getScope();

    String getType();
}
