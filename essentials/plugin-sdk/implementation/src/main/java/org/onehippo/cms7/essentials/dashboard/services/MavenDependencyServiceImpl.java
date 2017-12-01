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

package org.onehippo.cms7.essentials.dashboard.services;

import javax.inject.Singleton;

import org.onehippo.cms7.essentials.dashboard.model.MavenDependency;
import org.onehippo.cms7.essentials.dashboard.model.MavenDependencyImpl;
import org.onehippo.cms7.essentials.dashboard.service.MavenDependencyService;
import org.springframework.stereotype.Service;

@Service
@Singleton
public class MavenDependencyServiceImpl implements MavenDependencyService {

    @Override
    public MavenDependency createDependency(final String groupId, final String artifactId) {
        return createDependency(groupId, artifactId, null, null, null);
    }

    @Override
    public MavenDependency createDependency(final String groupId, final String artifactId, final String version) {
        return createDependency(groupId, artifactId, version, null, null);
    }

    @Override
    public MavenDependency createDependency(final String groupId, final String artifactId, final String version, final String type, final String scope) {
        return new MavenDependencyImpl(groupId, artifactId, version, type, scope);
    }
}
