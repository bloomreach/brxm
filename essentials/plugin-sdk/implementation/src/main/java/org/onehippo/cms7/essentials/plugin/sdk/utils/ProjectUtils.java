/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import org.apache.commons.lang.StringUtils;

public final class ProjectUtils {

    static final String ENT_GROUP_ID = "com.onehippo.cms7";
    static final String ENT_RELEASE_ID = "hippo-cms7-enterprise-release";
    static final String ENT_REPO_ID = "hippo-maven2-enterprise";
    static final String ENT_REPO_NAME = "Hippo Enterprise Maven 2";
    static final String ENT_REPO_URL = "https://maven.onehippo.com/maven2-enterprise";

    public static String getBaseProjectDirectory() {
        final String basePath = System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);

        if (StringUtils.isNotBlank(basePath)) {
            return basePath;
        }
        throw new IllegalStateException("System property 'project.basedir' was null or empty. Please start your application with -D=project.basedir=/project/path");
    }

    public static String getEssentialsModuleName() {
        final String essentialsModuleName = System.getProperty(EssentialConst.ESSENTIALS_BASEDIR_PROPERTY);
        return StringUtils.isNotBlank(essentialsModuleName) ? essentialsModuleName : "essentials";
    }
}
