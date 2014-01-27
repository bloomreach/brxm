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

package org.onehippo.cms7.essentials.dashboard.model.hst;


import java.util.Collections;
import java.util.List;

import org.onehippo.cms7.essentials.dashboard.config.Document;

/**
 * @version "$Id$"
 */
public abstract class BaseJcrModel implements Document {

    private String name;
    private String parentPath;


    @Override
    public String getParentPath() {
        return parentPath;
    }

    @Override
    public void setParentPath(final String parentPath) {
        this.parentPath = parentPath;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public List<String> getProperties() {
        return Collections.emptyList();
    }

    @Override
    public void setProperties(final List<String> properties) {
        // ignore
    }

    @Override
    public void addProperty(final String value) {
        // ignore
    }

    @Override
    public String getPath() {
        return parentPath + '/' + name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BaseJcrModel{");
        sb.append("name='").append(name).append('\'');
        sb.append(", parentPath='").append(parentPath).append('\'');
        sb.append(", class='").append(this.getClass()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
