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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import org.onehippo.cms7.essentials.dashboard.service.ProjectService;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.springframework.stereotype.Service;

@Service
@Singleton
public class ProjectServiceImpl implements ProjectService {

    @Override
    public List<File> getLog4j2Files() {
        final FilenameFilter log4j2Filter = (dir, name) -> name.matches("log4j2.*\\.xml");
        final File[] log4j2Files = ProjectUtils.getConfFolder().listFiles(log4j2Filter);

        return log4j2Files != null ? Arrays.asList(log4j2Files) : Collections.emptyList();
    }
}
