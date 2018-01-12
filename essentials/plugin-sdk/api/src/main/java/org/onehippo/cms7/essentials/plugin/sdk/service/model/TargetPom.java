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

package org.onehippo.cms7.essentials.plugin.sdk.service.model;

import com.google.common.base.Strings;

public enum TargetPom {

    INVALID(null),
    SITE("site"),
    CMS("cms"),
    REPOSITORY_DATA("repository-data"),
    REPOSITORY_DATA_WEB_FILES("webfiles"),
    REPOSITORY_DATA_APPLICATION("application"),
    REPOSITORY_DATA_DEVELOPMENT("development"),
    ESSENTIALS("essentials"),
    PROJECT("project");

    private final String name;

    TargetPom(final String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DependencyType{");
        sb.append("name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static TargetPom pomForName(final String type) {
        if (Strings.isNullOrEmpty(type)) {
            return TargetPom.INVALID;
        }
        if (type.equals(SITE.name)) {
            return SITE;
        } else if (type.equals(CMS.name)) {
            return CMS;
        } else if (type.equals(REPOSITORY_DATA.name)) {
            return REPOSITORY_DATA;
        } else if (type.equals(PROJECT.name)) {
            return PROJECT;
        } else if (type.equals(REPOSITORY_DATA_APPLICATION.name)) {
            return REPOSITORY_DATA_APPLICATION;
        } else if (type.equals(REPOSITORY_DATA_DEVELOPMENT.name)) {
            return REPOSITORY_DATA_DEVELOPMENT;
        } else if (type.equals(REPOSITORY_DATA_WEB_FILES.name)) {
            return REPOSITORY_DATA_WEB_FILES;
        } else if (type.equals(ESSENTIALS.name)) {
            return ESSENTIALS;
        }
        return TargetPom.INVALID;

    }
}
