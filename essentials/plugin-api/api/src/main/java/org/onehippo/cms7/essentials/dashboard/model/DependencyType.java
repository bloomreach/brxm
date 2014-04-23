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

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public enum DependencyType {


    INVALID(null), SITE("site"), CMS("cms"), BOOTSTRAP("bootstrap"), BOOTSTRAP_CONFIG("config"), BOOTSTRAP_CONTENT("content"), ESSENTIALS("essentials");
    private final String name;

    DependencyType(final String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DependencyType{");
        sb.append("name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static DependencyType typeForName(final String type) {
        if (Strings.isNullOrEmpty(type)) {
            return DependencyType.INVALID;
        }
        if (type.equals(SITE.name)) {
            return SITE;
        } else if (type.equals(CMS.name)) {
            return CMS;
        } else if (type.equals(BOOTSTRAP.name)) {
            return BOOTSTRAP;
        } else if (type.equals(BOOTSTRAP_CONFIG.name)) {
            return BOOTSTRAP_CONFIG;
        } else if (type.equals(BOOTSTRAP_CONTENT.name)) {
            return BOOTSTRAP_CONTENT;
        } else if (type.equals(ESSENTIALS.name)) {
            return ESSENTIALS;
        }
        return DependencyType.INVALID;

    }
}
